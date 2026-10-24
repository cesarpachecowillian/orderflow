package com.orderflow.order.exception;

import com.orderflow.order.entity.OrderStatus;

public class OrderNotCancellableException extends RuntimeException {
    public OrderNotCancellableException(OrderStatus status) {
        super("Pedido com status " + status + " não pode ser cancelado");
    }
}
