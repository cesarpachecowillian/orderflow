package com.orderflow.auth.config;

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
 * O JVM nos containers roda em UTC, então o LocalDateTime já É um valor UTC —
 * só faltava o marcador. Com isso, o frontend (e qualquer outro consumidor)
 * pode usar new Date(iso) e ter o fuso convertido corretamente.
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
