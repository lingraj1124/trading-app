package com.example.webhook.controller;

import com.example.webhook.model.DhanOrderRequest;
import com.example.webhook.service.DhanService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.math.RoundingMode;
import java.math.BigDecimal;

@RestController
public class DhanController {

    @Autowired
    private DhanService dhanService;

    @Autowired
    private ObjectMapper objectMapper;

    // ================= WEBHOOK =================
    @PostMapping("/webhook")
    public String receiveWebhook(@RequestBody Map<String, Object> payload) {
        System.out.println("Received webhook: " + payload);
        return "Webhook received";
    }

    // ================= BUY =================
    @PostMapping("/buy")
    public ResponseEntity<String> placeBuyOrder(@RequestBody String body) {
        try {
            System.out.println("buy Received webhook: " + body);

            List<DhanOrderRequest> orders = objectMapper.readValue(
                    body,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DhanOrderRequest.class)
            );

            StringBuilder resultLog = new StringBuilder();

            // BUY first
            for (DhanOrderRequest request : orders) {
                if ("BUY".equalsIgnoreCase(request.getTransactionType())) {
                    request.setTransactionType("BUY");
                    ResponseEntity<String> response = dhanService.placeOrder(request);
                    resultLog.append("BUY Response: ").append(response.getBody()).append("\n");
                }
            }

            // STOP LOSS (SELL)
            for (DhanOrderRequest request : orders) {
                if ("SELL".equalsIgnoreCase(request.getTransactionType())) {

                    request.setTransactionType("SELL");
                    request.setOrderType("STOP_LOSS");

                    if (request.getTriggerPrice() != 0.0) {
                        double trigger = request.getTriggerPrice();
                        double roundedTrigger = Math.floor(trigger * 20.0) / 20.0;
                        request.setTriggerPrice(new BigDecimal(roundedTrigger)
                                .setScale(2, RoundingMode.HALF_UP).doubleValue());

                        double rawPrice = roundedTrigger - 0.15;
                        double roundedPrice = Math.floor(rawPrice * 20.0) / 20.0;
                        request.setPrice(new BigDecimal(roundedPrice)
                                .setScale(2, RoundingMode.HALF_UP).doubleValue());
                    }

                    ResponseEntity<String> response = dhanService.placeOrder(request);
                    resultLog.append("STOP_LOSS Response: ").append(response.getBody()).append("\n");
                }
            }

            return ResponseEntity.ok(resultLog.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Invalid payload: " + e.getMessage());
        }
    }

    // ================= SELL =================
    @PostMapping("/sell")
    public ResponseEntity<String> placeSellOrder(@RequestBody String body) {
        try {
            System.out.println("sell Received webhook: " + body);

            DhanOrderRequest request = objectMapper.readValue(body, DhanOrderRequest.class);

            request.setTransactionType("SELL");
            request.setOrderType("STOP_LOSS");

            if (request.getTriggerPrice() != 0.0) {
                double trigger = request.getTriggerPrice();
                double roundedTrigger = Math.floor(trigger * 20.0) / 20.0;
                request.setTriggerPrice(new BigDecimal(roundedTrigger)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue());

                double rawPrice = roundedTrigger - 0.15;
                double roundedPrice = Math.floor(rawPrice * 20.0) / 20.0;
                request.setPrice(new BigDecimal(roundedPrice)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue());
            }

            return dhanService.placeOrder(request);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid payload");
        }
    }

    // ================= STOPLOSS =================
    @PostMapping("/stoploss")
    public ResponseEntity<String> placeStopLoss(@RequestBody String body) {
        try {
            System.out.println("stoploss Received webhook: " + body);

            DhanOrderRequest request = objectMapper.readValue(body, DhanOrderRequest.class);

            request.setTransactionType("SELL");
            request.setOrderType("STOP_LOSS");

            if (request.getTriggerPrice() != 0.0) {
                double rawPrice = request.getTriggerPrice() - 0.15;
                double roundedPrice = new BigDecimal(rawPrice)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
                request.setPrice(roundedPrice);
            }

            return dhanService.placeOrder(request);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid payload");
        }
    }

    // ================= PROXY =================
    @RequestMapping(
            value = "/proxy/**",
            method = {
                    RequestMethod.GET,
                    RequestMethod.POST,
                    RequestMethod.PUT,
                    RequestMethod.DELETE
            }
    )
    public ResponseEntity<?> forward(
            HttpMethod method,
            HttpServletRequest request,
            @RequestBody(required = false) String body
    ) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            String path = request.getRequestURI().replaceFirst("/proxy", "");
            String query = request.getQueryString();

            String url = "http://64.227.143.158" + path + (query != null ? "?" + query : "");

            System.out.println("Forwarding to: " + url);

            HttpEntity<String> entity = new HttpEntity<>(body);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    method,
                    entity,
                    String.class
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}
