package com.orderflow.order.controller;

import com.orderflow.order.dto.ProductResponse;
import com.orderflow.order.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
@Tag(name = "Products", description = "Catálogo de produtos")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os produtos ativos do catálogo, ordenados por nome")
    public ResponseEntity<List<ProductResponse>> list() {
        return ResponseEntity.ok(service.listActive());
    }
}
