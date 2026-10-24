package com.orderflow.notification.consumer;

import com.orderflow.notification.dto.OrderEventDTO;
import com.orderflow.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final NotificationService notificationService;

    public OrderEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${orderflow.kafka.topic:order-events}", groupId = "${orderflow.kafka.group-id:notification-group}")
    public void onEvent(OrderEventDTO event) {
        log.info("Evento recebido: {}", event);
        notificationService.process(event);
    }
}
