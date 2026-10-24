package com.orderflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Estrutura padrão de erro")
public record ErrorResponse(
        int status,
        String error,
        String message,
        List<String> details,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(int status, String error, String message, List<String> details) {
        return new ErrorResponse(status, error, message, details, LocalDateTime.now());
    }
}
