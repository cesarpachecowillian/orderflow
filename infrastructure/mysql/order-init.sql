SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_name VARCHAR(100) NOT NULL,
  customer_email VARCHAR(150),
  status ENUM('PENDING', 'CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
  total_amount DECIMAL(10,2) NOT NULL,
  confirmed_at TIMESTAMP NULL,
  confirmed_by_name VARCHAR(100) NULL,
  confirmed_by_email VARCHAR(150) NULL,
  confirmation_method VARCHAR(30) NULL,
  cancelled_at TIMESTAMP NULL,
  cancelled_by_name VARCHAR(100) NULL,
  cancelled_by_email VARCHAR(150) NULL,
  cancellation_reason VARCHAR(500) NULL,
  cancellation_method VARCHAR(30) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_customer_name (customer_name),
  INDEX idx_customer_email (customer_email),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL UNIQUE,
  price DECIMAL(10,2) NOT NULL CHECK (price > 0),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(200) NOT NULL,
  quantity INT NOT NULL CHECK (quantity > 0),
  unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price > 0),
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id),
  INDEX idx_order_id (order_id),
  INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO products (name, price) VALUES
  ('Notebook Dell Inspiron 15', 3899.90),
  ('Mouse Logitech MX Master 3S', 549.90),
  ('Teclado Mecânico Keychron K2', 699.00),
  ('Monitor LG UltraWide 29"', 1299.00),
  ('Headset HyperX Cloud II', 449.90),
  ('Webcam Logitech C920', 399.90),
  ('SSD Kingston NV2 1TB', 379.90),
  ('Cadeira Gamer ThunderX3', 1199.00),
  ('Smartphone Samsung Galaxy A55', 2199.00),
  ('Carregador Anker 65W USB-C', 249.90),
  ('Hub USB-C Ugreen 7 em 1', 189.90),
  ('Mousepad XL Redragon', 79.90);
