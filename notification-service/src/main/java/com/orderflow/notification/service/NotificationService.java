package com.orderflow.notification.service;

import com.orderflow.notification.document.NotificationLog;
import com.orderflow.notification.document.NotificationStatus;
import com.orderflow.notification.document.NotificationType;
import com.orderflow.notification.dto.OrderEventDTO;
import com.orderflow.notification.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository repository;
    private final EmailService emailService;

    public NotificationService(NotificationLogRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    public void process(OrderEventDTO event) {
        if (event == null || event.orderId() == null || event.type() == null) {
            log.warn("Evento inválido recebido: {}", event);
            return;
        }

        NotificationType type;
        try {
            type = NotificationType.valueOf(event.type());
        } catch (IllegalArgumentException ex) {
            log.warn("Tipo de evento desconhecido: {}", event.type());
            return;
        }

        if (repository.existsByOrderIdAndTypeAndStatus(event.orderId(), type, NotificationStatus.SENT)) {
            log.info("Notificação já enviada para orderId={} type={} — pulando (idempotência)",
                    event.orderId(), type);
            return;
        }

        try {
            emailService.send(event);
            repository.save(NotificationLog.forSent(
                    event.orderId(), event.customerName(), event.customerEmail(), type));
        } catch (Exception ex) {
            log.error("Falha ao enviar notificação para orderId={}: {}", event.orderId(), ex.getMessage());
            repository.save(NotificationLog.forFailed(
                    event.orderId(), event.customerName(), event.customerEmail(), type, ex.getMessage()));
        }
    }
}
