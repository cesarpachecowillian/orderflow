package com.orderflow.order.exception;

import com.orderflow.order.entity.OrderStatus;

public class OrderNotConfirmableException extends RuntimeException {
    public OrderNotConfirmableException(OrderStatus status) {
        super("Pedido com status " + status + " não pode ser confirmado");
    }
}
