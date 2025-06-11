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
import java.util.Objects;
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
        try {
            // Step 1: Check position from Dhan API
            boolean hasPosition = checkPositionFromDhan(request.getSecurityId());


            logger.info("full request data {}", objectMapper.writeValueAsString(request));

            // Step 2: Avoid repeated BUY if position already exists
            if ("BUY".equalsIgnoreCase(request.getTransactionType()) && hasPosition) {
                logger.info("Skipping BUY order for {} as position already exists.", request.getSecurityId());
                return ResponseEntity.ok("BUY skipped due to existing position");
            }

            // Step 3: STOP_LOSS check - if position doesn't exist, don't place STOP_LOSS
            if ("STOP_LOSS".equalsIgnoreCase(request.getOrderType())) {
                if (!hasPosition) {
                    updateStopLossLogAsClosed(request.getSecurityId());
                    logger.info("STOP_LOSS skipped, no active position for {}", request.getSecurityId());
                    return ResponseEntity.ok("STOP_LOSS skipped due to no position");
                }

                // Step 4: Cancel existing STOP_LOSS from DB
                Optional<OrderLog> existingLogOpt = orderLogRepository.findTopBySecurityIdAndOrderTypeOrderByIdDesc(
                        request.getSecurityId(), "STOP_LOSS");

                existingLogOpt.ifPresent(log -> {
                    String cancelUrl = "https://api.dhan.co/v2/orders/" + log.getOrderId();
                    try {
                        logger.info("Cancelling old STOP_LOSS with ID: {}", log.getOrderId());
                        HttpHeaders headers = createHeaders();
                        restTemplate.exchange(cancelUrl, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
                    } catch (Exception e) {
                        logger.warn("Failed to cancel STOP_LOSS: {}", e.getMessage());
                    }
                });
            }

            // Step 5: Place new order
            HttpHeaders headers = createHeaders();
            request.setCorrelationId("order-" + System.currentTimeMillis());
            request.setDhanClientId(clientId);

            HttpEntity<DhanOrderRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.dhan.co/v2/orders", HttpMethod.POST, entity, String.class);

            logger.info("Order Response: {}", response.getBody());

            // Step 6: Store Order in DB
            JsonNode json = objectMapper.readTree(response.getBody());
            if (json.has("orderId")) {
                String orderId = json.get("orderId").asText();
                OrderLog log = new OrderLog();
                log.setOrderId(orderId);
                log.setOrderType(request.getOrderType());
                log.setSecurityId(request.getSecurityId());
                log.setCreatedDateTime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")));
                log.setPosition("true");
                OrderLog saved = orderLogRepository.save(log);
                logger.info("Saved order log to DB: {}", saved);
            }

            return response;

        } catch (Exception e) {
            logger.error("Error placing order", e);
            return ResponseEntity.internalServerError().body("Error placing order: " + e.getMessage());
        }
    }

    private boolean checkPositionFromDhan(String securityId) {
        try {
            String url = "https://api.dhan.co/v2/positions";
            HttpHeaders headers = createHeaders();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

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
        Optional<OrderLog> logOpt = orderLogRepository.findTopBySecurityIdAndOrderTypeOrderByIdDesc(securityId, "STOP_LOSS");
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