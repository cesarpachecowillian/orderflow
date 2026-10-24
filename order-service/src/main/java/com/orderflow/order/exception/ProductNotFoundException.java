package com.orderflow.order.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Produto não encontrado ou indisponível: " + id);
    }
}
