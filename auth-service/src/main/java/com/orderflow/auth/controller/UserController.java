package com.orderflow.auth.controller;

import com.orderflow.auth.dto.UpdateRoleRequest;
import com.orderflow.auth.dto.UserResponse;
import com.orderflow.auth.exception.ForbiddenOperationException;
import com.orderflow.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/users")
@Tag(name = "Users", description = "Gestão de usuários (somente ADMIN/super-admin)")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    private static boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    private static boolean isSuperAdmin(String header) {
        return "true".equalsIgnoreCase(header);
    }

    @GetMapping
    @Operation(summary = "Lista usuários cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de usuários")
    @ApiResponse(responseCode = "403", description = "Caller não é ADMIN")
    public ResponseEntity<List<UserResponse>> list(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdmin(role)) {
            throw new ForbiddenOperationException("Apenas administradores podem listar usuários");
        }
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário")
    public ResponseEntity<UserResponse> findById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdmin(role)) {
            throw new ForbiddenOperationException("Apenas administradores podem ver usuários");
        }
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Promove ou remove ADMIN de um usuário (apenas super-admin)")
    @ApiResponse(responseCode = "200", description = "Role atualizado")
    @ApiResponse(responseCode = "403", description = "Caller não é super-admin ou alvo é o admin original")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Super-Admin", required = false) String superAdminHeader,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(service.updateRole(id, request.role(), isSuperAdmin(superAdminHeader)));
    }
}
