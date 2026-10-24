package com.orderflow.notification.service;

import com.orderflow.notification.dto.OrderEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"docker", "dev", "default", "test"})
public class FakeEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(FakeEmailService.class);

    @Override
    public void send(OrderEventDTO event) {
        String to = event.customerEmail() != null ? event.customerEmail() : "(sem e-mail)";
        log.info("[FAKE EMAIL] To: {} | Subject: Pedido #{} - {} | Cliente: {} | Total: R$ {} | Itens: {}",
                to, event.orderId(), event.type(), event.customerName(),
                event.totalAmount(), event.itemCount());
    }
}
