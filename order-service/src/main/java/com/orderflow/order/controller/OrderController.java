package com.orderflow.order.controller;

import com.orderflow.order.dto.AdminActivitySummary;
import com.orderflow.order.dto.CancelOrderRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.OrderSummary;
import com.orderflow.order.dto.UserOrdersSummary;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Gestão de pedidos")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    private static boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    @PostMapping
    @Operation(summary = "Cria um novo pedido")
    @ApiResponse(responseCode = "201", description = "Pedido criado")
    public ResponseEntity<OrderResponse> create(
            @RequestHeader("X-User-Name") String userName,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse resp = service.create(userName, userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    @Operation(summary = "Lista pedidos. Filtros (todos opcionais; alguns só admin): status, customerEmail, confirmedBy, cancelledBy.")
    public ResponseEntity<Page<OrderResponse>> list(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String confirmedBy,
            @RequestParam(required = false) String cancelledBy,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.list(
                pageable, userEmail, isAdmin(userRole),
                status, customerEmail, confirmedBy, cancelledBy));
    }

    @GetMapping("/summary")
    @Operation(summary = "Totalizadores: contagem e valor por status. USER vê só os próprios, ADMIN vê tudo (ou filtra por customerEmail).")
    public ResponseEntity<OrderSummary> summary(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(required = false) String customerEmail) {
        return ResponseEntity.ok(service.summary(userEmail, isAdmin(userRole), customerEmail));
    }

    @GetMapping("/summary/by-user")
    @Operation(summary = "Resumo por usuário (apenas ADMIN)")
    @ApiResponse(responseCode = "200", description = "Lista de totalizadores por cliente")
    @ApiResponse(responseCode = "403", description = "Usuário não é ADMIN")
    public ResponseEntity<List<UserOrdersSummary>> summaryByUser(
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(service.summaryByUser(isAdmin(userRole)));
    }

    @GetMapping("/admin-activity")
    @Operation(summary = "Atividade dos administradores: confirmações e cancelamentos manuais por admin")
    @ApiResponse(responseCode = "200", description = "Lista por administrador")
    @ApiResponse(responseCode = "403", description = "Usuário não é ADMIN")
    public ResponseEntity<List<AdminActivitySummary>> adminActivity(
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(service.adminActivity(isAdmin(userRole)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca pedido por ID. USER só vê os próprios, ADMIN vê qualquer.")
    @ApiResponse(responseCode = "200", description = "Encontrado")
    @ApiResponse(responseCode = "403", description = "Pedido pertence a outro usuário")
    @ApiResponse(responseCode = "404", description = "Não encontrado")
    public ResponseEntity<OrderResponse> findById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(service.findById(id, userEmail, isAdmin(userRole)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancela um pedido em status PENDING (dono ou ADMIN). Body opcional: { reason }")
    @ApiResponse(responseCode = "200", description = "Cancelado")
    @ApiResponse(responseCode = "403", description = "Não é o dono do pedido")
    @ApiResponse(responseCode = "409", description = "Status não permite cancelamento")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String userName,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestBody(required = false) CancelOrderRequest body) {
        String reason = body == null ? null : body.reason();
        return ResponseEntity.ok(service.cancel(id, userName, userEmail, isAdmin(userRole), reason));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirma um pedido em status PENDING (apenas ADMIN)")
    @ApiResponse(responseCode = "200", description = "Confirmado")
    @ApiResponse(responseCode = "403", description = "Usuário não é ADMIN")
    @ApiResponse(responseCode = "409", description = "Status não permite confirmação")
    public ResponseEntity<OrderResponse> confirm(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        return ResponseEntity.ok(service.confirm(id, userRole, userName, userEmail));
    }
}
