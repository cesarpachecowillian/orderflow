package com.orderflow.notification.service;

import com.orderflow.notification.dto.OrderEventDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(OrderEventDTO event) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(event.customerEmail());
        msg.setSubject("Pedido #" + event.orderId() + " - " + event.type());
        msg.setText(buildBody(event));
        mailSender.send(msg);
    }

    private String buildBody(OrderEventDTO e) {
        return "Olá %s,%n%nPedido #%d - %s%nTotal: R$ %s%nItens: %d".formatted(
                e.customerName(), e.orderId(), e.type(), e.totalAmount(), e.itemCount());
    }
}
