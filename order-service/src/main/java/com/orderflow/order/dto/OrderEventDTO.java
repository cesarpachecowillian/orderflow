package com.orderflow.order.dto;

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
) {
    public static OrderEventDTO created(Long id, String name, String email, BigDecimal total, int items) {
        return new OrderEventDTO("ORDER_CREATED", id, name, email, total, items, LocalDateTime.now());
    }

    public static OrderEventDTO cancelled(Long id, String name, String email, BigDecimal total, int items) {
        return new OrderEventDTO("ORDER_CANCELLED", id, name, email, total, items, LocalDateTime.now());
    }

    public static OrderEventDTO confirmed(Long id, String name, String email, BigDecimal total, int items) {
        return new OrderEventDTO("ORDER_CONFIRMED", id, name, email, total, items, LocalDateTime.now());
    }
}
