package com.example.webhook.repository;

import com.example.webhook.entity.OrderLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderLogRepository extends JpaRepository<OrderLog, Long> {
    Optional<OrderLog> findTopBySecurityIdAndOrderTypeOrderByIdDesc(String securityId, String orderType);
}