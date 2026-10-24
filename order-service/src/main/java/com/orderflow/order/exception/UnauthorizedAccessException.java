package com.orderflow.order.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException() {
        super("Acesso negado: o pedido não pertence ao usuário autenticado");
    }
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
