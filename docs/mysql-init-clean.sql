USE `crypto`;

-- 干净初始化脚本：只初始化全局运行配置，不写入测试订单、测试链、测试币种或密钥。
-- 使用顺序：先执行 mysql-schema.sql 建表，再执行本脚本写入默认配置。

INSERT INTO `manager_role`
(`role_code`, `role_name`, `super_admin`, `enabled`, `created_at`, `updated_at`)
VALUES
('ADMIN', '超级管理员', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `super_admin` = 1,
  `enabled` = 1,
  `updated_at` = NOW();

INSERT INTO `manager_role_data_permission`
(`role_code`, `scope_type`, `scope_value`, `enabled`, `created_at`, `updated_at`)
VALUES
('ADMIN', 'ALL', '*', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `enabled` = 1,
  `updated_at` = NOW();

INSERT INTO `manager_message_template`
(`template_code`, `channel`, `template_name`, `subject`, `content`, `enabled`, `created_at`, `updated_at`)
VALUES
('APPROVAL_PENDING', 'EMAIL', '审批待办提醒', '审批待办：${title}', '你有一条审批待处理，审批单号：${approvalNo}，业务类型：${bizType}，当前步骤：${stepName}。', 1, NOW(), NOW()),
('APPROVAL_PENDING', 'PHONE', '审批待办短信提醒', NULL, '审批待处理：${title}，审批单号：${approvalNo}，当前步骤：${stepName}。', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `template_name` = VALUES(`template_name`),
  `subject` = VALUES(`subject`),
  `content` = VALUES(`content`),
  `enabled` = VALUES(`enabled`),
  `updated_at` = NOW();

INSERT INTO `manager_approval_definition`
(`definition_code`, `biz_type`, `definition_name`, `notify_next_approver`, `enabled`, `created_at`, `updated_at`)
VALUES
('MERCHANT_BALANCE_ADJUST_V1', 'MERCHANT_BALANCE_ADJUST', '商户余额调账审批', 0, 0, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `biz_type` = VALUES(`biz_type`),
  `definition_name` = VALUES(`definition_name`),
  `notify_next_approver` = VALUES(`notify_next_approver`),
  `updated_at` = NOW(3);

INSERT INTO `manager_approval_step_definition`
(`definition_code`, `step_no`, `step_name`, `required_approvals`, `approver_users`, `approver_roles`, `enabled`, `created_at`, `updated_at`)
VALUES
('MERCHANT_BALANCE_ADJUST_V1', 1, '财务初审', 1, NULL, 'ADMIN', 1, NOW(3), NOW(3)),
('MERCHANT_BALANCE_ADJUST_V1', 2, '负责人复核', 1, NULL, 'ADMIN', 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `step_name` = VALUES(`step_name`),
  `required_approvals` = VALUES(`required_approvals`),
  `approver_users` = VALUES(`approver_users`),
  `approver_roles` = VALUES(`approver_roles`),
  `enabled` = VALUES(`enabled`),
  `updated_at` = NOW(3);

INSERT INTO `payment_platform_config`
(`config_scope`, `order_expire_minutes`, `cashier_base_url`, `treasury_address`, `idempotency_wait_millis`, `enabled`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 15, 'http://localhost:9888/crypto-gateway', '', 4000, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `order_expire_minutes` = VALUES(`order_expire_minutes`),
  `cashier_base_url` = VALUES(`cashier_base_url`),
  `treasury_address` = VALUES(`treasury_address`),
  `idempotency_wait_millis` = VALUES(`idempotency_wait_millis`),
  `enabled` = VALUES(`enabled`),
  `updated_at` = NOW(3);

INSERT INTO `payment_scanner_config`
(`config_scope`, `confirmation_depth`, `scan_interval_seconds`, `checkpoint_flush_blocks`, `backfill_blocks`, `log_scan_batch_blocks`, `log_scan_retry_attempts`, `failure_cooldown_seconds`, `websocket_enabled`, `websocket_leader_lease_seconds`, `chain_replay_ttl_hours`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 12, 15, 20, 12, 50, 3, 60, 1, 45, 720, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `confirmation_depth` = VALUES(`confirmation_depth`),
  `scan_interval_seconds` = VALUES(`scan_interval_seconds`),
  `checkpoint_flush_blocks` = VALUES(`checkpoint_flush_blocks`),
  `backfill_blocks` = VALUES(`backfill_blocks`),
  `log_scan_batch_blocks` = VALUES(`log_scan_batch_blocks`),
  `log_scan_retry_attempts` = VALUES(`log_scan_retry_attempts`),
  `failure_cooldown_seconds` = VALUES(`failure_cooldown_seconds`),
  `websocket_enabled` = VALUES(`websocket_enabled`),
  `websocket_leader_lease_seconds` = VALUES(`websocket_leader_lease_seconds`),
  `chain_replay_ttl_hours` = VALUES(`chain_replay_ttl_hours`),
  `updated_at` = NOW(3);

INSERT INTO `payment_signature_config`
(`config_scope`, `signature_enabled`, `verify_inbound_enabled`, `sign_outbound_enabled`, `algorithm`, `merchant_id_header`, `timestamp_header`, `nonce_header`, `signature_header`, `key_version_header`, `allowed_clock_skew_seconds`, `replay_ttl_seconds`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 0, 1, 1, 'RSA_SHA256', 'X-Merchant-Id', 'X-Timestamp', 'X-Nonce', 'X-Signature', 'X-Key-Version', 300, 600, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `signature_enabled` = VALUES(`signature_enabled`),
  `verify_inbound_enabled` = VALUES(`verify_inbound_enabled`),
  `sign_outbound_enabled` = VALUES(`sign_outbound_enabled`),
  `algorithm` = VALUES(`algorithm`),
  `allowed_clock_skew_seconds` = VALUES(`allowed_clock_skew_seconds`),
  `replay_ttl_seconds` = VALUES(`replay_ttl_seconds`),
  `updated_at` = NOW(3);

INSERT INTO `payment_cashier_config`
(`config_scope`, `token_encryption_enabled`, `token_key_alias`, `token_ttl_minutes`, `token_prefix`, `short_token_enabled`, `short_token_prefix`, `short_token_length`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 1, 'cashier-v1', 60, 'cashier', 1, 'c', 12, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `token_encryption_enabled` = VALUES(`token_encryption_enabled`),
  `token_key_alias` = VALUES(`token_key_alias`),
  `token_ttl_minutes` = VALUES(`token_ttl_minutes`),
  `token_prefix` = VALUES(`token_prefix`),
  `short_token_enabled` = VALUES(`short_token_enabled`),
  `short_token_prefix` = VALUES(`short_token_prefix`),
  `short_token_length` = VALUES(`short_token_length`),
  `updated_at` = NOW(3);

INSERT INTO `payment_callback_config`
(`config_scope`, `retry_enabled`, `max_attempts`, `initial_backoff_seconds`, `max_backoff_seconds`, `retention_days`, `dead_letter_enabled`, `dispatch_immediately`, `dispatch_batch_size`, `dispatch_lock_wait_millis`, `dispatch_lock_lease_seconds`, `max_stored_response_chars`, `max_stored_error_chars`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 1, 6, 15, 900, 14, 1, 1, 100, 2000, 60, 8000, 4000, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `retry_enabled` = VALUES(`retry_enabled`),
  `max_attempts` = VALUES(`max_attempts`),
  `initial_backoff_seconds` = VALUES(`initial_backoff_seconds`),
  `max_backoff_seconds` = VALUES(`max_backoff_seconds`),
  `retention_days` = VALUES(`retention_days`),
  `dead_letter_enabled` = VALUES(`dead_letter_enabled`),
  `dispatch_immediately` = VALUES(`dispatch_immediately`),
  `dispatch_batch_size` = VALUES(`dispatch_batch_size`),
  `dispatch_lock_wait_millis` = VALUES(`dispatch_lock_wait_millis`),
  `dispatch_lock_lease_seconds` = VALUES(`dispatch_lock_lease_seconds`),
  `max_stored_response_chars` = VALUES(`max_stored_response_chars`),
  `max_stored_error_chars` = VALUES(`max_stored_error_chars`),
  `updated_at` = NOW(3);

INSERT INTO `payment_security_config`
(`config_scope`, `validate_redirect_url`, `allow_local_redirect`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 1, 0, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `validate_redirect_url` = VALUES(`validate_redirect_url`),
  `allow_local_redirect` = VALUES(`allow_local_redirect`),
  `updated_at` = NOW(3);

INSERT INTO `payment_kyt_config`
(`config_scope`, `kyt_enabled`, `strict_mode`, `review_threshold`, `reject_threshold`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 0, 0, 60, 80, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `kyt_enabled` = VALUES(`kyt_enabled`),
  `strict_mode` = VALUES(`strict_mode`),
  `review_threshold` = VALUES(`review_threshold`),
  `reject_threshold` = VALUES(`reject_threshold`),
  `updated_at` = NOW(3);

INSERT INTO `payment_gas_config`
(`config_scope`, `gas_enabled`, `hosted_wallet_prefer_platform`, `hosted_wallet_sponsor_enabled`, `evm_sponsor_enabled`, `evm_relayer_private_key_source_type`, `evm_relayer_private_key_env`, `evm_relayer_private_key_kms_key_id`, `sponsor_protection_enabled`, `sponsor_counter_ttl_seconds`, `sponsor_order_max_attempts`, `sponsor_wallet_max_attempts`, `sponsor_ip_max_attempts`, `sponsor_cashier_token_max_attempts`, `sponsor_order_lock_lease_seconds`, `customer_balance_precheck_enabled`, `platform_sponsored_gas_limit`, `customer_gas_limit`, `free_transfer_gas_limit`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 1, 1, 0, 0, 'ENV', 'CRYPTO_PAYMENT_EVM_RELAYER_PRIVATE_KEY', 'evm-relayer-private-key', 1, 3600, 3, 10, 30, 10, 60, 1, 250000, 120000, 21000, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `gas_enabled` = VALUES(`gas_enabled`),
  `hosted_wallet_prefer_platform` = VALUES(`hosted_wallet_prefer_platform`),
  `hosted_wallet_sponsor_enabled` = VALUES(`hosted_wallet_sponsor_enabled`),
  `evm_sponsor_enabled` = VALUES(`evm_sponsor_enabled`),
  `sponsor_protection_enabled` = VALUES(`sponsor_protection_enabled`),
  `updated_at` = NOW(3);

INSERT INTO `payment_gateway_config`
(`config_scope`, `gateway_enabled`, `service_fee_token`, `service_fee_amount`, `api_key`, `public_resources_enabled`, `facilitator_name`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 0, 'USDC', 0.00, '', 0, 'Crypto Gateway X402 Facilitator', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `gateway_enabled` = VALUES(`gateway_enabled`),
  `service_fee_token` = VALUES(`service_fee_token`),
  `service_fee_amount` = VALUES(`service_fee_amount`),
  `public_resources_enabled` = VALUES(`public_resources_enabled`),
  `facilitator_name` = VALUES(`facilitator_name`),
  `updated_at` = NOW(3);

INSERT INTO `payment_discovery_config`
(`config_scope`, `discovery_enabled`, `public_base_url`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 1, 'http://localhost:9888/crypto-gateway', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `discovery_enabled` = VALUES(`discovery_enabled`),
  `public_base_url` = VALUES(`public_base_url`),
  `updated_at` = NOW(3);

INSERT INTO `payment_subscription_config`
(`config_scope`, `subscription_enabled`, `default_mode`, `default_cycle_seconds`, `superfluid_host_address`, `superfluid_cfa_address`, `erc1337_executor_address`, `scheduler_enabled`, `scheduler_interval_seconds`, `stream_settlement_interval_seconds`, `scheduler_batch_size`, `max_retry_count`, `retry_backoff_seconds`, `execution_gas_limit`, `executor_private_key_source_type`, `executor_private_key_env`, `executor_private_key_kms_key_id`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 0, 'SUPERFLUID_STREAM', 2592000, '', '', '', 0, 30, 3600, 50, 3, 300, 300000, 'ENV', 'CRYPTO_PAYMENT_SUBSCRIPTION_EXECUTOR_PRIVATE_KEY', 'subscription-executor-private-key', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `subscription_enabled` = VALUES(`subscription_enabled`),
  `default_mode` = VALUES(`default_mode`),
  `default_cycle_seconds` = VALUES(`default_cycle_seconds`),
  `scheduler_enabled` = VALUES(`scheduler_enabled`),
  `scheduler_interval_seconds` = VALUES(`scheduler_interval_seconds`),
  `stream_settlement_interval_seconds` = VALUES(`stream_settlement_interval_seconds`),
  `updated_at` = NOW(3);

INSERT INTO `payment_derived_address_config`
(`config_scope`, `derived_enabled`, `reuse_address`, `reuse_cooldown_minutes`, `mode`, `hd_mnemonic_source_type`, `hd_mnemonic_ciphertext`, `hd_mnemonic_key_id`, `hd_mnemonic_env`, `hd_passphrase_env`, `hd_derivation_path_prefix`, `hd_start_index`, `hd_generate_mnemonic_when_missing`, `keystore_output_dir`, `keystore_password_env`, `keystore_file_prefix`, `third_party_enabled`, `third_party_base_url`, `third_party_create_path`, `third_party_api_key_env`, `third_party_provider_name`, `address_factory_namespace`, `address_factory_seed_prefix`, `created_at`, `updated_at`)
VALUES
('GLOBAL', 1, 0, 60, 'HD', 'ENV', NULL, 'derived-hd-mnemonic', 'CRYPTO_PAYMENT_DERIVED_HD_MNEMONIC', NULL, 'm/44''/60''/0''/0', 0, 0, './wallet-keys', 'CRYPTO_PAYMENT_DERIVED_KEYSTORE_PASSWORD', 'crypto-wallet', 0, '', '/wallets/derived', 'CRYPTO_PAYMENT_DERIVED_THIRD_PARTY_API_KEY', 'external-wallet-provider', 'crypto', 'derived', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  `derived_enabled` = VALUES(`derived_enabled`),
  `reuse_address` = VALUES(`reuse_address`),
  `reuse_cooldown_minutes` = VALUES(`reuse_cooldown_minutes`),
  `mode` = VALUES(`mode`),
  `hd_mnemonic_source_type` = VALUES(`hd_mnemonic_source_type`),
  `hd_mnemonic_key_id` = VALUES(`hd_mnemonic_key_id`),
  `hd_mnemonic_env` = VALUES(`hd_mnemonic_env`),
  `updated_at` = NOW(3);
