package com.orderflow.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderEventDTO(
        String type,
        Long orderId,
        String customerName,
        String customerEmail,
        BigDecimal totalAmount,
        Integer itemCount,
        LocalDateTime timestamp
) {}
