package com.orderflow.gateway.exception;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class GatewayExceptionHandler {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public JsonErrorWebExceptionHandler errorHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer) {
        JsonErrorWebExceptionHandler handler = new JsonErrorWebExceptionHandler(
                errorAttributes,
                webProperties.getResources(),
                applicationContext);
        handler.setMessageWriters(serverCodecConfigurer.getWriters());
        handler.setMessageReaders(serverCodecConfigurer.getReaders());
        return handler;
    }

    public static class JsonErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

        public JsonErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                            WebProperties.Resources resources,
                                            ApplicationContext applicationContext) {
            super(errorAttributes, resources, applicationContext);
        }

        @Override
        protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
            return RouterFunctions.route(RequestPredicates.all(), this::renderJson);
        }

        private Mono<ServerResponse> renderJson(ServerRequest request) {
            Map<String, Object> attrs = getErrorAttributes(request, ErrorAttributeOptions.defaults());
            int status = (int) attrs.getOrDefault("status", 500);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", status);
            body.put("error", attrs.getOrDefault("error", "Internal Server Error"));
            body.put("message", attrs.getOrDefault("message", "Erro inesperado"));
            body.put("path", attrs.getOrDefault("path", request.path()));
            body.put("timestamp", LocalDateTime.now().toString());
            return ServerResponse.status(HttpStatus.valueOf(status))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body));
        }
    }
}
