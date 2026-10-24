package com.orderflow.order.dto;

import java.math.BigDecimal;

public record OrderSummary(
        long totalOrders,
        BigDecimal totalAmount,
        long pendingCount,
        long confirmedCount,
        long cancelledCount,
        BigDecimal pendingAmount,
        BigDecimal confirmedAmount
) {
    public static OrderSummary empty() {
        return new OrderSummary(0L, BigDecimal.ZERO, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
