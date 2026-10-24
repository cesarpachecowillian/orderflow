package com.orderflow.order.service;

import com.orderflow.order.dto.AdminActivityRow;
import com.orderflow.order.dto.AdminActivitySummary;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderEventDTO;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.OrderSummary;
import com.orderflow.order.dto.UserOrdersSummary;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.OrderNotCancellableException;
import com.orderflow.order.exception.OrderNotConfirmableException;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.exception.UnauthorizedAccessException;
import com.orderflow.order.mapper.OrderMapper;
import com.orderflow.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final String CONFIRMATION_METHOD_ADMIN = "ADMIN_MANUAL";
    private static final String CANCELLATION_METHOD_USER = "USER_REQUEST";
    private static final String CANCELLATION_METHOD_ADMIN = "ADMIN_MANUAL";
    private static final String DEFAULT_REASON_USER = "Cancelado a pedido do cliente";
    private static final String DEFAULT_REASON_ADMIN = "Cancelado pela administração";

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OrderEventPublisher publisher;

    public OrderService(OrderRepository repository, OrderMapper mapper, OrderEventPublisher publisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.publisher = publisher;
    }

    @Transactional
    public OrderResponse create(String userName, String userEmail, CreateOrderRequest request) {
        Order order = new Order(userName, userEmail);
        request.items().forEach(item -> order.addItem(mapper.toEntity(item)));
        order.recalculateTotal();
        Order saved = repository.save(order);

        publisher.publish(OrderEventDTO.created(
                saved.getId(),
                saved.getCustomerName(),
                saved.getCustomerEmail(),
                saved.getTotalAmount(),
                saved.getItems().size()
        ));

        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(Pageable pageable, String userEmail, boolean isAdmin,
                                    OrderStatus status, String filterCustomerEmail,
                                    String confirmedBy, String cancelledBy) {
        // Não-admin é forçado a ver só os próprios pedidos, ignora filtros admin.
        String email = isAdmin ? blankToNull(filterCustomerEmail) : userEmail;
        String confBy = isAdmin ? blankToNull(confirmedBy) : null;
        String canBy = isAdmin ? blankToNull(cancelledBy) : null;
        return repository.findFiltered(email, status, confBy, canBy, pageable).map(mapper::toResponse);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    @Transactional(readOnly = true)
    public OrderSummary summary(String userEmail, boolean isAdmin, String filterCustomerEmail) {
        OrderSummary s;
        if (isAdmin) {
            String email = blankToNull(filterCustomerEmail);
            s = email != null ? repository.summaryByCustomerEmail(email) : repository.globalSummary();
        } else {
            s = repository.summaryByCustomerEmail(userEmail);
        }
        return s == null ? OrderSummary.empty() : s;
    }

    @Transactional(readOnly = true)
    public List<UserOrdersSummary> summaryByUser(boolean isAdmin) {
        if (!isAdmin) {
            throw new UnauthorizedAccessException("Apenas administradores podem ver o resumo por usuário");
        }
        return repository.summaryByUser();
    }

    @Transactional(readOnly = true)
    public List<AdminActivitySummary> adminActivity(boolean isAdmin) {
        if (!isAdmin) {
            throw new UnauthorizedAccessException("Apenas administradores podem ver a atividade dos administradores");
        }
        Map<String, AdminActivitySummary> byEmail = new LinkedHashMap<>();
        for (AdminActivityRow row : repository.confirmedByAdmin()) {
            byEmail.put(row.email(), new AdminActivitySummary(
                    row.name(), row.email(), row.count(), row.amount(), 0L, BigDecimal.ZERO));
        }
        for (AdminActivityRow row : repository.cancelledByAdmin()) {
            AdminActivitySummary existing = byEmail.get(row.email());
            if (existing == null) {
                byEmail.put(row.email(), new AdminActivitySummary(
                        row.name(), row.email(), 0L, BigDecimal.ZERO, row.count(), row.amount()));
            } else {
                byEmail.put(row.email(), new AdminActivitySummary(
                        existing.adminName(), existing.adminEmail(),
                        existing.confirmedCount(), existing.confirmedAmount(),
                        row.count(), row.amount()));
            }
        }
        return byEmail.values().stream()
                .sorted((a, b) -> a.adminName().compareToIgnoreCase(b.adminName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id, String userEmail, boolean isAdmin) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!isAdmin && !userEmail.equals(order.getCustomerEmail())) {
            throw new UnauthorizedAccessException();
        }
        return mapper.toResponse(order);
    }

    @Transactional
    public OrderResponse confirm(Long id, String userRole, String userName, String userEmail) {
        if (!"ADMIN".equals(userRole)) {
            throw new UnauthorizedAccessException("Apenas administradores podem confirmar pedidos");
        }
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.isConfirmable()) {
            throw new OrderNotConfirmableException(order.getStatus());
        }

        order.confirm(userName, userEmail, CONFIRMATION_METHOD_ADMIN);
        Order saved = repository.save(order);

        publisher.publish(OrderEventDTO.confirmed(
                saved.getId(),
                saved.getCustomerName(),
                saved.getCustomerEmail(),
                saved.getTotalAmount(),
                saved.getItems().size()
        ));

        return mapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse cancel(Long id, String userName, String userEmail, boolean isAdmin, String reason) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (!isAdmin && !order.getCustomerName().equals(userName)) {
            throw new UnauthorizedAccessException();
        }
        if (!order.isCancellable()) {
            throw new OrderNotCancellableException(order.getStatus());
        }

        String method = isAdmin ? CANCELLATION_METHOD_ADMIN : CANCELLATION_METHOD_USER;
        String defaultReason = isAdmin ? DEFAULT_REASON_ADMIN : DEFAULT_REASON_USER;
        String finalReason = (reason == null || reason.isBlank()) ? defaultReason : reason.trim();

        order.cancel(userName, userEmail, finalReason, method);
        Order saved = repository.save(order);

        publisher.publish(OrderEventDTO.cancelled(
                saved.getId(),
                saved.getCustomerName(),
                saved.getCustomerEmail(),
                saved.getTotalAmount(),
                saved.getItems().size()
        ));

        return mapper.toResponse(saved);
    }
}
