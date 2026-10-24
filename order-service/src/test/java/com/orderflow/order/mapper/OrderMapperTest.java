package com.orderflow.order.mapper;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderItem;
import com.orderflow.order.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toEntity_fromRequest() {
        var req = new CreateOrderItemRequest("Mouse", 2, new BigDecimal("99.90"));
        OrderItem item = mapper.toEntity(req);
        assertThat(item.getProductName()).isEqualTo("Mouse");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("99.90");
    }

    @Test
    void toResponse_includesSubtotalsAndTotalsAndCustomerEmail() {
        Order order = new Order("César", "cesar@email.com");
        order.setId(7L);
        order.addItem(new OrderItem("Notebook", 1, new BigDecimal("4500.00")));
        order.addItem(new OrderItem("Mouse", 3, new BigDecimal("100.00")));
        order.recalculateTotal();

        OrderResponse resp = mapper.toResponse(order);

        assertThat(resp.id()).isEqualTo(7L);
        assertThat(resp.customerName()).isEqualTo("César");
        assertThat(resp.customerEmail()).isEqualTo("cesar@email.com");
        assertThat(resp.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(resp.totalAmount()).isEqualByComparingTo("4800.00");
        assertThat(resp.items()).hasSize(2);
        assertThat(resp.items().get(0).subtotal()).isEqualByComparingTo("4500.00");
        assertThat(resp.confirmedAt()).isNull();
        assertThat(resp.confirmationMethod()).isNull();
    }

    @Test
    void toResponse_includesConfirmationDataWhenPresent() {
        Order order = new Order("César", "cesar@email.com");
        order.setId(7L);
        order.addItem(new OrderItem("Item", 1, new BigDecimal("10.00")));
        order.recalculateTotal();
        order.confirm("Admin", "admin@orderflow.com", "ADMIN_MANUAL");

        OrderResponse resp = mapper.toResponse(order);

        assertThat(resp.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(resp.confirmedAt()).isNotNull();
        assertThat(resp.confirmedByName()).isEqualTo("Admin");
        assertThat(resp.confirmedByEmail()).isEqualTo("admin@orderflow.com");
        assertThat(resp.confirmationMethod()).isEqualTo("ADMIN_MANUAL");
    }
}
