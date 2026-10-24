package com.orderflow.order.repository;

import com.orderflow.order.dto.AdminActivityRow;
import com.orderflow.order.dto.OrderSummary;
import com.orderflow.order.dto.UserOrdersSummary;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o")
    Page<Order> findAllWithItems(Pageable pageable);

    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.customerEmail = :email")
    Page<Order> findByCustomerEmail(@Param("email") String email, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    @Query("""
        select o from Order o
        where (:email is null or o.customerEmail = :email)
          and (:status is null or o.status = :status)
          and (:confirmedBy is null or o.confirmedByEmail = :confirmedBy)
          and (:cancelledBy is null or (o.cancelledByEmail = :cancelledBy and o.cancellationMethod = 'ADMIN_MANUAL'))
        """)
    Page<Order> findFiltered(@Param("email") String email,
                             @Param("status") OrderStatus status,
                             @Param("confirmedBy") String confirmedBy,
                             @Param("cancelledBy") String cancelledBy,
                             Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findById(Long id);

    @Query("""
        select new com.orderflow.order.dto.OrderSummary(
            count(o),
            coalesce(sum(o.totalAmount), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.PENDING   then 1 else 0 end), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.CONFIRMED then 1 else 0 end), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.CANCELLED then 1 else 0 end), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.PENDING   then o.totalAmount else 0 end), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.CONFIRMED then o.totalAmount else 0 end), 0)
        )
        from Order o
        where o.customerEmail = :email
        """)
    OrderSummary summaryByCustomerEmail(@Param("email") String email);

    @Query("""
        select new com.orderflow.order.dto.OrderSummary(
            count(o),
            coalesce(sum(o.totalAmount), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.PENDING   then 1 else 0 end), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.CONFIRMED then 1 else 0 end), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.CANCELLED then 1 else 0 end), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.PENDING   then o.totalAmount else 0 end), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.CONFIRMED then o.totalAmount else 0 end), 0)
        )
        from Order o
        """)
    OrderSummary globalSummary();

    @Query("""
        select new com.orderflow.order.dto.UserOrdersSummary(
            o.customerName,
            o.customerEmail,
            count(o),
            coalesce(sum(o.totalAmount), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.PENDING   then o.totalAmount else 0 end), 0),
            coalesce(sum(case when o.status = com.orderflow.order.entity.OrderStatus.CONFIRMED then o.totalAmount else 0 end), 0)
        )
        from Order o
        where o.customerEmail is not null
        group by o.customerEmail, o.customerName
        order by o.customerName
        """)
    List<UserOrdersSummary> summaryByUser();

    @Query("""
        select new com.orderflow.order.dto.AdminActivityRow(
            o.confirmedByEmail,
            o.confirmedByName,
            count(o),
            coalesce(sum(o.totalAmount), 0)
        )
        from Order o
        where o.confirmedByEmail is not null
          and o.status = com.orderflow.order.entity.OrderStatus.CONFIRMED
        group by o.confirmedByEmail, o.confirmedByName
        """)
    List<AdminActivityRow> confirmedByAdmin();

    @Query("""
        select new com.orderflow.order.dto.AdminActivityRow(
            o.cancelledByEmail,
            o.cancelledByName,
            count(o),
            coalesce(sum(o.totalAmount), 0)
        )
        from Order o
        where o.cancelledByEmail is not null
          and o.cancellationMethod = 'ADMIN_MANUAL'
        group by o.cancelledByEmail, o.cancelledByName
        """)
    List<AdminActivityRow> cancelledByAdmin();
}
