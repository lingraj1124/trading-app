package com.example.webhook.controller;

import com.example.webhook.model.DhanOrderRequest;
import com.example.webhook.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.math.RoundingMode;
import java.math.BigDecimal;

@RestController
public class DhanController {

    @Autowired
    private DhanService dhanService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public String receiveWebhook(@RequestBody Map<String, Object> payload) {
        System.out.println("Received webhook: " + payload);
        return "Webhook received";
    }

    @PostMapping("/buy")
    public ResponseEntity<String> placeBuyOrder(@RequestBody String body) {
        try {
            System.out.println("buy Received webhook: " + body);
            DhanOrderRequest request = objectMapper.readValue(body, DhanOrderRequest.class);
            request.setTransactionType("BUY");
            return dhanService.placeOrder(request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid payload");
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<String> placeSellOrder(@RequestBody String body) {
        try {
            System.out.println("sell Received webhook: " + body);
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
}

