package com.example.webhook.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.webhook.model.DhanOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DhanService {
	
    private static final Logger logger = LoggerFactory.getLogger(DhanService.class);


    @Value("${dhan.client-id}")
    private String clientId;

    @Value("${dhan.access-token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<String> placeOrder(DhanOrderRequest request) {
        try {
			logger.info("Received alert from TradingView: {}", objectMapper.writeValueAsString(request));

            String url = "https://api.dhan.co/v2/orders";

            HttpHeaders headers = new HttpHeaders();
            headers.set("access-token", accessToken);
            headers.set("Dhan-Client-Id", clientId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Add Dhan Client ID to request body
            request.setCorrelationId("order-" + System.currentTimeMillis());

            HttpEntity<DhanOrderRequest> entity = new HttpEntity<>(request, headers);

            return restTemplate.postForEntity(url, entity, String.class);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
}