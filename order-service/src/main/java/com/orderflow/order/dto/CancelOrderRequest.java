package com.orderflow.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload opcional de cancelamento")
public record CancelOrderRequest(
        @Schema(example = "Cliente solicitou substituição do produto") @Size(max = 500) String reason
) {}
