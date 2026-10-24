package com.orderflow.notification.service;

import com.orderflow.notification.dto.OrderEventDTO;

public interface EmailService {
    void send(OrderEventDTO event);
}
