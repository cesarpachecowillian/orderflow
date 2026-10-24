package com.orderflow.order.dto;

import java.math.BigDecimal;

/** Linha intermediária retornada pelo repositório antes de ser fundida em AdminActivitySummary. */
public record AdminActivityRow(
        String email,
        String name,
        long count,
        BigDecimal amount
) {}
