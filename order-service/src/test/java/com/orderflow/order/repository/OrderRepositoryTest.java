package com.orderflow.order.repository;

import com.orderflow.order.dto.OrderSummary;
import com.orderflow.order.dto.UserOrdersSummary;
import java.util.List;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderItem;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class OrderRepositoryTest {

    @Autowired OrderRepository repo;
    @Autowired ProductRepository productRepo;

    Product notebook;
    Product mouse;

    @BeforeEach
    void setUpProducts() {
        notebook = productRepo.save(new Product("Notebook", new BigDecimal("4500.00")));
        mouse = productRepo.save(new Product("Mouse", new BigDecimal("100.00")));
    }

    private Order order(String name, String email, OrderStatus status, String price) {
        Order o = new Order(name, email);
        o.addItem(new OrderItem(mouse.getId(), "Item", 1, new BigDecimal(price)));
        o.recalculateTotal();
        if (status != OrderStatus.PENDING) o.setStatus(status);
        return o;
    }

    @Test
    void saveCascadesItems_andLoadsViaEntityGraph() {
        Order o = new Order("César", "cesar@email.com");
        o.addItem(new OrderItem(notebook.getId(), "Notebook", 1, new BigDecimal("4500.00")));
        o.addItem(new OrderItem(mouse.getId(), "Mouse", 2, new BigDecimal("100.00")));
        o.recalculateTotal();
        Long id = repo.save(o).getId();

        Order loaded = repo.findById(id).orElseThrow();
        assertThat(loaded.getItems()).hasSize(2);
        assertThat(loaded.getItems()).extracting(OrderItem::getProductId)
                .containsExactlyInAnyOrder(notebook.getId(), mouse.getId());
        assertThat(loaded.getCustomerEmail()).isEqualTo("cesar@email.com");
        assertThat(loaded.getTotalAmount()).isEqualByComparingTo("4700.00");
    }

    @Test
    void findByCustomerEmail_filtersCorrectly() {
        repo.save(order("Maria", "maria@e.com", OrderStatus.PENDING, "100.00"));
        repo.save(order("Maria", "maria@e.com", OrderStatus.CONFIRMED, "200.00"));
        repo.save(order("Bob", "bob@e.com", OrderStatus.PENDING, "50.00"));

        var maria = repo.findByCustomerEmail("maria@e.com", PageRequest.of(0, 10));
        var bob = repo.findByCustomerEmail("bob@e.com", PageRequest.of(0, 10));

        assertThat(maria.getTotalElements()).isEqualTo(2);
        assertThat(bob.getTotalElements()).isEqualTo(1);
    }

    @Test
    void summaryByCustomerEmail_aggregatesCorrectly() {
        repo.save(order("Maria", "maria@e.com", OrderStatus.PENDING, "100.00"));
        repo.save(order("Maria", "maria@e.com", OrderStatus.CONFIRMED, "200.00"));
        repo.save(order("Maria", "maria@e.com", OrderStatus.CANCELLED, "50.00"));
        repo.save(order("Bob", "bob@e.com", OrderStatus.CONFIRMED, "999.00"));

        OrderSummary s = repo.summaryByCustomerEmail("maria@e.com");
        assertThat(s.totalOrders()).isEqualTo(3);
        assertThat(s.totalAmount()).isEqualByComparingTo("350.00");
        assertThat(s.pendingCount()).isEqualTo(1);
        assertThat(s.confirmedCount()).isEqualTo(1);
        assertThat(s.cancelledCount()).isEqualTo(1);
        assertThat(s.pendingAmount()).isEqualByComparingTo("100.00");
        assertThat(s.confirmedAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void summaryByUser_groupsAndAggregatesPerCustomer() {
        repo.save(order("Maria", "maria@e.com", OrderStatus.PENDING, "100.00"));
        repo.save(order("Maria", "maria@e.com", OrderStatus.CONFIRMED, "200.00"));
        repo.save(order("Bob", "bob@e.com", OrderStatus.CONFIRMED, "999.00"));
        repo.save(order("Bob", "bob@e.com", OrderStatus.PENDING, "50.00"));

        List<UserOrdersSummary> rows = repo.summaryByUser();

        assertThat(rows).hasSize(2);
        UserOrdersSummary bob = rows.stream().filter(r -> r.customerEmail().equals("bob@e.com")).findFirst().orElseThrow();
        UserOrdersSummary maria = rows.stream().filter(r -> r.customerEmail().equals("maria@e.com")).findFirst().orElseThrow();
        assertThat(maria.totalOrders()).isEqualTo(2);
        assertThat(maria.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(maria.pendingAmount()).isEqualByComparingTo("100.00");
        assertThat(maria.confirmedAmount()).isEqualByComparingTo("200.00");
        assertThat(bob.totalOrders()).isEqualTo(2);
        assertThat(bob.pendingAmount()).isEqualByComparingTo("50.00");
        assertThat(bob.confirmedAmount()).isEqualByComparingTo("999.00");
    }

    @Test
    void summaryByCustomerEmail_emptyUser_returnsZeros() {
        OrderSummary s = repo.summaryByCustomerEmail("ghost@e.com");
        assertThat(s.totalOrders()).isEqualTo(0);
        assertThat(s.totalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void globalSummary_aggregatesAcrossAllUsers() {
        repo.save(order("Maria", "maria@e.com", OrderStatus.PENDING, "100.00"));
        repo.save(order("Bob", "bob@e.com", OrderStatus.CONFIRMED, "200.00"));

        OrderSummary s = repo.globalSummary();
        assertThat(s.totalOrders()).isEqualTo(2);
        assertThat(s.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(s.confirmedAmount()).isEqualByComparingTo("200.00");
    }
}
