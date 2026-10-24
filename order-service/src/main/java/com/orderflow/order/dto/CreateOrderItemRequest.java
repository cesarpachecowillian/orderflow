package com.orderflow.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Item de pedido. O preço é definido pelo catálogo no servidor.")
public record CreateOrderItemRequest(
        @Schema(example = "1", description = "ID do produto no catálogo")
        @NotNull Long productId,

        @Schema(example = "1")
        @NotNull @Min(1) Integer quantity
) {}
