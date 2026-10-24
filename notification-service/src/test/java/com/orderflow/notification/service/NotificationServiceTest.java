package com.orderflow.notification.service;

import com.orderflow.notification.document.NotificationLog;
import com.orderflow.notification.document.NotificationStatus;
import com.orderflow.notification.document.NotificationType;
import com.orderflow.notification.dto.OrderEventDTO;
import com.orderflow.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationLogRepository repository;
    @Mock EmailService emailService;
    @InjectMocks NotificationService service;

    private OrderEventDTO event(String type) {
        return new OrderEventDTO(type, 42L, "César", "cesar@email.com",
                new BigDecimal("100.00"), 2, LocalDateTime.now());
    }

    @Test
    void process_happyPath_sendsAndPersistsSent() {
        OrderEventDTO e = event("ORDER_CREATED");
        when(repository.existsByOrderIdAndTypeAndStatus(42L, NotificationType.ORDER_CREATED, NotificationStatus.SENT))
                .thenReturn(false);

        service.process(e);

        verify(emailService).send(e);
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(captor.getValue().getOrderId()).isEqualTo(42L);
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.ORDER_CREATED);
    }

    @Test
    void process_idempotent_skipsWhenAlreadySent() {
        OrderEventDTO e = event("ORDER_CREATED");
        when(repository.existsByOrderIdAndTypeAndStatus(42L, NotificationType.ORDER_CREATED, NotificationStatus.SENT))
                .thenReturn(true);

        service.process(e);

        verify(emailService, never()).send(any());
        verify(repository, never()).save(any());
    }

    @Test
    void process_emailFailure_persistsFailed() {
        OrderEventDTO e = event("ORDER_CREATED");
        when(repository.existsByOrderIdAndTypeAndStatus(any(), any(), any())).thenReturn(false);
        doThrow(new RuntimeException("SMTP timeout")).when(emailService).send(e);

        service.process(e);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).contains("SMTP timeout");
    }

    @Test
    void process_unknownType_isIgnored() {
        OrderEventDTO e = event("UNKNOWN_TYPE");
        service.process(e);
        verifyNoInteractions(emailService);
        verify(repository, never()).save(any());
    }

    @Test
    void process_nullEvent_isIgnored() {
        service.process(null);
        verifyNoInteractions(emailService, repository);
    }

    @Test
    void process_cancelledEvent_persistsCancelType() {
        OrderEventDTO e = event("ORDER_CANCELLED");
        when(repository.existsByOrderIdAndTypeAndStatus(42L, NotificationType.ORDER_CANCELLED, NotificationStatus.SENT))
                .thenReturn(false);

        service.process(e);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
    }
}
