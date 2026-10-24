package com.orderflow.notification.repository;

import com.orderflow.notification.document.NotificationLog;
import com.orderflow.notification.document.NotificationStatus;
import com.orderflow.notification.document.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
    boolean existsByOrderIdAndTypeAndStatus(Long orderId, NotificationType type, NotificationStatus status);
}
