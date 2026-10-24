package com.orderflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload de login")
public record LoginRequest(
        @Schema(example = "cesar@email.com")
        @NotBlank @Email String email,

        @Schema(example = "senha123")
        @NotBlank String password
) {}
