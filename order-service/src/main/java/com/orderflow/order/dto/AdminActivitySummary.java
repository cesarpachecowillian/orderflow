package com.orderflow.order.dto;

import java.math.BigDecimal;

public record AdminActivitySummary(
        String adminName,
        String adminEmail,
        long confirmedCount,
        BigDecimal confirmedAmount,
        long cancelledCount,
        BigDecimal cancelledAmount
) {}
