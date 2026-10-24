package com.orderflow.order.dto;

import java.math.BigDecimal;

public record UserOrdersSummary(
        String customerName,
        String customerEmail,
        long totalOrders,
        BigDecimal totalAmount,
        BigDecimal pendingAmount,
        BigDecimal confirmedAmount
) {}
