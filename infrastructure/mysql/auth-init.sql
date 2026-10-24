CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
  super_admin BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed do usuário admin original (super-admin, idempotente). Senha: admin123 (BCrypt $2b$10).
INSERT IGNORE INTO users (name, email, password, role, super_admin) VALUES (
  'Administrador',
  'admin@orderflow.com',
  '$2b$10$e2TUJAgxwZW36i0UvwqoauACAT1om9ffrbuVW5VkXub3n0HlQs0wq',
  'ADMIN',
  TRUE
);
