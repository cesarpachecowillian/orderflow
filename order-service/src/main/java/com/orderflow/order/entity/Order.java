package com.orderflow.order.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by_name", length = 100)
    private String confirmedByName;

    @Column(name = "confirmed_by_email", length = 150)
    private String confirmedByEmail;

    @Column(name = "confirmation_method", length = 30)
    private String confirmationMethod;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by_name", length = 100)
    private String cancelledByName;

    @Column(name = "cancelled_by_email", length = 150)
    private String cancelledByEmail;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancellation_method", length = 30)
    private String cancellationMethod;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Order(String customerName, String customerEmail) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isCancellable() {
        return status == OrderStatus.PENDING;
    }

    public boolean isConfirmable() {
        return status == OrderStatus.PENDING;
    }

    public void cancel(String byName, String byEmail, String reason, String method) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledByName = byName;
        this.cancelledByEmail = byEmail;
        this.cancellationReason = reason;
        this.cancellationMethod = method;
    }

    public void confirm(String byName, String byEmail, String method) {
        this.status = OrderStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.confirmedByName = byName;
        this.confirmedByEmail = byEmail;
        this.confirmationMethod = method;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public String getConfirmedByName() { return confirmedByName; }
    public String getConfirmedByEmail() { return confirmedByEmail; }
    public String getConfirmationMethod() { return confirmationMethod; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancelledByName() { return cancelledByName; }
    public String getCancelledByEmail() { return cancelledByEmail; }
    public String getCancellationReason() { return cancellationReason; }
    public String getCancellationMethod() { return cancellationMethod; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return items; }
}
