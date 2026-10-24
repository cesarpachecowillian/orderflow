package com.orderflow.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderflow.notification.dto.OrderEventDTO;
import com.orderflow.notification.repository.NotificationLogRepository;
import com.orderflow.notification.service.NotificationService;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order-events"})
class KafkaConsumerIntegrationTest {

    @MockBean NotificationService notificationService;
    @MockBean NotificationLogRepository repository;

    @Autowired EmbeddedKafkaBroker broker;

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @Test
    void consumesEventAndDelegatesToService() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put("bootstrap.servers", broker.getBrokersAsString());

        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        JsonSerializer<OrderEventDTO> valueSer = new JsonSerializer<>(om);
        valueSer.setAddTypeInfo(false);

        var pf = new DefaultKafkaProducerFactory<String, OrderEventDTO>(
                producerProps, new StringSerializer(), valueSer);
        KafkaTemplate<String, OrderEventDTO> template = new KafkaTemplate<>(pf);

        OrderEventDTO event = new OrderEventDTO("ORDER_CREATED", 99L, "Bob", "bob@email.com",
                new BigDecimal("50.00"), 1, LocalDateTime.now());

        template.send("order-events", "99", event);
        template.flush();

        verify(notificationService, timeout(10_000)).process(any(OrderEventDTO.class));
    }
}
