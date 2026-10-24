package com.orderflow.auth.dto;

import com.orderflow.auth.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Atualização de role de um usuário")
public record UpdateRoleRequest(
        @Schema(example = "ADMIN") @NotNull Role role
) {}
