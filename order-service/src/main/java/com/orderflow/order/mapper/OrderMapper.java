package com.orderflow.order.mapper;

import com.orderflow.order.dto.OrderItemResponse;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderItem;
import com.orderflow.order.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderMapper {

    /** Snapshot do produto no momento da compra: nome e preço vêm sempre do catálogo, nunca do request. */
    public OrderItem toEntity(Product product, Integer quantity) {
        return new OrderItem(product.getId(), product.getName(), quantity, product.getPrice());
    }

    public OrderResponse toResponse(Order order) {
        var items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getConfirmedAt(),
                order.getConfirmedByName(),
                order.getConfirmedByEmail(),
                order.getConfirmationMethod(),
                order.getCancelledAt(),
                order.getCancelledByName(),
                order.getCancelledByEmail(),
                order.getCancellationReason(),
                order.getCancellationMethod(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                subtotal
        );
    }
}
