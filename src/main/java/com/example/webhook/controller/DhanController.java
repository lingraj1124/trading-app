package com.example.webhook.controller;

import com.example.webhook.model.DhanOrderRequest;
import com.example.webhook.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/webhook")
    public String receiveWebhook(@RequestBody Map<String, Object> payload) {
        System.out.println("Received webhook: " + payload);
        return "Webhook received";
    }

    @PostMapping("/buy")
    public ResponseEntity<String> placeBuyOrder(@RequestBody String body) {
//        try {
//            System.out.println("buy Received webhook: " + body);
//            DhanOrderRequest request = objectMapper.readValue(body, DhanOrderRequest.class);
//            request.setTransactionType("BUY");
//            return dhanService.placeOrder(request);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("Invalid payload");
//        }
    	
        try {
            System.out.println("buy Received webhook: " + body);

            List<DhanOrderRequest> orders = objectMapper.readValue(
                    body, objectMapper.getTypeFactory().constructCollectionType(List.class, DhanOrderRequest.class)
            );

            StringBuilder resultLog = new StringBuilder();

            // 1. Handle BUY orders first
            for (DhanOrderRequest request : orders) {
                if ("BUY".equalsIgnoreCase(request.getTransactionType())) {
                    request.setTransactionType("BUY");
                    ResponseEntity<String> response = dhanService.placeOrder(request);
                    resultLog.append("BUY Response: ").append(response.getBody()).append("\n");
                }
            }

            // 2. Then handle STOP_LOSS (SELL) orders
            // for (DhanOrderRequest request : orders) {
            //     if ("SELL".equalsIgnoreCase(request.getTransactionType())) {
            //         request.setTransactionType("SELL");
            //         request.setOrderType("STOP_LOSS");

            //         if (request.getTriggerPrice() != 0.0) {
            //             double rawPrice = request.getTriggerPrice() - 0.15;
            //             double roundedPrice = new BigDecimal(rawPrice)
            //                     .setScale(2, RoundingMode.HALF_UP)
            //                     .doubleValue();
            //             request.setPrice(roundedPrice);
            //         }

            //         ResponseEntity<String> response = dhanService.placeOrder(request);
            //         resultLog.append("STOP_LOSS Response: ").append(response.getBody()).append("\n");
            //     }
            // }

            // 2. Then handle STOP_LOSS (SELL) orders
            for (DhanOrderRequest request : orders) {
                if ("SELL".equalsIgnoreCase(request.getTransactionType())) {
                    // request.setTransactionType("SELL");
                    // request.setOrderType("STOP_LOSS");
            
                    // if (request.getTriggerPrice() != 0.0) {
                    //     // Round triggerPrice to nearest multiple of 5
                    //     double roundedTrigger = Math.round(request.getTriggerPrice() / 5.0) * 5.0;
                    //     BigDecimal triggerRounded = new BigDecimal(roundedTrigger).setScale(2, RoundingMode.HALF_UP);
                    //     request.setTriggerPrice(triggerRounded.doubleValue());
            
                    //     // Set price 0.15 below trigger and round to nearest 5
                    //     double rawPrice = triggerRounded.doubleValue() - 0.15;
                    //     double roundedPrice = Math.round(rawPrice / 5.0) * 5.0;
                    //     BigDecimal priceRounded = new BigDecimal(roundedPrice).setScale(2, RoundingMode.HALF_UP);
                    //     request.setPrice(priceRounded.doubleValue());
                    // }
            
                    // ResponseEntity<String> response = dhanService.placeOrder(request);
                    // resultLog.append("STOP_LOSS Response: ").append(response.getBody()).append("\n");

                    request.setTransactionType("SELL");
                    request.setOrderType("STOP_LOSS");
                    
                    if (request.getTriggerPrice() != 0.0) {
                        // Round trigger price down to nearest 0.05
                        double trigger = request.getTriggerPrice();
                        double roundedTrigger = Math.floor(trigger * 20.0) / 20.0; // 1/0.05 = 20
                        request.setTriggerPrice(new BigDecimal(roundedTrigger).setScale(2, RoundingMode.HALF_UP).doubleValue());
                    
                        // Calculate price = trigger - 0.15 and round down to nearest 0.05
                        double rawPrice = roundedTrigger - 0.15;
                        double roundedPrice = Math.floor(rawPrice * 20.0) / 20.0;
                        request.setPrice(new BigDecimal(roundedPrice).setScale(2, RoundingMode.HALF_UP).doubleValue());
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

    @PostMapping("/sell")
    public ResponseEntity<String> placeSellOrder(@RequestBody String body) {
        try {
            System.out.println("sell Received webhook: " + body);
            DhanOrderRequest request = objectMapper.readValue(body, DhanOrderRequest.class);
            // request.setTransactionType("SELL");
            // request.setOrderType("STOP_LOSS");

            // if (request.getTriggerPrice() != 0.0) {
            //     double rawPrice = request.getTriggerPrice() - 0.15;
            //     double roundedPrice = new BigDecimal(rawPrice)
            //                             .setScale(2, RoundingMode.HALF_UP)
            //                             .doubleValue();
            //     request.setPrice(roundedPrice);
            // }

            request.setTransactionType("SELL");
            request.setOrderType("STOP_LOSS");
            
            if (request.getTriggerPrice() != 0.0) {
                // Round trigger price down to nearest 0.05
                double trigger = request.getTriggerPrice();
                double roundedTrigger = Math.floor(trigger * 20.0) / 20.0; // 1/0.05 = 20
                request.setTriggerPrice(new BigDecimal(roundedTrigger).setScale(2, RoundingMode.HALF_UP).doubleValue());
            
                // Calculate price = trigger - 0.15 and round down to nearest 0.05
                double rawPrice = roundedTrigger - 0.15;
                double roundedPrice = Math.floor(rawPrice * 20.0) / 20.0;
                request.setPrice(new BigDecimal(roundedPrice).setScale(2, RoundingMode.HALF_UP).doubleValue());
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
