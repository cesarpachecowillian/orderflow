package com.orderflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de login com JWT")
public record AuthResponse(
        @Schema(description = "JWT Bearer token") String token,
        @Schema(example = "Bearer") String type,
        @Schema(example = "86400000") long expiresInMs
) {
    public static AuthResponse of(String token, long expiresInMs) {
        return new AuthResponse(token, "Bearer", expiresInMs);
    }
}
