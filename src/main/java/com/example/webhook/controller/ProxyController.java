package com.example.webhook.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    @Autowired
    private RestTemplate restTemplate;

    private final String TARGET_BASE = "http://64.227.143.158";

    @PostMapping("/**")
    public ResponseEntity<String> forwardPost(
            HttpServletRequest request,
            @RequestBody(required = false) String body
    ) {

        String forwardPath = request.getRequestURI().replace("/proxy", "");
        String targetUrl = TARGET_BASE + forwardPath;

        HttpHeaders headers = extractHeaders(request);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                targetUrl,
                HttpMethod.POST,
                entity,
                String.class
        );
    }

    @GetMapping("/**")
    public ResponseEntity<String> forwardGet(HttpServletRequest request) {

        String forwardPath = request.getRequestURI().replace("/proxy", "");
        String targetUrl = TARGET_BASE + forwardPath;

        HttpHeaders headers = extractHeaders(request);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                targetUrl,
                HttpMethod.GET,
                entity,
                String.class
        );
    }

    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();

        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.add(headerName, request.getHeader(headerName));
        }

        return headers;
    }
}
