db = db.getSiblingDB('notifications_db');
db.createCollection('notification_logs', {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["orderId", "customerName", "type", "status", "createdAt"],
      properties: {
        orderId: { bsonType: "long" },
        customerName: { bsonType: "string" },
        customerEmail: { bsonType: "string" },
        type: { enum: ["ORDER_CREATED", "ORDER_CONFIRMED", "ORDER_CANCELLED"] },
        status: { enum: ["SENT", "FAILED", "PENDING"] },
        errorMessage: { bsonType: "string" },
        sentAt: { bsonType: "date" },
        createdAt: { bsonType: "date" }
      }
    }
  }
});
db.notification_logs.createIndex({ orderId: 1 });
db.notification_logs.createIndex({ status: 1 });
db.notification_logs.createIndex({ createdAt: -1 });
print("notifications_db inicializado com sucesso");
