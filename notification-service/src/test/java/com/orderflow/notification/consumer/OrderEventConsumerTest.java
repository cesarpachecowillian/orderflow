package com.orderflow.notification.consumer;

import com.orderflow.notification.dto.OrderEventDTO;
import com.orderflow.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock NotificationService service;
    @InjectMocks OrderEventConsumer consumer;

    @Test
    void delegatesEventToService() {
        var e = new OrderEventDTO("ORDER_CREATED", 1L, "X", "x@e.com",
                new BigDecimal("10.00"), 1, LocalDateTime.now());
        consumer.onEvent(e);
        verify(service).process(e);
    }
}
