package com.example.webhook.controller;

import com.example.webhook.model.DhanOrderRequest;
import com.example.webhook.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class DhanController {

    @Autowired
    private DhanService dhanService;

    @PostMapping("/webhook")
    public String receiveWebhook(@RequestBody Map<String, Object> payload) {
        System.out.println("Received webhook: " + payload);
        
        return "Webhook received";
    }
    
    @PostMapping("/buy")
    public ResponseEntity<String> placeBuyOrder(@RequestBody DhanOrderRequest request) {
        request.setTransactionType("BUY");
        return dhanService.placeOrder(request);
    }

    @PostMapping("/sell")
    public ResponseEntity<String> placeSellOrder(@RequestBody DhanOrderRequest request) {
        request.setTransactionType("SELL");
        return dhanService.placeOrder(request);
    }

    @PostMapping("/stoploss")
    public ResponseEntity<String> placeStopLoss(@RequestBody DhanOrderRequest request) {
        request.setTransactionType("SELL");
        request.setOrderType("SL");
        return dhanService.placeOrder(request);
    }
}
