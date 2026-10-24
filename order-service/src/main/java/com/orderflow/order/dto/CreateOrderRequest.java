package com.orderflow.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Payload de criação de pedido")
public record CreateOrderRequest(
        @NotEmpty @Valid List<CreateOrderItemRequest> items
) {}
