package com.example.webhook.controller;

import com.example.webhook.model.DhanOrderRequest;
import com.example.webhook.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class HealthCheckController {
    @GetMapping("/")
    public String health() {
        return "✅ App is alive!";
    }
}