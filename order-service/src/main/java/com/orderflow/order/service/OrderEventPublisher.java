package com.orderflow.order.service;

import com.orderflow.order.dto.OrderEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, OrderEventDTO> kafkaTemplate;
    private final String topic;

    public OrderEventPublisher(KafkaTemplate<String, OrderEventDTO> kafkaTemplate,
                               @Value("${orderflow.kafka.topic:order-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(OrderEventDTO event) {
        kafkaTemplate.send(topic, String.valueOf(event.orderId()), event);
        log.info("Evento publicado em '{}': {}", topic, event);
    }
}
