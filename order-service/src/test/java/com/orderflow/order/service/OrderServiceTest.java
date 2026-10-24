package com.orderflow.order.service;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderEventDTO;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderItem;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.entity.Product;
import com.orderflow.order.exception.OrderNotCancellableException;
import com.orderflow.order.exception.OrderNotConfirmableException;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.exception.ProductNotFoundException;
import com.orderflow.order.exception.UnauthorizedAccessException;
import com.orderflow.order.mapper.OrderMapper;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository repository;
    @Mock OrderEventPublisher publisher;
    @Mock ProductRepository productRepository;
    OrderMapper mapper = new OrderMapper();

    OrderService service;

    private OrderService svc() {
        return new OrderService(repository, mapper, publisher, productRepository);
    }

    private static Product product(Long id, String name, String price) {
        Product p = new Product(name, new BigDecimal(price));
        p.setId(id);
        return p;
    }

    @Test
    void create_calculatesTotal_persists_andPublishesEvent_withCustomerEmail() {
        service = svc();
        when(productRepository.findAllByIdInAndActiveTrue(any())).thenReturn(List.of(
                product(1L, "Notebook", "4500.00"),
                product(2L, "Mouse", "100.00")));
        var req = new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(1L, 1),
                new CreateOrderItemRequest(2L, 2)
        ));
        when(repository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(42L);
            return o;
        });

        OrderResponse resp = service.create("César", "cesar@email.com", req);

        assertThat(resp.id()).isEqualTo(42L);
        assertThat(resp.customerName()).isEqualTo("César");
        assertThat(resp.customerEmail()).isEqualTo("cesar@email.com");
        assertThat(resp.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(resp.totalAmount()).isEqualByComparingTo("4700.00");

        ArgumentCaptor<OrderEventDTO> captor = ArgumentCaptor.forClass(OrderEventDTO.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("ORDER_CREATED");
        assertThat(captor.getValue().customerEmail()).isEqualTo("cesar@email.com");
    }

    @Test
    void create_priceComesFromCatalog() {
        service = svc();
        when(productRepository.findAllByIdInAndActiveTrue(any()))
                .thenReturn(List.of(product(2L, "Mouse Logitech MX Master 3S", "549.90")));
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = service.create("César", "cesar@email.com",
                new CreateOrderRequest(List.of(new CreateOrderItemRequest(2L, 2))));

        assertThat(resp.totalAmount()).isEqualByComparingTo("1099.80");
        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().get(0).unitPrice()).isEqualByComparingTo("549.90");
        assertThat(resp.items().get(0).subtotal()).isEqualByComparingTo("1099.80");
    }

    @Test
    void create_itemStoresSnapshotOfProduct() {
        service = svc();
        when(productRepository.findAllByIdInAndActiveTrue(any()))
                .thenReturn(List.of(product(7L, "SSD Kingston NV2 1TB", "379.90")));
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create("César", "cesar@email.com",
                new CreateOrderRequest(List.of(new CreateOrderItemRequest(7L, 3))));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(repository).save(captor.capture());
        OrderItem item = captor.getValue().getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(7L);
        assertThat(item.getProductName()).isEqualTo("SSD Kingston NV2 1TB");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("379.90");
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void create_unknownOrInactiveProduct_throwsAndDoesNotSaveNorPublish() {
        service = svc();
        // o repositório só devolve ativos: o id 99 (inexistente ou inativo) não vem no resultado
        when(productRepository.findAllByIdInAndActiveTrue(any()))
                .thenReturn(List.of(product(1L, "Notebook", "4500.00")));
        var req = new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(1L, 1),
                new CreateOrderItemRequest(99L, 1)));

        assertThatThrownBy(() -> service.create("César", "cesar@email.com", req))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Produto não encontrado ou indisponível: 99");

        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void cancel_byOwner_pending_succeeds_andPublishesEvent() {
        service = svc();
        Order order = new Order("César", "cesar@email.com");
        order.setId(10L);
        order.setTotalAmount(new BigDecimal("100.00"));
        when(repository.findById(10L)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = service.cancel(10L, "César", "cesar@email.com", false, null);

        assertThat(resp.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(resp.cancellationMethod()).isEqualTo("USER_REQUEST");
        assertThat(resp.cancellationReason()).isEqualTo("Cancelado a pedido do cliente");
        assertThat(resp.cancelledByName()).isEqualTo("César");
        assertThat(resp.cancelledAt()).isNotNull();

        ArgumentCaptor<OrderEventDTO> captor = ArgumentCaptor.forClass(OrderEventDTO.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("ORDER_CANCELLED");
        assertThat(captor.getValue().customerEmail()).isEqualTo("cesar@email.com");
    }

    @Test
    void cancel_byAdmin_withCustomReason_storesIt() {
        service = svc();
        Order order = new Order("Maria", "maria@email.com");
        order.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = service.cancel(10L, "Admin", "admin@e.com", true, "Estoque indisponível");

        assertThat(resp.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(resp.cancellationMethod()).isEqualTo("ADMIN_MANUAL");
        assertThat(resp.cancellationReason()).isEqualTo("Estoque indisponível");
        assertThat(resp.cancelledByName()).isEqualTo("Admin");
    }

    @Test
    void cancel_byAdmin_blankReason_usesDefaultMessage() {
        service = svc();
        Order order = new Order("Maria", "maria@email.com");
        order.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = service.cancel(10L, "Admin", "admin@e.com", true, "   ");

        assertThat(resp.cancellationReason()).isEqualTo("Cancelado pela administração");
    }

    @Test
    void cancel_byNonOwner_nonAdmin_throws403() {
        service = svc();
        Order order = new Order("Alice", "alice@email.com");
        order.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(10L, "Bob", "bob@email.com", false, null))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(publisher, never()).publish(any());
    }

    @Test
    void cancel_sameNameDifferentEmail_nonAdmin_throws403_andDoesNotSave() {
        service = svc();
        Order order = new Order("Maria Silva", "maria1@email.com");
        order.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(10L, "Maria Silva", "maria2@email.com", false, null))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void cancel_ownerIdentifiedByEmail_succeeds_evenIfNameChanged() {
        service = svc();
        Order order = new Order("César", "cesar@email.com");
        order.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = service.cancel(10L, "César Souza", "cesar@email.com", false, null);

        assertThat(resp.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(publisher).publish(any(OrderEventDTO.class));
    }

    @Test
    void cancel_nonAdmin_nullUserEmail_throws403_andDoesNotSave() {
        service = svc();
        Order order = new Order("César", "cesar@email.com");
        order.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(10L, "César", null, false, null))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void cancel_nonAdmin_nullUserEmail_orderWithoutEmail_throws403() {
        service = svc();
        Order order = new Order("César", null);
        order.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(10L, "César", null, false, null))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void cancel_alreadyCancelled_throws409() {
        service = svc();
        Order order = new Order("César", "cesar@email.com");
        order.setId(10L);
        order.setStatus(OrderStatus.CANCELLED);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(10L, "César", "cesar@email.com", false, null))
                .isInstanceOf(OrderNotCancellableException.class);
    }

    @Test
    void cancel_notFound_throws404() {
        service = svc();
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(99L, "x", "x@e.com", false, null))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void findById_owner_succeeds() {
        service = svc();
        Order order = new Order("César", "cesar@email.com");
        order.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse resp = service.findById(1L, "cesar@email.com", false);

        assertThat(resp.customerName()).isEqualTo("César");
    }

    @Test
    void findById_admin_seesOthers() {
        service = svc();
        Order order = new Order("Maria", "maria@email.com");
        order.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse resp = service.findById(1L, "admin@e.com", true);

        assertThat(resp.customerEmail()).isEqualTo("maria@email.com");
    }

    @Test
    void findById_nonOwner_nonAdmin_throws403() {
        service = svc();
        Order order = new Order("Alice", "alice@email.com");
        order.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.findById(1L, "bob@email.com", false))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void findById_nonAdmin_nullUserEmail_throws403_notNPE() {
        service = svc();
        Order order = new Order("César", "cesar@email.com");
        order.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.findById(1L, null, false))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void findById_notFound_throws() {
        service = svc();
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(1L, "x@e.com", false))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void confirm_admin_succeeds_storesMetadata_andPublishes() {
        service = svc();
        Order order = new Order("Maria", "maria@email.com");
        order.setId(20L);
        order.setTotalAmount(new BigDecimal("100.00"));
        when(repository.findById(20L)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = service.confirm(20L, "ADMIN", "Admin", "admin@orderflow.com");

        assertThat(resp.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(resp.confirmedByName()).isEqualTo("Admin");
        assertThat(resp.confirmedByEmail()).isEqualTo("admin@orderflow.com");
        assertThat(resp.confirmationMethod()).isEqualTo("ADMIN_MANUAL");
        assertThat(resp.confirmedAt()).isNotNull();

        ArgumentCaptor<OrderEventDTO> captor = ArgumentCaptor.forClass(OrderEventDTO.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("ORDER_CONFIRMED");
        assertThat(captor.getValue().customerEmail()).isEqualTo("maria@email.com");
    }

    @Test
    void confirm_nonAdmin_throws403_andDoesNotTouchRepo() {
        service = svc();
        assertThatThrownBy(() -> service.confirm(20L, "USER", "X", "x@e.com"))
                .isInstanceOf(UnauthorizedAccessException.class);
        verify(repository, never()).findById(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void confirm_alreadyConfirmed_throws409() {
        service = svc();
        Order order = new Order("Maria", "maria@email.com");
        order.setId(20L);
        order.setStatus(OrderStatus.CONFIRMED);
        when(repository.findById(20L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.confirm(20L, "ADMIN", "A", "a@e.com"))
                .isInstanceOf(OrderNotConfirmableException.class);
    }

    @Test
    void confirm_cancelled_throws409() {
        service = svc();
        Order order = new Order("Maria", "maria@email.com");
        order.setId(20L);
        order.setStatus(OrderStatus.CANCELLED);
        when(repository.findById(20L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.confirm(20L, "ADMIN", "A", "a@e.com"))
                .isInstanceOf(OrderNotConfirmableException.class);
    }

    @Test
    void confirm_notFound_throws404() {
        service = svc();
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirm(99L, "ADMIN", "A", "a@e.com"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void summaryByUser_admin_callsRepository() {
        service = svc();
        when(repository.summaryByUser()).thenReturn(List.of());
        service.summaryByUser(true);
        verify(repository).summaryByUser();
    }

    @Test
    void summaryByUser_nonAdmin_throws403() {
        service = svc();
        assertThatThrownBy(() -> service.summaryByUser(false))
                .isInstanceOf(UnauthorizedAccessException.class);
        verify(repository, never()).summaryByUser();
    }
}
