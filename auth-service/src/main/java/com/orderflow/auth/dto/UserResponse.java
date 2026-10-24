package com.orderflow.auth.dto;

import com.orderflow.auth.entity.Role;
import com.orderflow.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dados públicos do usuário (sem senha)")
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean superAdmin,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isSuperAdmin(), u.getCreatedAt());
    }
}
