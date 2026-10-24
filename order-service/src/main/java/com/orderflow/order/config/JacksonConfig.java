package com.orderflow.order.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Serializa todos os LocalDateTime como ISO-8601 UTC com sufixo 'Z'.
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter UTC_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Bean
    public SimpleModule localDateTimeAsUtcModule() {
        SimpleModule module = new SimpleModule("LocalDateTimeAsUtc");
        module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.atOffset(ZoneOffset.UTC).format(UTC_ISO));
            }
        });
        return module;
    }
}
