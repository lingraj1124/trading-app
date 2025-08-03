package com.example.webhook.service;

import com.example.webhook.entity.OrderLog;
import com.example.webhook.model.DhanOrderRequest;
import com.example.webhook.repository.OrderLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class DhanService {

    private static final Logger logger = LoggerFactory.getLogger(DhanService.class);

    @Autowired
    private OrderLogRepository orderLogRepository;

    @Value("${dhan.client-id}")
    private String clientId;

    @Value("${dhan.access-token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<String> placeOrder(DhanOrderRequest request) {
        OrderLog backupLog = null;

        try {
            boolean hasPosition = checkPositionFromDhan(request.getSecurityId());
            logger.info("Full request data: {}", objectMapper.writeValueAsString(request));

            if ("BUY".equalsIgnoreCase(request.getTransactionType()) && hasPosition) {
                logger.info("Skipping BUY for {} as position exists.", request.getSecurityId());
                return ResponseEntity.ok("BUY skipped due to existing position");
            }

            if ("STOP_LOSS".equalsIgnoreCase(request.getOrderType())) {

                if (!hasPosition) {
                    updateStopLossLogAsClosed(request.getSecurityId());
                    logger.info("STOP_LOSS skipped, no active position for {}", request.getSecurityId());
                    return ResponseEntity.ok("STOP_LOSS skipped due to no position");
                }

                Optional<OrderLog> existingLogOpt = orderLogRepository
                        .findTopBySecurityIdAndOrderTypeOrderByIdDesc(request.getSecurityId(), "STOP_LOSS");

                if (existingLogOpt.isPresent()) {
                    OrderLog existingLog = existingLogOpt.get();

                    backupLog = new OrderLog();
                    backupLog.setOrderId(existingLog.getOrderId());
                    backupLog.setOrderType(existingLog.getOrderType());
                    backupLog.setSecurityId(existingLog.getSecurityId());
                    backupLog.setCreatedDateTime(existingLog.getCreatedDateTime());
                    backupLog.setPosition(existingLog.getPosition());

                    try {
                        logger.info("Cancelling old STOP_LOSS: {}", existingLog.getOrderId());
                        HttpHeaders headers = createHeaders();
                        restTemplate.exchange("https://api.dhan.co/v2/orders/" + existingLog.getOrderId(),
                                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
                    } catch (Exception e) {
                        logger.warn("Failed to cancel STOP_LOSS: {}", e.getMessage());
                    }
                }
            }

            HttpHeaders headers = createHeaders();
            request.setCorrelationId("order-" + System.currentTimeMillis());
            request.setDhanClientId(clientId);

            HttpEntity<DhanOrderRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.dhan.co/v2/orders", HttpMethod.POST, entity, String.class);

            logger.info("Order Response: {}", response.getBody());

            JsonNode json = objectMapper.readTree(response.getBody());
            if (json.has("orderId")) {
                OrderLog log = new OrderLog();
                log.setOrderId(json.get("orderId").asText());
                log.setOrderType(request.getOrderType());
                log.setSecurityId(request.getSecurityId());
                log.setCreatedDateTime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")));
                log.setPosition("true");

                orderLogRepository.save(log);
                logger.info("Saved order log to DB");
            }

            return response;

        } catch (Exception e) {
            logger.error("Error placing order: {}", e.getMessage());

            if ("STOP_LOSS".equalsIgnoreCase(request.getOrderType()) && backupLog != null) {
                try {
                    logger.warn("Restoring previous STOP_LOSS log due to failure.");
                    backupLog.setId(null);
                    orderLogRepository.save(backupLog);
                    logger.info("Restored STOP_LOSS log.");
                } catch (Exception restoreEx) {
                    logger.error("Failed to restore old STOP_LOSS: {}", restoreEx.getMessage());
                }
            }

            return ResponseEntity.internalServerError().body("Error placing order: " + e.getMessage());
        }
    }

    private boolean checkPositionFromDhan(String securityId) {
        try {
            HttpHeaders headers = createHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.dhan.co/v2/positions", HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            for (JsonNode position : root) {
                if (
                        position.has("securityId") &&
                        position.has("positionType") &&
                        !"CLOSED".equalsIgnoreCase(position.get("positionType").asText()) &&
                        securityId.equals(position.get("securityId").asText())
                ) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Error checking position: {}", e.getMessage());
        }
        return false;
    }

    private void updateStopLossLogAsClosed(String securityId) {
        Optional<OrderLog> logOpt = orderLogRepository
                .findTopBySecurityIdAndOrderTypeOrderByIdDesc(securityId, "STOP_LOSS");

        logOpt.ifPresent(log -> {
            log.setPosition("closed");
            orderLogRepository.save(log);
            logger.info("STOP_LOSS log marked closed for {}", securityId);
        });
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", accessToken);
        headers.set("Dhan-Client-Id", clientId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
