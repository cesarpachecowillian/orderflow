package com.orderflow.order.dto;

import com.orderflow.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        String customerEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        LocalDateTime confirmedAt,
        String confirmedByName,
        String confirmedByEmail,
        String confirmationMethod,
        LocalDateTime cancelledAt,
        String cancelledByName,
        String cancelledByEmail,
        String cancellationReason,
        String cancellationMethod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
