CREATE DATABASE IF NOT EXISTS ai_vision_voice_assistant
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ai_vision_voice_assistant;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(190) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  nickname VARCHAR(80) NOT NULL,
  avatar_url MEDIUMTEXT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  token_version INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_token_accounts (
  user_id BIGINT NOT NULL,
  balance_tokens BIGINT NOT NULL DEFAULT 0,
  total_recharged_tokens BIGINT NOT NULL DEFAULT 0,
  total_used_tokens BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id),
  CONSTRAINT fk_user_token_accounts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS token_transactions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  amount_tokens BIGINT NOT NULL,
  balance_after_tokens BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  request_id VARCHAR(64) NULL,
  reason VARCHAR(255) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_token_transactions_user_created (user_id, created_at),
  KEY idx_token_transactions_request_id (request_id),
  CONSTRAINT fk_token_transactions_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_request_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  session_id VARCHAR(100) NOT NULL,
  model_name VARCHAR(80) NOT NULL,
  input_tokens INT NOT NULL DEFAULT 0,
  output_tokens INT NOT NULL DEFAULT 0,
  total_tokens INT NOT NULL DEFAULT 0,
  charged_tokens BIGINT NOT NULL DEFAULT 0,
  token_unit_price_yuan DECIMAL(12, 8) NOT NULL DEFAULT 0.00001000,
  revenue_amount_yuan DECIMAL(12, 6) NOT NULL DEFAULT 0.000000,
  provider_cost_amount_yuan DECIMAL(12, 6) NOT NULL DEFAULT 0.000000,
  gross_profit_amount_yuan DECIMAL(12, 6) NOT NULL DEFAULT 0.000000,
  frame_count INT NOT NULL DEFAULT 0,
  image_total_bytes BIGINT NOT NULL DEFAULT 0,
  latency_ms BIGINT NULL,
  cached BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL,
  error_message VARCHAR(1000) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_request_logs_request_id (request_id),
  KEY idx_ai_request_logs_user_created (user_id, created_at),
  KEY idx_ai_request_logs_model_created (model_name, created_at),
  CONSTRAINT fk_ai_request_logs_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS chat_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id VARCHAR(100) NOT NULL,
  title VARCHAR(200) NOT NULL,
  message_count INT NOT NULL DEFAULT 0,
  last_message_preview VARCHAR(500) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  last_message_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_chat_sessions_user_session (user_id, session_id),
  KEY idx_chat_sessions_user_last_message (user_id, last_message_at),
  KEY idx_chat_sessions_user_updated (user_id, updated_at),
  CONSTRAINT fk_chat_sessions_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  request_id VARCHAR(64) NULL,
  model_name VARCHAR(80) NULL,
  input_tokens INT NOT NULL DEFAULT 0,
  output_tokens INT NOT NULL DEFAULT 0,
  total_tokens INT NOT NULL DEFAULT 0,
  charged_tokens BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_chat_messages_user_created (user_id, created_at),
  KEY idx_chat_messages_session_created (user_id, session_id, created_at),
  CONSTRAINT fk_chat_messages_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
