package com.orderflow.notification.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "notification_logs")
public class NotificationLog {

    @Id
    private String id;

    @Field("orderId")
    private Long orderId;

    @Field("customerName")
    private String customerName;

    @Field("customerEmail")
    private String customerEmail;

    @Field("type")
    private NotificationType type;

    @Field("status")
    private NotificationStatus status;

    @Field("errorMessage")
    private String errorMessage;

    @Field("sentAt")
    private LocalDateTime sentAt;

    @Field("createdAt")
    private LocalDateTime createdAt;

    public NotificationLog() {}

    public static NotificationLog forSent(Long orderId, String name, String email, NotificationType type) {
        NotificationLog n = new NotificationLog();
        n.orderId = orderId;
        n.customerName = name;
        n.customerEmail = email;
        n.type = type;
        n.status = NotificationStatus.SENT;
        n.sentAt = LocalDateTime.now();
        n.createdAt = LocalDateTime.now();
        return n;
    }

    public static NotificationLog forFailed(Long orderId, String name, String email, NotificationType type, String error) {
        NotificationLog n = new NotificationLog();
        n.orderId = orderId;
        n.customerName = name;
        n.customerEmail = email;
        n.type = type;
        n.status = NotificationStatus.FAILED;
        n.errorMessage = error;
        n.createdAt = LocalDateTime.now();
        return n;
    }

    public String getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public NotificationType getType() { return type; }
    public NotificationStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
