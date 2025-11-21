-- Database schema blueprint for ai-bill backend.
-- Generated from entity classes under src/main/java/org/maram/bill/entity.
-- Assumptions:
-- * MySQL 8.0+ with InnoDB, utf8mb4.
-- * Boolean flags stored as TINYINT(1) with 0/1 semantics.
-- * Lengths chosen from validation hints or set to sensible defaults when unspecified.

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `openid` VARCHAR(64) NOT NULL COMMENT 'WeChat OpenID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT 'Username/login',
  `email` VARCHAR(100) DEFAULT NULL COMMENT 'Email address',
  `phone_number` VARCHAR(20) DEFAULT NULL COMMENT 'Mobile phone number',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT 'Nickname',
  `role` VARCHAR(30) DEFAULT NULL COMMENT 'User role',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT 'Avatar URL',
  `status` VARCHAR(30) DEFAULT NULL COMMENT 'Account status',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `last_login_at` DATETIME DEFAULT NULL COMMENT 'Last login time',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  `ai_model` VARCHAR(100) DEFAULT NULL COMMENT 'Preferred AI model',
  `ai_temperature` DOUBLE DEFAULT NULL COMMENT 'Preferred AI temperature',
  `ai_config_updated_at` DATETIME DEFAULT NULL COMMENT 'AI config last updated',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_openid` (`openid`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User accounts';

CREATE TABLE IF NOT EXISTS `ai_model_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `model_name` VARCHAR(100) NOT NULL COMMENT 'Model name identifier',
  `model_display_name` VARCHAR(100) NOT NULL COMMENT 'Model display name',
  `model_description` TEXT DEFAULT NULL COMMENT 'Model description',
  `max_tokens` INT DEFAULT NULL COMMENT 'Maximum tokens',
  `default_temperature` DOUBLE DEFAULT NULL COMMENT 'Default temperature',
  `min_temperature` DOUBLE DEFAULT NULL COMMENT 'Minimum temperature',
  `max_temperature` DOUBLE DEFAULT NULL COMMENT 'Maximum temperature',
  `cost_per_1k_input_tokens` DECIMAL(12,6) DEFAULT NULL COMMENT 'Cost per 1k input tokens',
  `cost_per_1k_output_tokens` DECIMAL(12,6) DEFAULT NULL COMMENT 'Cost per 1k output tokens',
  `supports_vision` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Vision support flag',
  `supports_function_calling` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Function calling support flag',
  `context_window` INT DEFAULT NULL COMMENT 'Context window size',
  `status` VARCHAR(32) DEFAULT NULL COMMENT 'Availability status',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Default model flag',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_config_model_name` (`model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI model configuration';

CREATE TABLE IF NOT EXISTS `invoice_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `file_name` VARCHAR(255) NOT NULL COMMENT 'Original filename',
  `file_url` VARCHAR(255) NOT NULL COMMENT 'Storage URL',
  `file_type` VARCHAR(50) DEFAULT NULL COMMENT 'MIME type',
  `file_size` BIGINT DEFAULT NULL COMMENT 'File size in bytes',
  `user_id` BIGINT NOT NULL COMMENT 'Uploader user id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  KEY `idx_invoice_file_user` (`user_id`),
  CONSTRAINT `fk_invoice_file_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Uploaded invoice files';

CREATE TABLE IF NOT EXISTS `bill_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` BIGINT DEFAULT NULL COMMENT 'Owner user id, NULL for system',
  `category_name` VARCHAR(50) NOT NULL COMMENT 'Category name',
  `category_code` VARCHAR(50) DEFAULT NULL COMMENT 'Category code',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0=disabled,1=enabled',
  `description` VARCHAR(255) DEFAULT NULL COMMENT 'Category description',
  `is_system` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'System category flag',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  KEY `idx_bill_category_user` (`user_id`),
  UNIQUE KEY `uk_bill_category_user_name` (`user_id`, `category_name`),
  CONSTRAINT `fk_bill_category_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bill categories';

CREATE TABLE IF NOT EXISTS `bill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'Owner user id',
  `file_id` BIGINT DEFAULT NULL COMMENT 'Linked invoice file id',
  `transaction_type` VARCHAR(32) NOT NULL COMMENT 'Transaction type',
  `category_id` BIGINT DEFAULT NULL COMMENT 'Category id',
  `name` VARCHAR(255) DEFAULT NULL COMMENT 'Bill title/name',
  `invoice_number` VARCHAR(100) DEFAULT NULL COMMENT 'Invoice number',
  `supplier_name` VARCHAR(120) DEFAULT NULL COMMENT 'Supplier name',
  `bill_type` VARCHAR(50) DEFAULT NULL COMMENT 'Bill type',
  `total_amount` DECIMAL(19,2) NOT NULL COMMENT 'Gross amount',
  `tax_amount` DECIMAL(19,2) DEFAULT NULL COMMENT 'Tax amount',
  `net_amount` DECIMAL(19,2) DEFAULT NULL COMMENT 'Net amount',
  `currency_code` VARCHAR(10) DEFAULT NULL COMMENT 'Currency code',
  `issue_date` DATE DEFAULT NULL COMMENT 'Issue date',
  `notes` TEXT DEFAULT NULL COMMENT 'Notes',
  `payment_status` VARCHAR(32) DEFAULT NULL COMMENT 'Payment status',
  `review_status` VARCHAR(32) DEFAULT NULL COMMENT 'Review status',
  `accounting_status` VARCHAR(32) DEFAULT NULL COMMENT 'Accounting status',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  KEY `idx_bill_user` (`user_id`),
  KEY `idx_bill_category` (`category_id`),
  KEY `idx_bill_issue_date` (`issue_date`),
  KEY `idx_bill_transaction_type` (`transaction_type`),
  CONSTRAINT `fk_bill_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bill_category` FOREIGN KEY (`category_id`) REFERENCES `bill_category` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_bill_file` FOREIGN KEY (`file_id`) REFERENCES `invoice_file` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bills';

CREATE TABLE IF NOT EXISTS `currencies` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `code` VARCHAR(10) NOT NULL COMMENT 'Currency code',
  `name` VARCHAR(100) NOT NULL COMMENT 'Currency name',
  `symbol` VARCHAR(10) DEFAULT NULL COMMENT 'Currency symbol',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Active flag',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_currencies_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Supported currencies';

CREATE TABLE IF NOT EXISTS `exchange_rates` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `base_currency_code` VARCHAR(10) NOT NULL COMMENT 'Base currency code',
  `target_currency_code` VARCHAR(10) NOT NULL COMMENT 'Target currency code',
  `rate` DECIMAL(19,6) NOT NULL COMMENT 'Exchange rate',
  `api_source` VARCHAR(64) DEFAULT NULL COMMENT 'Source API',
  `last_updated_from_api` DATETIME DEFAULT NULL COMMENT 'Last API sync time',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exchange_rates_pair` (`base_currency_code`, `target_currency_code`),
  CONSTRAINT `fk_exchange_rates_base` FOREIGN KEY (`base_currency_code`) REFERENCES `currencies` (`code`) ON UPDATE CASCADE,
  CONSTRAINT `fk_exchange_rates_target` FOREIGN KEY (`target_currency_code`) REFERENCES `currencies` (`code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Currency exchange rates';

CREATE TABLE IF NOT EXISTS `user_budget` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'Owner user id',
  `budget_amount` DECIMAL(19,2) NOT NULL COMMENT 'Budget amount',
  `budget_type` VARCHAR(20) NOT NULL COMMENT 'Budget type (MONTHLY/QUARTERLY/YEARLY)',
  `start_date` DATE NOT NULL COMMENT 'Budget start date',
  `end_date` DATE NOT NULL COMMENT 'Budget end date',
  `alert_threshold` DECIMAL(5,2) DEFAULT NULL COMMENT 'Alert threshold percentage',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  KEY `idx_user_budget_user` (`user_id`),
  KEY `idx_user_budget_period` (`start_date`, `end_date`),
  CONSTRAINT `fk_user_budget_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User budgets';
