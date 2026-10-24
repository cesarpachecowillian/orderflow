package com.orderflow.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("OrderFlow — Order Service API")
                .description("CRUD de pedidos com paginação, cancelamento e eventos Kafka")
                .version("1.0.0"));
    }
}
