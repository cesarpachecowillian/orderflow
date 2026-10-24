package com.orderflow.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderEventDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order-events"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EmbeddedKafkaBroker broker;

    static Consumer<String, OrderEventDTO> consumer;

    @BeforeAll
    static void setupConsumer(@Autowired EmbeddedKafkaBroker broker) {
        Map<String, Object> props = KafkaTestUtils.consumerProps("itest-group", "true", broker);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderEventDTO.class.getName());
        ConsumerFactory<String, OrderEventDTO> cf = new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(props);
        consumer = cf.createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, "order-events");
    }

    @AfterAll
    static void closeConsumer() {
        if (consumer != null) consumer.close();
    }

    private String createOrderJson() throws Exception {
        return json.writeValueAsString(new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("Monitor", 1, new BigDecimal("1200.00"))
        )));
    }

    @Test
    @Order(1)
    void create_returns201_andPublishesEventWithCustomerEmail() throws Exception {
        mvc.perform(post("/orders")
                        .header("X-User-Name", "César")
                        .header("X-User-Email", "cesar@email.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.customerName").value("César"))
                .andExpect(jsonPath("$.customerEmail").value("cesar@email.com"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1200.00));

        ConsumerRecord<String, OrderEventDTO> rec = KafkaTestUtils.getSingleRecord(consumer, "order-events", Duration.ofSeconds(10));
        assertThat(rec.value().type()).isEqualTo("ORDER_CREATED");
        assertThat(rec.value().customerEmail()).isEqualTo("cesar@email.com");
        assertThat(rec.value().totalAmount()).isEqualByComparingTo("1200.00");
    }

    @Test
    @Order(2)
    void create_missingHeader_returns400() throws Exception {
        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    void create_emptyItems_returns400() throws Exception {
        mvc.perform(post("/orders")
                        .header("X-User-Name", "César")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void list_asAdmin_returnsAll() throws Exception {
        mvc.perform(get("/orders")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Email", "admin@orderflow.com")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(4)
    void list_asUser_returnsOwnOrdersOnly() throws Exception {
        mvc.perform(get("/orders")
                        .header("X-User-Role", "USER")
                        .header("X-User-Email", "cesar@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].customerEmail").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("cesar@email.com"))));
    }

    @Test
    @Order(4)
    void summary_byUser_returnsAggregates() throws Exception {
        mvc.perform(get("/orders/summary")
                        .header("X-User-Role", "USER")
                        .header("X-User-Email", "cesar@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").isNumber())
                .andExpect(jsonPath("$.totalAmount").isNumber())
                .andExpect(jsonPath("$.pendingCount").isNumber())
                .andExpect(jsonPath("$.pendingAmount").isNumber())
                .andExpect(jsonPath("$.confirmedCount").isNumber())
                .andExpect(jsonPath("$.confirmedAmount").isNumber())
                .andExpect(jsonPath("$.cancelledCount").isNumber());
    }

    @Test
    @Order(4)
    void summaryByUser_admin_returnsList() throws Exception {
        mvc.perform(get("/orders/summary/by-user")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(4)
    void summaryByUser_nonAdmin_returns403() throws Exception {
        mvc.perform(get("/orders/summary/by-user")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    void confirm_byAdmin_returnsConfirmedWithMetadata() throws Exception {
        String body = mvc.perform(post("/orders")
                        .header("X-User-Name", "Test")
                        .header("X-User-Email", "test@email.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = ((Number) json.readValue(body, java.util.Map.class).get("id")).longValue();
        KafkaTestUtils.getSingleRecord(consumer, "order-events", Duration.ofSeconds(10));

        mvc.perform(patch("/orders/" + id + "/confirm")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Name", "AdminUser")
                        .header("X-User-Email", "admin@orderflow.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty())
                .andExpect(jsonPath("$.confirmedByName").value("AdminUser"))
                .andExpect(jsonPath("$.confirmedByEmail").value("admin@orderflow.com"))
                .andExpect(jsonPath("$.confirmationMethod").value("ADMIN_MANUAL"));

        ConsumerRecord<String, OrderEventDTO> rec = KafkaTestUtils.getSingleRecord(consumer, "order-events", Duration.ofSeconds(10));
        assertThat(rec.value().type()).isEqualTo("ORDER_CONFIRMED");
        assertThat(rec.value().customerEmail()).isEqualTo("test@email.com");
    }

    @Test
    @Order(4)
    void confirm_byUser_returns403() throws Exception {
        mvc.perform(patch("/orders/1/confirm")
                        .header("X-User-Role", "USER")
                        .header("X-User-Name", "U")
                        .header("X-User-Email", "u@e.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void cancel_pending_returns200_andPublishesEvent() throws Exception {
        String body = mvc.perform(post("/orders")
                        .header("X-User-Name", "Maria")
                        .header("X-User-Email", "maria@email.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = ((Number) json.readValue(body, java.util.Map.class).get("id")).longValue();

        // drain the create event
        KafkaTestUtils.getSingleRecord(consumer, "order-events", Duration.ofSeconds(10));

        mvc.perform(patch("/orders/" + id + "/cancel")
                        .header("X-User-Name", "Maria")
                        .header("X-User-Email", "maria@email.com")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        ConsumerRecord<String, OrderEventDTO> rec = KafkaTestUtils.getSingleRecord(consumer, "order-events", Duration.ofSeconds(10));
        assertThat(rec.value().type()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    @Order(6)
    void cancel_byNonOwner_returns403() throws Exception {
        String body = mvc.perform(post("/orders")
                        .header("X-User-Name", "Alice")
                        .header("X-User-Email", "alice@email.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = ((Number) json.readValue(body, java.util.Map.class).get("id")).longValue();

        mvc.perform(patch("/orders/" + id + "/cancel")
                        .header("X-User-Name", "Bob")
                        .header("X-User-Email", "bob@email.com")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    void cancel_notFound_returns404() throws Exception {
        mvc.perform(patch("/orders/99999/cancel")
                        .header("X-User-Name", "x")
                        .header("X-User-Email", "x@e.com")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    void findById_admin_notFound_returns404() throws Exception {
        mvc.perform(get("/orders/99999")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Email", "admin@e.com"))
                .andExpect(status().isNotFound());
    }
}
