package com.orderflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de registro de usuário")
public record RegisterRequest(
        @Schema(example = "César William")
        @NotBlank @Size(max = 100) String name,

        @Schema(example = "cesar@email.com")
        @NotBlank @Email @Size(max = 150) String email,

        @Schema(example = "senha123")
        @NotBlank @Size(min = 6, max = 100) String password
) {}
