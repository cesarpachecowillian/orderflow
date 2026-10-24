package com.orderflow.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Item de pedido")
public record CreateOrderItemRequest(
        @Schema(example = "Notebook")
        @NotBlank String productName,

        @Schema(example = "1")
        @NotNull @Min(1) Integer quantity,

        @Schema(example = "4500.00")
        @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice
) {}
