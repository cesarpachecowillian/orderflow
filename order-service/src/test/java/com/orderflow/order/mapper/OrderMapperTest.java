package com.orderflow.order.mapper;

import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderItem;
import com.orderflow.order.entity.Product;
import com.orderflow.order.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toEntity_usesNameAndPriceFromProduct() {
        Product product = new Product("Mouse Logitech MX Master 3S", new BigDecimal("549.90"));
        product.setId(2L);

        OrderItem item = mapper.toEntity(product, 2);

        assertThat(item.getProductId()).isEqualTo(2L);
        assertThat(item.getProductName()).isEqualTo("Mouse Logitech MX Master 3S");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("549.90");
    }

    @Test
    void toResponse_includesSubtotalsAndTotalsAndCustomerEmail() {
        Order order = new Order("César", "cesar@email.com");
        order.setId(7L);
        order.addItem(new OrderItem(1L, "Notebook", 1, new BigDecimal("4500.00")));
        order.addItem(new OrderItem(2L, "Mouse", 3, new BigDecimal("100.00")));
        order.recalculateTotal();

        OrderResponse resp = mapper.toResponse(order);

        assertThat(resp.id()).isEqualTo(7L);
        assertThat(resp.customerName()).isEqualTo("César");
        assertThat(resp.customerEmail()).isEqualTo("cesar@email.com");
        assertThat(resp.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(resp.totalAmount()).isEqualByComparingTo("4800.00");
        assertThat(resp.items()).hasSize(2);
        assertThat(resp.items().get(0).productId()).isEqualTo(1L);
        assertThat(resp.items().get(0).subtotal()).isEqualByComparingTo("4500.00");
        assertThat(resp.confirmedAt()).isNull();
        assertThat(resp.confirmationMethod()).isNull();
    }

    @Test
    void toResponse_includesConfirmationDataWhenPresent() {
        Order order = new Order("César", "cesar@email.com");
        order.setId(7L);
        order.addItem(new OrderItem(1L, "Item", 1, new BigDecimal("10.00")));
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
