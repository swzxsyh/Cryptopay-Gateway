CREATE DATABASE IF NOT EXISTS `crypto`
  DEFAULT CHARACTER SET utf8mb4;

USE `crypto`;

DROP TABLE IF EXISTS `manager_role_data_permission`;
DROP TABLE IF EXISTS `manager_approval_action`;
DROP TABLE IF EXISTS `manager_approval_request`;
DROP TABLE IF EXISTS `manager_approval_step_definition`;
DROP TABLE IF EXISTS `manager_approval_definition`;
DROP TABLE IF EXISTS `manager_message_send_record`;
DROP TABLE IF EXISTS `manager_message_template`;
DROP TABLE IF EXISTS `manager_role_function_permission`;
DROP TABLE IF EXISTS `manager_user_data_permission`;
DROP TABLE IF EXISTS `manager_user_role`;
DROP TABLE IF EXISTS `manager_role`;
DROP TABLE IF EXISTS `manager_admin_user`;
DROP TABLE IF EXISTS `replay_transaction_record`;
DROP TABLE IF EXISTS `payment_audit_record`;
DROP TABLE IF EXISTS `pending_chain_transaction`;
DROP TABLE IF EXISTS `payment_reconciliation_record`;
DROP TABLE IF EXISTS `raw_chain_logs`;
DROP TABLE IF EXISTS `payment_exception_order`;
DROP TABLE IF EXISTS `payment_callback_delivery_record`;
DROP TABLE IF EXISTS `request_idempotency_record`;
DROP TABLE IF EXISTS `request_signature_replay_record`;
DROP TABLE IF EXISTS `platform_cipher_key`;
DROP TABLE IF EXISTS `platform_signing_key`;
DROP TABLE IF EXISTS `merchant_signature_key`;
DROP TABLE IF EXISTS `contract_settlement_split_record`;
DROP TABLE IF EXISTS `contract_settlement_record`;
DROP TABLE IF EXISTS `chain_scanner_checkpoint`;
DROP TABLE IF EXISTS `derived_address_pool_policy`;
DROP TABLE IF EXISTS `derived_address_pool_record`;
DROP TABLE IF EXISTS `merchant_balance_adjust_record`;
DROP TABLE IF EXISTS `merchant_withdraw_record`;
DROP TABLE IF EXISTS `merchant_balance_flow_record`;
DROP TABLE IF EXISTS `merchant_balance_account`;
DROP TABLE IF EXISTS `merchant_service_fee_account`;
DROP TABLE IF EXISTS `payment_callback_config`;
DROP TABLE IF EXISTS `payment_redirect_host`;
DROP TABLE IF EXISTS `payment_security_config`;
DROP TABLE IF EXISTS `payment_derived_address_config`;
DROP TABLE IF EXISTS `payment_contract_split_rule`;
DROP TABLE IF EXISTS `payment_contract_config`;
DROP TABLE IF EXISTS `payment_token_capability`;
DROP TABLE IF EXISTS `payment_token_config`;
DROP TABLE IF EXISTS `payment_chain_config`;
DROP TABLE IF EXISTS `payment_kyt_token_rule`;
DROP TABLE IF EXISTS `payment_kyt_chain_rule`;
DROP TABLE IF EXISTS `payment_kyt_address_rule`;
DROP TABLE IF EXISTS `payment_kyt_config`;
DROP TABLE IF EXISTS `payment_gas_low_fee_chain`;
DROP TABLE IF EXISTS `payment_gas_config`;
DROP TABLE IF EXISTS `payment_subscription_config`;
DROP TABLE IF EXISTS `payment_cashier_config`;
DROP TABLE IF EXISTS `cashier_token_mapping`;
DROP TABLE IF EXISTS `payment_signature_config`;
DROP TABLE IF EXISTS `payment_scanner_config`;
DROP TABLE IF EXISTS `payment_discovery_config`;
DROP TABLE IF EXISTS `payment_gateway_config`;
DROP TABLE IF EXISTS `payment_platform_config`;
DROP TABLE IF EXISTS `merchant_payment_channel_config`;
DROP TABLE IF EXISTS `merchant_product`;
DROP TABLE IF EXISTS `merchant_info`;
DROP TABLE IF EXISTS `subscription_order`;
DROP TABLE IF EXISTS `payment_order`;

CREATE TABLE `manager_admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) NOT NULL COMMENT '登录名',
  `password_hash` varchar(120) NOT NULL COMMENT 'BCrypt 密码摘要',
  `display_name` varchar(64) DEFAULT NULL COMMENT '显示名称',
  `contact_type` varchar(32) DEFAULT NULL COMMENT '联系方式类型：PHONE/EMAIL',
  `contact_value` varchar(128) DEFAULT NULL COMMENT '联系方式内容：手机号或邮箱',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `account_non_locked` tinyint(1) NOT NULL DEFAULT 1 COMMENT '账号是否未锁定',
  `failed_login_count` int NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `roles` varchar(255) NOT NULL DEFAULT '' COMMENT '兼容字段：角色编码，逗号分隔；普通用户默认留空，权限通过 manager_user_role 绑定',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_admin_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端管理员账号';

CREATE TABLE `manager_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码，不含 ROLE_ 前缀',
  `role_name` varchar(128) NOT NULL COMMENT '角色名称',
  `super_admin` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否超级管理员，超级管理员不受功能和数据权限限制',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端角色表';

CREATE TABLE `manager_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '管理员账号ID',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_user_role` (`user_id`, `role_code`),
  KEY `idx_manager_user_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端用户角色关系表';

CREATE TABLE `manager_role_function_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `permission_code` varchar(128) NOT NULL COMMENT '功能权限编码，如 order:view/callback:manage',
  `permission_name` varchar(128) DEFAULT NULL COMMENT '功能权限名称',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_role_function` (`role_code`, `permission_code`),
  KEY `idx_manager_function_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端角色功能权限表';

CREATE TABLE `manager_role_data_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `scope_type` varchar(64) NOT NULL COMMENT '数据范围类型：ALL/MERCHANT/CHAIN',
  `scope_value` varchar(128) NOT NULL DEFAULT '*' COMMENT '数据范围值，ALL 使用 *，MERCHANT 使用商户号',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_role_data_scope` (`role_code`, `scope_type`, `scope_value`),
  KEY `idx_manager_role_data_scope` (`scope_type`, `scope_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端角色数据权限表';

CREATE TABLE `manager_user_data_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '管理员账号ID',
  `scope_type` varchar(64) NOT NULL COMMENT '数据范围类型：ALL/MERCHANT/CHAIN',
  `scope_value` varchar(128) NOT NULL DEFAULT '*' COMMENT '数据范围值，MERCHANT 使用商户号',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_user_data_scope` (`user_id`, `scope_type`, `scope_value`),
  KEY `idx_manager_user_data_scope` (`scope_type`, `scope_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端用户数据权限表';

CREATE TABLE `manager_message_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `channel` varchar(32) NOT NULL COMMENT '消息通道：EMAIL/PHONE/IN_APP',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `subject` varchar(256) DEFAULT NULL COMMENT '消息标题',
  `content` text NOT NULL COMMENT '消息正文，变量格式为 ${name}',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_message_template` (`template_code`, `channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端消息模板表';

CREATE TABLE `manager_message_send_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_no` varchar(64) NOT NULL COMMENT '消息单号',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `channel` varchar(32) NOT NULL COMMENT '消息通道',
  `receiver` varchar(128) NOT NULL COMMENT '接收人联系方式',
  `subject` varchar(256) DEFAULT NULL COMMENT '消息标题',
  `content` text NOT NULL COMMENT '消息正文',
  `status` varchar(32) NOT NULL COMMENT '发送状态：PENDING/SENT/FAILED',
  `provider` varchar(64) DEFAULT NULL COMMENT '发送服务商',
  `provider_message_id` varchar(128) DEFAULT NULL COMMENT '服务商消息ID',
  `failure_reason` varchar(1024) DEFAULT NULL COMMENT '失败原因',
  `sent_at` datetime(3) DEFAULT NULL COMMENT '发送时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_message_no` (`message_no`),
  KEY `idx_manager_message_receiver` (`channel`, `receiver`),
  KEY `idx_manager_message_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端消息发送记录表';

CREATE TABLE `manager_approval_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `definition_code` varchar(64) NOT NULL COMMENT '审批流编码',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型，如 MERCHANT_BALANCE_ADJUST',
  `definition_name` varchar(128) NOT NULL COMMENT '审批流名称',
  `notify_next_approver` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否在提交审批单或进入下一审批步骤时通知下一责任人',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_approval_definition_code` (`definition_code`),
  KEY `idx_manager_approval_definition_biz` (`biz_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端审批流定义表';

CREATE TABLE `manager_approval_step_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `definition_code` varchar(64) NOT NULL COMMENT '审批流编码',
  `step_no` int NOT NULL COMMENT '审批步骤序号，从 1 开始',
  `step_name` varchar(128) NOT NULL COMMENT '审批步骤名称',
  `required_approvals` int NOT NULL DEFAULT 1 COMMENT '当前步骤至少需要几人通过',
  `approver_users` varchar(1024) DEFAULT NULL COMMENT '允许审批的用户名，英文逗号分隔；为空表示不限制用户',
  `approver_roles` varchar(1024) DEFAULT NULL COMMENT '允许审批的角色编码，英文逗号分隔；为空表示不限制角色',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_approval_step` (`definition_code`, `step_no`),
  KEY `idx_manager_approval_step_definition` (`definition_code`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端审批步骤定义表';

CREATE TABLE `manager_approval_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `approval_no` varchar(64) NOT NULL COMMENT '审批单号',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `biz_key` varchar(128) NOT NULL COMMENT '业务键',
  `definition_code` varchar(64) NOT NULL COMMENT '命中的审批流编码',
  `title` varchar(256) NOT NULL COMMENT '审批标题',
  `status` varchar(32) NOT NULL COMMENT '审批状态：PENDING/APPROVED/REJECTED',
  `current_step_no` int NOT NULL DEFAULT 1 COMMENT '当前审批步骤序号',
  `total_steps` int NOT NULL DEFAULT 1 COMMENT '总审批步骤数',
  `required_approvals` int NOT NULL DEFAULT 1 COMMENT '当前步骤所需通过人数',
  `approved_count` int NOT NULL DEFAULT 0 COMMENT '当前步骤已通过人数',
  `applicant` varchar(64) NOT NULL COMMENT '申请人',
  `payload_json` longtext NOT NULL COMMENT '业务申请快照 JSON',
  `remark` varchar(512) DEFAULT NULL COMMENT '申请备注，可为空',
  `reject_reason` varchar(1024) DEFAULT NULL COMMENT '拒绝原因',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_approval_no` (`approval_no`),
  KEY `idx_manager_approval_biz` (`biz_type`, `biz_key`),
  KEY `idx_manager_approval_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端审批申请表';

CREATE TABLE `manager_approval_action` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `approval_no` varchar(64) NOT NULL COMMENT '审批单号',
  `step_no` int NOT NULL COMMENT '审批步骤序号',
  `approver` varchar(64) NOT NULL COMMENT '审批人',
  `action` varchar(32) NOT NULL COMMENT '审批动作：APPROVE/REJECT',
  `comment` varchar(1024) DEFAULT NULL COMMENT '审批意见',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_approval_action_once` (`approval_no`, `step_no`, `approver`),
  KEY `idx_manager_approval_action_no` (`approval_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端审批动作记录表';

CREATE TABLE `merchant_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号，业务侧稳定标识',
  `merchant_name` varchar(128) NOT NULL COMMENT '商户名称',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '商户状态：ENABLED/DISABLED/REVIEW',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系邮箱',
  `contact_phone` varchar(64) DEFAULT NULL COMMENT '联系电话',
  `default_notify_url` varchar(512) DEFAULT NULL COMMENT '默认异步通知地址，后续可作为订单未传 notifyUrl 时的兜底',
  `default_return_url` varchar(512) DEFAULT NULL COMMENT '默认支付完成跳转地址，后续可作为订单未传 returnUrl 时的兜底',
  `default_withdraw_chain` varchar(64) DEFAULT NULL COMMENT '默认提现链，仅作为商户端提现申请的默认值',
  `default_withdraw_address` varchar(128) DEFAULT NULL COMMENT '默认提现收款地址，仅作为商户端提现申请的默认值',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_info_merchant_id` (`merchant_id`),
  KEY `idx_merchant_info_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户主数据表';

CREATE TABLE `merchant_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号，对应 merchant_info.merchant_id',
  `product_name` varchar(128) NOT NULL COMMENT '产品名',
  `product_url` varchar(512) DEFAULT NULL COMMENT '产品链接',
  `test_username` varchar(128) DEFAULT NULL COMMENT '测试账号',
  `test_password` varchar(256) DEFAULT NULL COMMENT '测试密码，仅用于内部测试记录，生产建议改为加密存储',
  `remark` varchar(1024) DEFAULT NULL COMMENT '备注',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_product_merchant` (`merchant_id`),
  KEY `idx_merchant_product_name` (`product_name`),
  KEY `idx_merchant_product_url` (`product_url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户产品库表';

CREATE TABLE `merchant_payment_channel_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `chain_code` varchar(64) NOT NULL COMMENT '链编码，对应 payment_chain_config.chain_code',
  `token_symbol` varchar(64) NOT NULL COMMENT '币种标识，对应 payment_token_config.token_symbol',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否允许该商户使用此链币收款',
  `transaction_fee_rate` decimal(18,8) NOT NULL DEFAULT 0 COMMENT '交易费率，小数表示，例如 0.006 表示 0.6%',
  `minimum_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '保底手续费',
  `fixed_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '固定手续费',
  `gateway_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '网关费',
  `tax_rate` decimal(18,8) NOT NULL DEFAULT 0 COMMENT '税率，小数表示，例如 0.05 表示 5%',
  `fee_settlement_mode` varchar(32) NOT NULL DEFAULT 'PER_ORDER' COMMENT '费用结算粒度：PER_ORDER/PER_BILLING/PER_STREAM_WINDOW/MONTHLY_AGGREGATED',
  `min_order_amount` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '单笔最小订单金额，0 表示不限制',
  `max_order_amount` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '单笔最大订单金额，0 表示不限制',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_payment_channel` (`merchant_id`,`chain_code`,`token_symbol`),
  KEY `idx_merchant_payment_channel_merchant` (`merchant_id`),
  KEY `idx_merchant_payment_channel_chain_token` (`chain_code`,`token_symbol`),
  KEY `idx_merchant_payment_channel_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户支付通道与费率配置表';

CREATE TABLE `merchant_balance_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `balance_token` varchar(64) NOT NULL COMMENT '余额币种',
  `available_balance` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '可用余额',
  `frozen_balance` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '提现冻结余额',
  `total_income` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '累计入账金额',
  `total_withdrawn` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '累计提现成功金额',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '账户状态：ENABLED/DISABLED',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_balance_account` (`merchant_id`,`balance_token`),
  KEY `idx_merchant_balance_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户资金余额账户表';

CREATE TABLE `merchant_balance_flow_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `flow_no` varchar(64) NOT NULL COMMENT '资金流水号',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `balance_token` varchar(64) NOT NULL COMMENT '余额币种',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型：PAYMENT_ORDER/SUBSCRIPTION_BILLING/BALANCE_ADJUST/WITHDRAW 等',
  `biz_no` varchar(64) NOT NULL COMMENT '业务单号，普通订单为 crypto_order_no，订阅账单为 billing_record.id',
  `merchant_order_no` varchar(64) DEFAULT NULL COMMENT '商户侧订单号或订阅单号',
  `chain` varchar(64) DEFAULT NULL COMMENT '链编码',
  `token_address` varchar(128) DEFAULT NULL COMMENT '代币合约地址',
  `tx_hash` varchar(128) DEFAULT NULL COMMENT '链上交易哈希',
  `direction` varchar(16) NOT NULL COMMENT '余额方向：CREDIT/DEBIT/FREEZE/UNFREEZE',
  `gross_amount` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '用户真实支付到账金额',
  `transaction_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '交易费',
  `fixed_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '固定手续费',
  `gateway_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '网关费',
  `tax_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '税费',
  `total_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '总费用',
  `net_amount` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '商户净入账金额',
  `before_available_balance` decimal(36,18) DEFAULT NULL COMMENT '变动前可用余额',
  `after_available_balance` decimal(36,18) DEFAULT NULL COMMENT '变动后可用余额',
  `status` varchar(32) NOT NULL COMMENT '流水状态：POSTED',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_balance_flow_no` (`flow_no`),
  UNIQUE KEY `uk_merchant_balance_flow_biz` (`biz_type`, `biz_no`),
  KEY `idx_merchant_balance_flow_merchant` (`merchant_id`, `balance_token`),
  KEY `idx_merchant_balance_flow_tx` (`chain`, `tx_hash`),
  KEY `idx_merchant_balance_flow_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户资金流水表';

CREATE TABLE `merchant_withdraw_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `withdraw_no` varchar(64) NOT NULL COMMENT '提现单号',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `token` varchar(64) NOT NULL COMMENT '提现币种',
  `amount` decimal(36,18) NOT NULL COMMENT '提现金额',
  `chain` varchar(64) NOT NULL COMMENT '提现链',
  `withdraw_address` varchar(128) NOT NULL COMMENT '提现收款地址',
  `tx_hash` varchar(128) DEFAULT NULL COMMENT '链上交易哈希',
  `status` varchar(32) NOT NULL COMMENT '提现状态：SUBMITTED/APPROVED/REJECTED/PROCESSING/SUCCEEDED/FAILED',
  `operator` varchar(64) DEFAULT NULL COMMENT '最后操作人',
  `operator_note` varchar(1024) DEFAULT NULL COMMENT '操作备注',
  `failure_reason` varchar(1024) DEFAULT NULL COMMENT '拒绝或失败原因',
  `requested_at` datetime(3) DEFAULT NULL COMMENT '商户申请时间',
  `reviewed_at` datetime(3) DEFAULT NULL COMMENT '审核时间',
  `submitted_at` datetime(3) DEFAULT NULL COMMENT '提交链上时间',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_withdraw_no` (`withdraw_no`),
  KEY `idx_merchant_withdraw_merchant` (`merchant_id`),
  KEY `idx_merchant_withdraw_status` (`status`),
  KEY `idx_merchant_withdraw_tx` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户提现流水表';

CREATE TABLE `merchant_balance_adjust_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `adjust_no` varchar(64) NOT NULL COMMENT '调账单号',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `token` varchar(64) NOT NULL COMMENT '币种',
  `direction` varchar(16) NOT NULL COMMENT '方向：CREDIT 增加余额，DEBIT 扣减余额',
  `amount` decimal(36,18) NOT NULL COMMENT '调账金额',
  `reason` varchar(512) DEFAULT NULL COMMENT '调账原因',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `operator_note` varchar(1024) DEFAULT NULL COMMENT '操作备注',
  `before_available_balance` decimal(36,18) DEFAULT NULL COMMENT '调账前可用余额',
  `after_available_balance` decimal(36,18) DEFAULT NULL COMMENT '调账后可用余额',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_balance_adjust_no` (`adjust_no`),
  KEY `idx_merchant_balance_adjust_merchant` (`merchant_id`),
  KEY `idx_merchant_balance_adjust_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户余额调账记录表';

CREATE TABLE `payment_order` (
  `crypto_order_no` varchar(32) NOT NULL COMMENT '平台内部支付单号',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `merchant_order_no` varchar(64) NOT NULL COMMENT '商户订单号',
  `amount` decimal(36,18) NOT NULL COMMENT '订单应付金额',
  `currency` varchar(32) NOT NULL COMMENT '订单币种',
  `chain` varchar(64) DEFAULT NULL COMMENT '最终支付链',
  `token` varchar(64) DEFAULT NULL COMMENT '最终支付币种',
  `token_address` varchar(128) DEFAULT NULL COMMENT '币种合约地址',
  `wallet_address` varchar(128) DEFAULT NULL COMMENT '用户钱包地址',
  `wallet_account_type` varchar(32) DEFAULT NULL COMMENT '钱包账户类型',
  `payment_method` varchar(32) DEFAULT NULL COMMENT '支付方式',
  `token_route_type` varchar(64) DEFAULT NULL COMMENT '路由类型',
  `route_reason` varchar(512) DEFAULT NULL COMMENT '路由原因',
  `status` varchar(32) NOT NULL COMMENT '订单状态：CREATED/METHOD_SELECTED/WAITING_PAYMENT/DETECTED/KYT_REVIEW/CONFIRMING/PAID/UNDERPAID/OVERPAID/EXPIRED/CANCELLED',
  `payment_address` varchar(128) DEFAULT NULL COMMENT '自由转账收款地址',
  `contract_address` varchar(128) DEFAULT NULL COMMENT '分账合约地址',
  `contract_call_data` longtext DEFAULT NULL COMMENT '分账调用参数',
  `gas_payer_mode` varchar(32) DEFAULT NULL COMMENT 'Gas 承担方',
  `gas_reason` varchar(512) DEFAULT NULL COMMENT 'Gas 决策原因',
  `gas_estimated_fee_wei` decimal(65,0) DEFAULT NULL COMMENT '预估 Gas',
  `gas_customer_balance_sufficient` tinyint(1) NOT NULL DEFAULT 0 COMMENT '客户 Gas 是否足够',
  `gas_platform_balance_sufficient` tinyint(1) NOT NULL DEFAULT 0 COMMENT '平台 Gas 是否足够',
  `gas_fallback_suggestion` varchar(512) DEFAULT NULL COMMENT 'Gas 兜底建议',
  `derived_address_pool_key` varchar(256) DEFAULT NULL COMMENT '派生地址池标识',
  `derived_address_lease_id` varchar(64) DEFAULT NULL COMMENT '派生地址租约号',
  `transaction_fee_rate` decimal(18,8) NOT NULL DEFAULT 0 COMMENT '交易费率快照',
  `minimum_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '保底手续费快照',
  `fixed_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '固定手续费快照',
  `gateway_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '网关费快照',
  `tax_rate` decimal(18,8) NOT NULL DEFAULT 0 COMMENT '税率快照',
  `fee_settlement_mode` varchar(32) NOT NULL DEFAULT 'PER_ORDER' COMMENT '费用结算粒度快照',
  `transaction_fee` decimal(36,18) DEFAULT NULL COMMENT '实际交易费',
  `tax_fee` decimal(36,18) DEFAULT NULL COMMENT '实际税费',
  `total_fee` decimal(36,18) DEFAULT NULL COMMENT '实际总费用',
  `settlement_amount` decimal(36,18) DEFAULT NULL COMMENT '商户净入账金额',
  `payment_tx_hash` varchar(128) DEFAULT NULL COMMENT '链上交易哈希',
  `real_amount` decimal(36,18) DEFAULT NULL COMMENT '实际到账金额',
  `paid_at` datetime(3) DEFAULT NULL COMMENT '支付完成时间',
  `late_payment` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否延迟支付',
  `late_payment_at` datetime(3) DEFAULT NULL COMMENT '延迟支付时间',
  `notify_url` varchar(512) DEFAULT NULL COMMENT '商户回调地址',
  `return_url` varchar(512) DEFAULT NULL COMMENT '商户跳转地址',
  `expire_time` datetime(3) DEFAULT NULL COMMENT '订单过期时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`crypto_order_no`),
  UNIQUE KEY `uk_payment_order_no` (`merchant_id`, `merchant_order_no`),
  UNIQUE KEY `uk_payment_order_tx_hash` (`payment_tx_hash`),
  KEY `idx_payment_order_status` (`status`),
  KEY `idx_payment_order_merchant_id` (`merchant_id`),
  KEY `idx_payment_order_chain` (`chain`),
  KEY `idx_payment_order_payment_address` (`payment_address`),
  KEY `idx_payment_order_contract_address` (`contract_address`),
  KEY `idx_payment_order_expire_time` (`expire_time`),
  KEY `idx_payment_order_status_expire_time` (`status`, `expire_time`),
  KEY `idx_payment_order_chain_token_payment_status` (`chain`, `token_address`, `payment_address`, `status`),
  KEY `idx_payment_order_chain_token_contract_status` (`chain`, `token_address`, `contract_address`, `status`),
  KEY `idx_payment_order_method_status_lease` (`payment_method`, `status`, `derived_address_pool_key`, `derived_address_lease_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主支付订单表';

CREATE TABLE `payment_exception_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exception_no` varchar(32) NOT NULL COMMENT '异常单号',
  `exception_type` varchar(64) NOT NULL COMMENT '异常类型：LATE_PAYMENT / KYT_REVIEW / KYT_REJECTED 等',
  `status` varchar(32) NOT NULL COMMENT '异常单状态：PENDING/PROCESSING/RESOLVED/IGNORED',
  `merchant_id` varchar(64) DEFAULT NULL COMMENT '商户号',
  `merchant_order_no` varchar(64) DEFAULT NULL COMMENT '商户订单号',
  `crypto_order_no` varchar(32) NOT NULL COMMENT '平台支付单号',
  `chain` varchar(64) DEFAULT NULL COMMENT '链编码',
  `token` varchar(64) DEFAULT NULL COMMENT '币种',
  `token_address` varchar(128) DEFAULT NULL COMMENT '币种合约地址',
  `payment_address` varchar(128) DEFAULT NULL COMMENT '收款地址',
  `source_address` varchar(128) DEFAULT NULL COMMENT '付款地址',
  `tx_hash` varchar(128) NOT NULL COMMENT '链上交易哈希',
  `expected_amount` decimal(36,18) DEFAULT NULL COMMENT '订单应付金额',
  `real_amount` decimal(36,18) DEFAULT NULL COMMENT '实际到账金额',
  `block_number` bigint DEFAULT NULL COMMENT '入账所在区块或 slot',
  `order_status` varchar(32) DEFAULT NULL COMMENT '创建异常单时原订单状态',
  `reason` varchar(1024) DEFAULT NULL COMMENT '异常原因说明',
  `operator` varchar(64) DEFAULT NULL COMMENT '处理人',
  `operator_note` varchar(1024) DEFAULT NULL COMMENT '处理备注',
  `handled_at` datetime(3) DEFAULT NULL COMMENT '处理时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_exception_no` (`exception_no`),
  UNIQUE KEY `uk_payment_exception_order_tx` (`crypto_order_no`, `tx_hash`),
  KEY `idx_payment_exception_status` (`status`),
  KEY `idx_payment_exception_merchant` (`merchant_id`),
  KEY `idx_payment_exception_order` (`crypto_order_no`),
  KEY `idx_payment_exception_tx_hash` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付异常单表';

CREATE TABLE `raw_chain_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chain` varchar(64) NOT NULL COMMENT '链编码',
  `tx_hash` varchar(128) NOT NULL COMMENT '交易哈希',
  `log_index` bigint NOT NULL DEFAULT 0 COMMENT '同一交易内的日志序号，原生币或无日志序号时为 0',
  `source` varchar(64) DEFAULT NULL COMMENT '扫描来源',
  `token` varchar(64) DEFAULT NULL COMMENT '币种',
  `token_address` varchar(128) DEFAULT NULL COMMENT '币种合约地址',
  `from_address` varchar(128) DEFAULT NULL COMMENT '付款地址',
  `to_address` varchar(128) DEFAULT NULL COMMENT '收款地址',
  `amount` decimal(36,18) DEFAULT NULL COMMENT '实际转账金额',
  `block_number` bigint DEFAULT NULL COMMENT '区块高度或 slot',
  `block_timestamp` datetime(3) DEFAULT NULL COMMENT '链上区块时间；拿不到时使用扫描观察时间',
  `status` varchar(32) NOT NULL DEFAULT 'UNMATCHED' COMMENT '处理状态：MATCHED/UNMATCHED/MANUAL_PROCESSED',
  `matched_crypto_order_no` varchar(32) DEFAULT NULL COMMENT '匹配到的平台订单号',
  `match_reason` varchar(1024) DEFAULT NULL COMMENT '匹配说明或失败原因',
  `operator` varchar(64) DEFAULT NULL COMMENT '运营处理人',
  `operator_note` varchar(1024) DEFAULT NULL COMMENT '运营处理备注',
  `manual_processed_at` datetime(3) DEFAULT NULL COMMENT '运营处理时间',
  `observed_at` datetime(3) DEFAULT NULL COMMENT '扫描观察时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_raw_chain_log_chain_tx_index` (`chain`, `tx_hash`, `log_index`),
  KEY `idx_raw_chain_log_tx_hash` (`tx_hash`),
  KEY `idx_raw_chain_log_to_address` (`to_address`),
  KEY `idx_raw_chain_log_status` (`status`),
  KEY `idx_raw_chain_log_block` (`chain`, `block_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原始链上流水表';

CREATE TABLE `payment_reconciliation_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `reconcile_key` varchar(256) NOT NULL COMMENT '对账唯一键，避免同一业务对象重复生成对账记录',
  `biz_type` varchar(32) NOT NULL COMMENT '对账对象类型：ORDER/RAW_CHAIN_LOG',
  `biz_key` varchar(256) NOT NULL COMMENT '对账对象主键，如平台订单号或 chain:txHash:logIndex',
  `merchant_id` varchar(64) DEFAULT NULL COMMENT '商户号',
  `crypto_order_no` varchar(32) DEFAULT NULL COMMENT '平台支付单号',
  `merchant_order_no` varchar(64) DEFAULT NULL COMMENT '商户订单号',
  `chain` varchar(64) DEFAULT NULL COMMENT '链编码',
  `token` varchar(64) DEFAULT NULL COMMENT '币种',
  `token_address` varchar(128) DEFAULT NULL COMMENT '代币合约地址',
  `tx_hash` varchar(128) DEFAULT NULL COMMENT '交易哈希',
  `log_index` bigint DEFAULT NULL COMMENT '日志序号',
  `expected_amount` decimal(36,18) DEFAULT NULL COMMENT '订单应收金额',
  `real_amount` decimal(36,18) DEFAULT NULL COMMENT '链上实际到账金额',
  `diff_amount` decimal(36,18) DEFAULT NULL COMMENT '差额：realAmount - expectedAmount',
  `order_status` varchar(32) DEFAULT NULL COMMENT '订单状态快照',
  `raw_log_status` varchar(32) DEFAULT NULL COMMENT '原始链上流水状态快照',
  `callback_status` varchar(32) DEFAULT NULL COMMENT '回调状态快照',
  `settlement_status` varchar(32) DEFAULT NULL COMMENT '合约分账状态快照',
  `reconcile_status` varchar(32) NOT NULL COMMENT '对账状态：MATCHED/WARNING/MISMATCH/MANUAL_CONFIRMED',
  `issue_type` varchar(64) NOT NULL COMMENT '问题类型：NONE/AMOUNT_MISMATCH/UNMATCHED_CHAIN_LOG 等',
  `issue_reason` varchar(1024) DEFAULT NULL COMMENT '问题说明',
  `operator` varchar(64) DEFAULT NULL COMMENT '运营处理人',
  `operator_note` varchar(1024) DEFAULT NULL COMMENT '运营处理备注',
  `manual_confirmed_at` datetime(3) DEFAULT NULL COMMENT '人工确认时间',
  `reconciled_at` datetime(3) DEFAULT NULL COMMENT '最近对账时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_reconcile_key` (`reconcile_key`),
  KEY `idx_payment_reconcile_order` (`crypto_order_no`),
  KEY `idx_payment_reconcile_merchant` (`merchant_id`),
  KEY `idx_payment_reconcile_status` (`reconcile_status`),
  KEY `idx_payment_reconcile_issue` (`issue_type`),
  KEY `idx_payment_reconcile_tx` (`tx_hash`),
  KEY `idx_payment_reconcile_time` (`reconciled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付对账结果表';

CREATE TABLE `pending_chain_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chain` varchar(64) NOT NULL COMMENT '链编码',
  `tx_hash` varchar(128) NOT NULL COMMENT '交易哈希',
  `log_index` bigint NOT NULL DEFAULT 0 COMMENT '同一交易内的日志序号，原生币或无日志序号时为 0',
  `source` varchar(64) DEFAULT NULL COMMENT '扫描来源',
  `token` varchar(64) DEFAULT NULL COMMENT '币种',
  `token_address` varchar(128) DEFAULT NULL COMMENT '币种合约地址',
  `from_address` varchar(128) DEFAULT NULL COMMENT '付款地址',
  `to_address` varchar(128) DEFAULT NULL COMMENT '收款地址',
  `amount` decimal(36,18) DEFAULT NULL COMMENT '实际转账金额',
  `block_number` bigint DEFAULT NULL COMMENT '交易所在区块高度或 slot',
  `block_timestamp` datetime(3) DEFAULT NULL COMMENT '链上区块时间',
  `target_confirmations` int NOT NULL COMMENT '目标确认数',
  `current_confirmations` int NOT NULL DEFAULT 0 COMMENT '当前已观察确认数，仅用于展示',
  `status` varchar(32) NOT NULL COMMENT '旁路状态：PENDING/DISPATCHED/MATCHED/CANCELLED',
  `matched_crypto_order_no` varchar(32) DEFAULT NULL COMMENT '提前匹配到的平台订单号',
  `observed_at` datetime(3) DEFAULT NULL COMMENT '首次观察时间',
  `last_dispatched_at` datetime(3) DEFAULT NULL COMMENT '最近一次投递到入账链路时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pending_chain_tx_index` (`chain`, `tx_hash`, `log_index`),
  KEY `idx_pending_chain_status_block` (`chain`, `status`, `block_number`),
  KEY `idx_pending_order` (`matched_crypto_order_no`),
  KEY `idx_pending_to_address` (`to_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待确认链上交易旁路表';

CREATE TABLE `cashier_token_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `short_code` varchar(64) NOT NULL COMMENT '收银台短码，暴露在支付链接中',
  `encrypted_token` text NOT NULL COMMENT '完整加密收银台 token',
  `crypto_order_no` varchar(32) NOT NULL COMMENT '平台内部支付单号',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `expire_at` datetime(3) NOT NULL COMMENT '短码过期时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cashier_token_short_code` (`short_code`),
  KEY `idx_cashier_token_order` (`crypto_order_no`),
  KEY `idx_cashier_token_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收银台短码映射表';

CREATE TABLE `payment_platform_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `order_expire_minutes` int NOT NULL DEFAULT 15 COMMENT '订单过期分钟数',
  `cashier_base_url` varchar(512) NOT NULL COMMENT '收银台基础地址',
  `treasury_address` varchar(128) DEFAULT NULL COMMENT '平台归集地址',
  `idempotency_wait_millis` bigint NOT NULL DEFAULT 4000 COMMENT '重复幂等请求等待首个请求完成的最大毫秒数',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_platform_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台基础配置表';

CREATE TABLE `payment_gateway_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `gateway_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用网关',
  `service_fee_token` varchar(32) NOT NULL DEFAULT 'USDC' COMMENT '服务费币种',
  `service_fee_amount` decimal(36,18) NOT NULL DEFAULT 0.10 COMMENT '服务费金额',
  `api_key` varchar(256) DEFAULT NULL COMMENT '网关访问密钥',
  `public_resources_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否允许公开资源',
  `facilitator_name` varchar(128) NOT NULL DEFAULT 'Crypto Gateway X402 Facilitator' COMMENT 'Facilitator 名称',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_gateway_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='x402 网关配置表';

CREATE TABLE `payment_discovery_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `discovery_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用能力发现',
  `public_base_url` varchar(512) NOT NULL COMMENT '对外暴露的基础 URL',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_discovery_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能力发现配置表';

CREATE TABLE `payment_scanner_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `confirmation_depth` int NOT NULL DEFAULT 3 COMMENT '确认块深度',
  `scan_interval_seconds` int NOT NULL DEFAULT 15 COMMENT '扫描间隔秒数',
  `checkpoint_flush_blocks` int NOT NULL DEFAULT 20 COMMENT 'Redis 检查点批量落库阈值',
  `backfill_blocks` int NOT NULL DEFAULT 12 COMMENT '每轮回补扫描的最近确认块数量，用于抵抗 WSS/RPC 抖动和短暂中断',
  `log_scan_batch_blocks` int NOT NULL DEFAULT 10 COMMENT 'ERC20 Transfer 日志单次 eth_getLogs 扫描的最大区块跨度',
  `log_scan_retry_attempts` int NOT NULL DEFAULT 3 COMMENT 'ERC20 日志分段扫描失败后的最大重试次数',
  `failure_cooldown_seconds` int NOT NULL DEFAULT 60 COMMENT '扫描失败后的冷却秒数，避免无效 RPC 每轮刷屏或压测节点',
  `websocket_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 EVM WebSocket 区块头监听',
  `websocket_leader_lease_seconds` int NOT NULL DEFAULT 45 COMMENT 'WSS leader Redis 租约秒数，多节点仅 leader 保持订阅',
  `chain_replay_ttl_hours` bigint NOT NULL DEFAULT 720 COMMENT '链上交易防重放保留小时数，默认 30 天',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_scanner_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链扫描配置表';

CREATE TABLE `payment_chain_scanner_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chain_code` varchar(64) NOT NULL COMMENT '链编码，对应 payment_chain_config.chain_code',
  `confirmation_depth` int DEFAULT NULL COMMENT '链级确认块深度，空值使用全局配置',
  `scan_interval_seconds` int DEFAULT NULL COMMENT '链级扫描间隔秒数，空值使用全局配置',
  `checkpoint_flush_blocks` int DEFAULT NULL COMMENT '链级 Redis 检查点批量落库阈值，空值使用全局配置',
  `backfill_blocks` int DEFAULT NULL COMMENT '链级最近确认块回补数量，空值使用全局配置',
  `log_scan_batch_blocks` int DEFAULT NULL COMMENT '链级 ERC20 Transfer 日志单次 eth_getLogs 最大区块跨度，空值使用全局配置',
  `log_scan_retry_attempts` int DEFAULT NULL COMMENT '链级 ERC20 日志分段扫描失败重试次数，空值使用全局配置',
  `failure_cooldown_seconds` int DEFAULT NULL COMMENT '链级扫描失败后的冷却秒数，空值使用全局配置',
  `websocket_enabled` tinyint(1) DEFAULT NULL COMMENT '链级是否启用 EVM WebSocket 区块头监听，空值使用全局配置',
  `websocket_leader_lease_seconds` int DEFAULT NULL COMMENT '链级 WSS leader Redis 租约秒数，空值使用全局配置',
  `chain_replay_ttl_hours` bigint DEFAULT NULL COMMENT '链级交易防重放保留小时数，空值使用全局配置',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用该链级覆盖配置',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_chain_scanner_chain` (`chain_code`),
  KEY `idx_payment_chain_scanner_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单链扫描覆盖配置表';

CREATE TABLE `payment_signature_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
    `signature_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '总开关',
  `verify_inbound_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否校验入站签名',
  `sign_outbound_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否签出站回调',
  `algorithm` varchar(32) NOT NULL DEFAULT 'RSA_SHA256' COMMENT '签名算法',
  `merchant_id_header` varchar(64) NOT NULL DEFAULT 'X-Merchant-Id' COMMENT '商户头',
  `timestamp_header` varchar(64) NOT NULL DEFAULT 'X-Timestamp' COMMENT '时间戳头',
  `nonce_header` varchar(64) NOT NULL DEFAULT 'X-Nonce' COMMENT '随机数头',
  `signature_header` varchar(64) NOT NULL DEFAULT 'X-Signature' COMMENT '签名头',
  `key_version_header` varchar(64) NOT NULL DEFAULT 'X-Key-Version' COMMENT '密钥版本头',
  `allowed_clock_skew_seconds` bigint NOT NULL DEFAULT 300 COMMENT '允许时间偏移',
  `replay_ttl_seconds` bigint NOT NULL DEFAULT 600 COMMENT '重放窗口秒数',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_signature_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签名配置表';

CREATE TABLE `payment_cashier_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `token_encryption_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否加密收银台 token',
  `token_key_alias` varchar(64) NOT NULL DEFAULT 'cashier-v1' COMMENT 'token 密钥别名',
  `token_ttl_minutes` int NOT NULL DEFAULT 60 COMMENT 'token 过期分钟数',
  `token_prefix` varchar(32) NOT NULL DEFAULT 'cashier' COMMENT 'token 前缀',
  `short_token_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用收银台短码映射',
  `short_token_prefix` varchar(16) NOT NULL DEFAULT 'c' COMMENT '短码前缀',
  `short_token_length` int NOT NULL DEFAULT 12 COMMENT '短码随机部分长度',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_cashier_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收银台配置表';

CREATE TABLE `payment_subscription_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `subscription_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用订阅',
  `default_mode` varchar(64) NOT NULL DEFAULT 'SUPERFLUID_STREAM' COMMENT '默认订阅模式',
  `default_cycle_seconds` int NOT NULL DEFAULT 2592000 COMMENT '默认周期秒数',
  `superfluid_host_address` varchar(128) DEFAULT NULL COMMENT 'Superfluid Host 地址',
  `superfluid_cfa_address` varchar(128) DEFAULT NULL COMMENT 'CFA 合约地址',
  `erc1337_executor_address` varchar(128) DEFAULT NULL COMMENT 'ERC-1337 执行器地址',
  `scheduler_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用订阅扣款调度',
  `scheduler_interval_seconds` int NOT NULL DEFAULT 30 COMMENT '订阅扣款调度间隔秒数；秒级订阅测试可配置为 1',
  `stream_settlement_interval_seconds` int NOT NULL DEFAULT 3600 COMMENT 'Superfluid 真流式结算窗口秒数，不表示链上每秒扣款',
  `scheduler_batch_size` int NOT NULL DEFAULT 50 COMMENT '单次调度最大处理数量',
  `max_retry_count` int NOT NULL DEFAULT 3 COMMENT '订阅账单最大重试次数',
  `retry_backoff_seconds` int NOT NULL DEFAULT 300 COMMENT '订阅账单失败后的重试间隔秒数',
  `execution_gas_limit` bigint NOT NULL DEFAULT 500000 COMMENT '平台执行器发交易 Gas 上限',
  `executor_private_key_source_type` varchar(16) NOT NULL DEFAULT 'ENV' COMMENT '执行器私钥来源：ENV/YAML/KMS/JNI',
  `executor_private_key_env` varchar(128) DEFAULT 'CRYPTO_PAYMENT_SUBSCRIPTION_EXECUTOR_PRIVATE_KEY' COMMENT '执行器私钥环境变量名',
  `executor_private_key_kms_key_id` varchar(128) DEFAULT 'subscription-executor-private-key' COMMENT '执行器私钥 KMS keyId',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_subscription_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅支付配置表';

CREATE TABLE `payment_gas_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `gas_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 Gas 逻辑',
  `hosted_wallet_prefer_platform` tinyint(1) NOT NULL DEFAULT 1 COMMENT '托管钱包优先平台代付',
  `hosted_wallet_sponsor_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否允许平台赞助 Gas',
  `evm_sponsor_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用 EVM 代付；通常用于 USDC EIP-3009 或后续 Paymaster/Relayer',
  `evm_relayer_private_key_source_type` varchar(16) NOT NULL DEFAULT 'ENV' COMMENT 'EVM relayer 私钥来源：ENV/YAML/KMS/JNI',
  `evm_relayer_private_key_env` varchar(128) DEFAULT 'CRYPTO_PAYMENT_EVM_RELAYER_PRIVATE_KEY' COMMENT 'EVM relayer 私钥环境变量名',
  `evm_relayer_private_key_kms_key_id` varchar(128) DEFAULT 'evm-relayer-private-key' COMMENT 'EVM relayer 私钥 KMS keyId',
  `sponsor_protection_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Gas 代付防刷开关',
  `sponsor_counter_ttl_seconds` int NOT NULL DEFAULT 3600 COMMENT 'Gas 代付限频计数窗口秒数',
  `sponsor_order_max_attempts` int NOT NULL DEFAULT 3 COMMENT '同一订单窗口内最大代付尝试次数',
  `sponsor_wallet_max_attempts` int NOT NULL DEFAULT 10 COMMENT '同一用户钱包窗口内最大代付尝试次数',
  `sponsor_ip_max_attempts` int NOT NULL DEFAULT 30 COMMENT '同一 IP 窗口内最大代付尝试次数',
  `sponsor_cashier_token_max_attempts` int NOT NULL DEFAULT 10 COMMENT '同一收银台 token 窗口内最大代付尝试次数',
  `sponsor_order_lock_lease_seconds` int NOT NULL DEFAULT 60 COMMENT '同一订单代付提交分布式锁持有秒数',
  `customer_balance_precheck_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否预检客户余额',
  `platform_sponsored_gas_limit` int NOT NULL DEFAULT 250000 COMMENT '平台代付 Gas 上限',
  `customer_gas_limit` int NOT NULL DEFAULT 120000 COMMENT '客户自付 Gas 上限',
  `free_transfer_gas_limit` int NOT NULL DEFAULT 21000 COMMENT '自由转账 Gas 上限',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_gas_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gas 策略配置表';

CREATE TABLE `payment_gas_low_fee_chain` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chain_code` varchar(64) NOT NULL COMMENT '链编码',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_gas_low_fee_chain` (`chain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低费链候选表';

CREATE TABLE `gas_sponsor_attempt_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `crypto_order_no` varchar(64) NOT NULL COMMENT '平台支付单号',
  `merchant_id` varchar(64) DEFAULT NULL COMMENT '商户号，仅用于审计统计，当前不做商户维度限额',
  `chain` varchar(64) DEFAULT NULL COMMENT '链编码',
  `token` varchar(64) DEFAULT NULL COMMENT '币种标识',
  `token_address` varchar(128) DEFAULT NULL COMMENT '币种合约地址',
  `wallet_address` varchar(128) DEFAULT NULL COMMENT '用户钱包地址',
  `ip_address` varchar(64) DEFAULT NULL COMMENT '客户端 IP',
  `cashier_token_hash` varchar(128) DEFAULT NULL COMMENT '收银台 token 哈希，避免明文落库',
  `provider_id` varchar(64) NOT NULL COMMENT '代付 provider，如 EIP3009 / SOLANA_FEE_PAYER',
  `request_type` varchar(64) NOT NULL COMMENT '代付请求类型，如 TRANSFER_WITH_AUTHORIZATION / FEE_PAYER_SIGN',
  `status` varchar(32) NOT NULL COMMENT '处理状态：SUCCESS / FAILED / REJECTED',
  `tx_hash` varchar(128) DEFAULT NULL COMMENT '代付上链交易哈希或 Solana signature',
  `reject_reason` varchar(512) DEFAULT NULL COMMENT '失败或拦截原因',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_gas_sponsor_order` (`crypto_order_no`),
  KEY `idx_gas_sponsor_wallet` (`wallet_address`),
  KEY `idx_gas_sponsor_ip` (`ip_address`),
  KEY `idx_gas_sponsor_token_hash` (`cashier_token_hash`),
  KEY `idx_gas_sponsor_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gas 代付尝试记录表';

CREATE TABLE `payment_kyt_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `kyt_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 KYT',
  `strict_mode` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否严格模式',
  `review_threshold` int NOT NULL DEFAULT 60 COMMENT '复核阈值',
  `reject_threshold` int NOT NULL DEFAULT 80 COMMENT '拒绝阈值',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_kyt_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KYT 全局配置表';

CREATE TABLE `payment_kyt_address_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `address` varchar(128) NOT NULL COMMENT '地址',
  `rule_type` varchar(16) NOT NULL COMMENT '规则类型：ALLOW / DENY',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_kyt_address_rule` (`address`, `rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KYT 地址规则表';

CREATE TABLE `payment_kyt_chain_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chain_code` varchar(64) NOT NULL COMMENT '链编码',
  `rule_type` varchar(32) NOT NULL COMMENT '规则类型',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_kyt_chain_rule` (`chain_code`, `rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KYT 风险链规则表';

CREATE TABLE `payment_kyt_token_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chain_code` varchar(64) DEFAULT NULL COMMENT '链编码',
  `token_symbol` varchar(64) NOT NULL COMMENT '币种标识',
  `rule_type` varchar(32) NOT NULL COMMENT '规则类型',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_kyt_token_rule` (`chain_code`, `token_symbol`, `rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KYT 风险币种规则表';

CREATE TABLE `payment_chain_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chain_code` varchar(64) NOT NULL COMMENT '链编码',
  `rpc_url` varchar(512) NOT NULL COMMENT 'RPC 地址',
  `ws_url` varchar(512) DEFAULT NULL COMMENT 'WebSocket 地址',
  `confirmation_depth` int DEFAULT NULL COMMENT '链级确认块数，空值使用全局配置',
  `sponsor_enabled` tinyint(1) DEFAULT NULL COMMENT '单链代付开关：NULL 使用全局配置，1 启用，0 禁用',
  `sponsor_provider` varchar(64) DEFAULT 'AUTO' COMMENT '单链代付实现：AUTO/EIP3009/SOLANA_FEE_PAYER/PAYMASTER/RELAYER/NONE',
  `relayer_address` varchar(128) DEFAULT NULL COMMENT '单链平台 relayer/fee payer 地址，空值使用全局或密钥推导',
  `sponsor_gas_limit` bigint DEFAULT NULL COMMENT '单链代付 gas/lamports 上限，空值使用全局配置',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_chain_code` (`chain_code`),
  KEY `idx_payment_chain_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链配置表';

CREATE TABLE `payment_token_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chain_code` varchar(64) NOT NULL COMMENT '链编码',
  `token_symbol` varchar(64) NOT NULL COMMENT '币种标识',
  `token_address` varchar(128) NOT NULL COMMENT '币种合约地址',
  `decimals` int NOT NULL DEFAULT 6 COMMENT '精度',
  `confirmation_depth` int DEFAULT NULL COMMENT '币种级确认块数，空值使用链级或全局配置',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_token_chain_symbol` (`chain_code`, `token_symbol`),
  UNIQUE KEY `uk_payment_token_chain_address` (`chain_code`, `token_address`),
  KEY `idx_payment_token_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='币种基础配置表';

CREATE TABLE `payment_token_capability` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chain_code` varchar(64) NOT NULL COMMENT '链编码',
  `token_symbol` varchar(64) NOT NULL COMMENT '币种标识',
  `transfer_with_authorization` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否支持 EIP-3009',
  `permit` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否支持 Permit',
  `approve` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否支持 approve',
  `smart_contract_settlement` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否支持合约分账',
  `settlement_contract_address` varchar(128) DEFAULT NULL COMMENT '分账合约地址',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_token_capability` (`chain_code`, `token_symbol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='币种协议能力表';

CREATE TABLE `payment_contract_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `contract_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用合约',
  `contract_address` varchar(128) NOT NULL COMMENT '分账合约地址',
  `create2_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用 CREATE2 隔离地址模式',
  `create2_hosted_wallet_only` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否仅对托管钱包启用 CREATE2',
  `create2_factory_address` varchar(128) DEFAULT NULL COMMENT 'CREATE2 工厂合约地址',
  `create2_init_code_hash` varchar(128) DEFAULT NULL COMMENT 'CREATE2 初始化代码哈希',
  `create2_salt_prefix` varchar(128) DEFAULT NULL COMMENT 'CREATE2 盐前缀',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_contract_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合约配置表';

CREATE TABLE `payment_contract_split_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `role_code` varchar(64) NOT NULL COMMENT '角色代码',
  `receiver_address` varchar(128) NOT NULL COMMENT '收款地址',
  `basis_points` int NOT NULL COMMENT '分账基点',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_contract_split_role` (`config_scope`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合约分账规则表';

CREATE TABLE `payment_derived_address_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `derived_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用派生地址',
  `reuse_address` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否复用地址',
  `reuse_cooldown_minutes` int NOT NULL DEFAULT 60 COMMENT '地址复用冷置分钟数，仅 reuse_address=1 时生效',
  `mode` varchar(64) NOT NULL DEFAULT 'ADDRESS_FACTORY' COMMENT '派生模式：HD/SOLANA_HD/KEYSTORE/THIRD_PARTY_API/ADDRESS_FACTORY；mode=HD且链为Solana时自动走SOLANA_HD',
  `hd_mnemonic_source_type` varchar(16) NOT NULL DEFAULT 'ENV' COMMENT '助记词来源：ENV/YAML/KMS/JNI；JNI 表示通过本地 .so/.dll 解密密文',
  `hd_mnemonic_ciphertext` varchar(2048) DEFAULT NULL COMMENT '助记词密文或 YAML 明文；JNI/KMS/YAML 模式下作为 configuredValue 传入解析器',
  `hd_mnemonic_key_id` varchar(128) DEFAULT 'derived-hd-mnemonic' COMMENT '助记词密钥标识；KMS/JNI 模式下传给对应 Provider',
  `hd_mnemonic_env` varchar(128) DEFAULT 'CRYPTO_PAYMENT_DERIVED_HD_MNEMONIC' COMMENT '助记词环境变量名',
  `hd_passphrase_env` varchar(128) DEFAULT NULL COMMENT '助记词密码环境变量名',
  `hd_derivation_path_prefix` varchar(128) DEFAULT 'm/44''/60''/0''/0' COMMENT '派生路径前缀',
  `hd_start_index` int NOT NULL DEFAULT 0 COMMENT '起始派生索引',
  `hd_generate_mnemonic_when_missing` tinyint(1) NOT NULL DEFAULT 1 COMMENT '缺失时是否生成助记词',
  `keystore_output_dir` varchar(256) DEFAULT './wallet-keys' COMMENT 'Keystore 输出目录',
  `keystore_password_env` varchar(128) DEFAULT 'CRYPTO_PAYMENT_DERIVED_KEYSTORE_PASSWORD' COMMENT 'Keystore 密码环境变量名',
  `keystore_file_prefix` varchar(64) DEFAULT 'crypto-wallet' COMMENT 'Keystore 文件前缀',
  `third_party_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用第三方托管 API',
  `third_party_base_url` varchar(512) DEFAULT NULL COMMENT '第三方 API 基础地址',
  `third_party_create_path` varchar(256) DEFAULT '/wallets/derived' COMMENT '第三方建地址路径',
  `third_party_api_key_env` varchar(128) DEFAULT 'CRYPTO_PAYMENT_DERIVED_THIRD_PARTY_API_KEY' COMMENT '第三方 API 密钥环境变量名',
  `third_party_provider_name` varchar(128) DEFAULT 'external-wallet-provider' COMMENT '第三方提供方名称',
  `address_factory_namespace` varchar(64) DEFAULT 'crypto' COMMENT '地址工厂命名空间',
  `address_factory_seed_prefix` varchar(64) DEFAULT 'derived' COMMENT '地址工厂种子前缀',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_derived_address_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派生地址策略配置表';

CREATE TABLE `payment_security_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `validate_redirect_url` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否校验跳转 URL',
  `allow_local_redirect` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否允许本地跳转',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_security_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全配置表';

CREATE TABLE `payment_redirect_host` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `host` varchar(255) NOT NULL COMMENT '允许跳转的主机名',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_redirect_host` (`host`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='允许跳转域名表';

CREATE TABLE `payment_callback_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_scope` varchar(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '配置作用域',
  `retry_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用重试',
  `max_attempts` int NOT NULL DEFAULT 6 COMMENT '最大重试次数',
  `initial_backoff_seconds` bigint NOT NULL DEFAULT 15 COMMENT '初始退避秒数',
  `max_backoff_seconds` bigint NOT NULL DEFAULT 900 COMMENT '最大退避秒数',
  `retention_days` bigint NOT NULL DEFAULT 14 COMMENT '回调记录保留天数',
  `dead_letter_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用死信',
  `dispatch_immediately` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否入队后立即投递；关闭后仅入队，由调度器按批量消费实现削峰',
  `dispatch_batch_size` int NOT NULL DEFAULT 100 COMMENT '单轮扫描最多投递的到期回调数量',
  `dispatch_lock_wait_millis` bigint NOT NULL DEFAULT 2000 COMMENT '单条回调投递锁等待毫秒数',
  `dispatch_lock_lease_seconds` bigint NOT NULL DEFAULT 60 COMMENT '单条回调投递锁租约秒数，应大于 HTTP call timeout',
  `max_stored_response_chars` int NOT NULL DEFAULT 8000 COMMENT '最后响应内容最大保存字符数',
  `max_stored_error_chars` int NOT NULL DEFAULT 4000 COMMENT '最后错误信息最大保存字符数',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_callback_scope` (`config_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调策略表';

CREATE TABLE `subscription_order` (
  `subscription_order_no` varchar(32) NOT NULL COMMENT '平台订阅订单号',
  `merchant_order_no` varchar(64) NOT NULL COMMENT '商户订阅订单号',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `amount_per_cycle` decimal(36,18) NOT NULL COMMENT '每周期扣款金额',
  `currency` varchar(32) NOT NULL COMMENT '订阅币种',
  `chain` varchar(64) NOT NULL COMMENT '订阅链',
  `token` varchar(64) NOT NULL COMMENT '订阅 Token',
  `token_address` varchar(128) DEFAULT NULL COMMENT 'Token 合约地址',
  `payer_address` varchar(128) DEFAULT NULL COMMENT '付款钱包地址',
  `recipient_address` varchar(128) NOT NULL COMMENT '收款地址',
  `billing_mode` varchar(32) NOT NULL COMMENT '扣款模式',
  `cycle_seconds` int NOT NULL COMMENT '扣款周期秒数',
  `transaction_fee_rate` decimal(18,8) NOT NULL DEFAULT 0 COMMENT '交易费率快照',
  `minimum_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '保底手续费快照',
  `fixed_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '固定手续费快照',
  `gateway_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '网关费快照',
  `tax_rate` decimal(18,8) NOT NULL DEFAULT 0 COMMENT '税率快照',
  `fee_settlement_mode` varchar(32) NOT NULL DEFAULT 'PER_BILLING' COMMENT '费用结算粒度快照',
  `notify_url` varchar(512) NOT NULL COMMENT '商户回调地址',
  `return_url` varchar(512) DEFAULT NULL COMMENT '商户跳转地址',
  `setup_contract_address` varchar(128) DEFAULT NULL COMMENT '订阅初始化合约地址',
  `setup_payload` longtext DEFAULT NULL COMMENT '订阅初始化调用参数',
  `setup_reason` varchar(512) DEFAULT NULL COMMENT '订阅初始化说明',
  `setup_tx_hash` varchar(128) DEFAULT NULL COMMENT '初始化订阅的链上交易哈希',
  `subscription_event_id` varchar(128) DEFAULT NULL COMMENT '链上订阅事件ID，通常为订阅单号哈希后的bytes32',
  `status` varchar(32) NOT NULL COMMENT '订阅状态',
  `next_billing_at` datetime(3) DEFAULT NULL COMMENT '下次扣款时间',
  `last_billing_at` datetime(3) DEFAULT NULL COMMENT '最近一次扣款时间',
  `activated_at` datetime(3) DEFAULT NULL COMMENT '订阅激活时间',
  `paused_at` datetime(3) DEFAULT NULL COMMENT '订阅暂停时间',
  `cancelled_at` datetime(3) DEFAULT NULL COMMENT '订阅取消时间',
  `failure_reason` varchar(512) DEFAULT NULL COMMENT '最近失败原因',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`subscription_order_no`),
  UNIQUE KEY `uk_subscription_order_no` (`merchant_id`, `merchant_order_no`),
  KEY `idx_subscription_order_setup_tx` (`setup_tx_hash`),
  KEY `idx_subscription_order_event_id` (`subscription_event_id`),
  KEY `idx_subscription_order_status` (`status`),
  KEY `idx_subscription_order_next_billing_at` (`next_billing_at`),
  KEY `idx_subscription_order_status_next_billing` (`status`, `next_billing_at`),
  KEY `idx_subscription_order_chain` (`chain`),
  KEY `idx_subscription_order_recipient` (`recipient_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅订单表';

CREATE TABLE `subscription_billing_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subscription_order_no` varchar(32) NOT NULL COMMENT '平台订阅订单号',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `merchant_order_no` varchar(64) NOT NULL COMMENT '商户订阅订单号',
  `billing_sequence` int NOT NULL COMMENT '账单期数，从 1 开始递增',
  `amount` decimal(36,18) NOT NULL COMMENT '本期应扣金额',
  `currency` varchar(32) NOT NULL COMMENT '币种',
  `chain` varchar(64) NOT NULL COMMENT '链编码',
  `token` varchar(64) NOT NULL COMMENT '代币符号',
  `token_address` varchar(128) DEFAULT NULL COMMENT '代币合约地址',
  `due_at` datetime(3) NOT NULL COMMENT '本期计划扣款时间',
  `status` varchar(32) NOT NULL COMMENT '账单状态：PENDING/EXECUTING/KYT_REVIEW/PAID/FAILED/CANCELLED',
  `execution_tx_hash` varchar(128) DEFAULT NULL COMMENT '链上扣款交易哈希',
  `confirmed_block_number` bigint DEFAULT NULL COMMENT '确认区块高度',
  `real_amount` decimal(36,18) DEFAULT NULL COMMENT '实际扣款/到账金额',
  `transaction_fee_rate` decimal(18,8) NOT NULL DEFAULT 0 COMMENT '交易费率快照',
  `minimum_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '保底手续费快照',
  `fixed_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '固定手续费快照',
  `gateway_fee` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '网关费快照',
  `tax_rate` decimal(18,8) NOT NULL DEFAULT 0 COMMENT '税率快照',
  `fee_settlement_mode` varchar(32) NOT NULL DEFAULT 'PER_BILLING' COMMENT '费用结算粒度快照',
  `transaction_fee` decimal(36,18) DEFAULT NULL COMMENT '实际交易费',
  `tax_fee` decimal(36,18) DEFAULT NULL COMMENT '实际税费',
  `total_fee` decimal(36,18) DEFAULT NULL COMMENT '实际总费用',
  `settlement_amount` decimal(36,18) DEFAULT NULL COMMENT '商户净入账金额',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `next_retry_at` datetime(3) DEFAULT NULL COMMENT '下次重试时间',
  `failure_reason` varchar(512) DEFAULT NULL COMMENT '最近失败原因',
  `paid_at` datetime(3) DEFAULT NULL COMMENT '支付确认时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subscription_billing_sequence` (`subscription_order_no`, `billing_sequence`),
  UNIQUE KEY `uk_subscription_billing_tx` (`execution_tx_hash`),
  KEY `idx_subscription_billing_status_due` (`status`, `due_at`),
  KEY `idx_subscription_billing_retry` (`status`, `next_retry_at`),
  KEY `idx_subscription_billing_merchant` (`merchant_id`, `merchant_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅周期账单表';

CREATE TABLE `merchant_service_fee_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `fee_token` varchar(32) NOT NULL COMMENT '服务费币种',
  `balance` decimal(36,18) NOT NULL DEFAULT 0 COMMENT '服务费余额',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_service_fee_account` (`merchant_id`, `fee_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户服务费余额表';

CREATE TABLE `merchant_signature_key` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `key_version` varchar(64) NOT NULL COMMENT '密钥版本',
  `algorithm` varchar(32) NOT NULL COMMENT '签名算法',
  `public_key_pem` longtext NOT NULL COMMENT '商户公钥 PEM',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_signature_key` (`merchant_id`, `key_version`),
  KEY `idx_merchant_signature_key_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户签名公钥表';

CREATE TABLE `platform_signing_key` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_alias` varchar(64) NOT NULL COMMENT '平台签名密钥别名',
  `algorithm` varchar(32) NOT NULL COMMENT '签名算法',
  `private_key_pem` longtext NOT NULL COMMENT '平台私钥 PEM',
  `public_key_pem` longtext NOT NULL COMMENT '平台公钥 PEM',
  `active` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否当前启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_signing_key_alias` (`key_alias`),
  KEY `idx_platform_signing_key_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台签名私钥表';

CREATE TABLE `platform_cipher_key` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_alias` varchar(64) NOT NULL COMMENT '平台加密密钥别名',
  `algorithm` varchar(32) NOT NULL COMMENT '加密算法',
  `secret_key_base64` longtext NOT NULL COMMENT 'Base64 编码密钥',
  `active` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否当前启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_cipher_key_alias` (`key_alias`),
  KEY `idx_platform_cipher_key_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台加密密钥表';

CREATE TABLE `request_signature_replay_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scope` varchar(64) NOT NULL COMMENT '防重放作用域',
  `merchant_id` varchar(64) NOT NULL COMMENT '商户号',
  `nonce` varchar(128) NOT NULL COMMENT '请求随机数',
  `request_hash` varchar(128) NOT NULL COMMENT '请求摘要',
  `expires_at` datetime(3) NOT NULL COMMENT '过期时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_signature_replay` (`scope`, `merchant_id`, `nonce`),
  KEY `idx_request_signature_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请求签名重放防护表';

CREATE TABLE `contract_settlement_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `crypto_order_no` varchar(32) NOT NULL COMMENT '平台订单号',
  `merchant_order_no` varchar(64) NOT NULL COMMENT '商户订单号',
  `chain` varchar(64) NOT NULL COMMENT '结算链',
  `token` varchar(64) NOT NULL COMMENT '结算币种',
  `token_address` varchar(128) DEFAULT NULL COMMENT 'Token 合约地址',
  `contract_address` varchar(128) DEFAULT NULL COMMENT '分账合约地址',
  `amount` decimal(36,18) NOT NULL COMMENT '结算总金额',
  `status` varchar(32) NOT NULL COMMENT '结算状态',
  `settlement_mode` varchar(32) DEFAULT NULL COMMENT '结算模式',
  `split_count` int NOT NULL DEFAULT 0 COMMENT '分账明细数量',
  `total_basis_points` int NOT NULL DEFAULT 0 COMMENT '总分账基点',
  `sign_payload` longtext DEFAULT NULL COMMENT '待签名内容',
  `signature` varchar(512) DEFAULT NULL COMMENT '签名结果',
  `contract_call_data` longtext DEFAULT NULL COMMENT '合约调用参数',
  `payment_tx_hash` varchar(128) DEFAULT NULL COMMENT '链上交易哈希',
  `block_number` bigint DEFAULT NULL COMMENT '区块高度',
  `planned_at` datetime(3) DEFAULT NULL COMMENT '生成计划时间',
  `submitted_at` datetime(3) DEFAULT NULL COMMENT '提交链上时间',
  `settled_at` datetime(3) DEFAULT NULL COMMENT '结算完成时间',
  `failed_at` datetime(3) DEFAULT NULL COMMENT '结算失败时间',
  `failure_reason` varchar(1024) DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_settlement_crypto_order_no` (`crypto_order_no`),
  UNIQUE KEY `uk_contract_settlement_payment_tx_hash` (`payment_tx_hash`),
  KEY `idx_contract_settlement_chain` (`chain`),
  KEY `idx_contract_settlement_status` (`status`),
  KEY `idx_contract_settlement_merchant_order_no` (`merchant_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合约分账主记录表';

CREATE TABLE `contract_settlement_split_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_record_id` bigint NOT NULL COMMENT '结算主记录 ID',
  `crypto_order_no` varchar(32) NOT NULL COMMENT '平台订单号',
  `role` varchar(64) NOT NULL COMMENT '分账角色',
  `receiver` varchar(128) NOT NULL COMMENT '分账收款地址',
  `basis_points` int NOT NULL COMMENT '分账基点',
  `amount` decimal(36,18) NOT NULL COMMENT '分账金额',
  `status` varchar(32) NOT NULL COMMENT '明细状态',
  `payment_tx_hash` varchar(128) DEFAULT NULL COMMENT '链上交易哈希',
  `block_number` bigint DEFAULT NULL COMMENT '区块高度',
  `executed_at` datetime(3) DEFAULT NULL COMMENT '执行时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_settlement_split_role` (`settlement_record_id`, `role`),
  KEY `idx_contract_settlement_split_order_no` (`crypto_order_no`),
  KEY `idx_contract_settlement_split_receiver` (`receiver`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合约分账明细表';

CREATE TABLE `chain_scanner_checkpoint` (
  `chain` varchar(64) NOT NULL COMMENT '链编码',
  `latest_observed_block` bigint NOT NULL DEFAULT 0 COMMENT '最新观察区块',
  `last_confirmed_block` bigint NOT NULL DEFAULT 0 COMMENT '已处理确认区块',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`chain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链扫描检查点表';

CREATE TABLE `derived_address_pool_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pool_key` varchar(256) NOT NULL COMMENT '地址池标识',
  `min_size` int NOT NULL DEFAULT 3 COMMENT '最小可用地址数量',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_derived_address_pool_policy_pool_key` (`pool_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派生地址池最小库存策略表';

CREATE TABLE `derived_address_pool_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pool_key` varchar(256) NOT NULL COMMENT '地址池标识',
  `chain` varchar(64) NOT NULL COMMENT '链编码',
  `token` varchar(64) NOT NULL COMMENT '币种标识',
  `address` varchar(128) NOT NULL COMMENT '收款地址',
  `source_mode` varchar(32) DEFAULT NULL COMMENT '地址来源模式',
  `source_reference` varchar(512) DEFAULT NULL COMMENT '地址生成来源引用(批次号/上游单号/导入标识)',
  `status` varchar(32) NOT NULL COMMENT '地址状态',
  `lease_id` varchar(64) DEFAULT NULL COMMENT '租约 ID',
  `lease_order_no` varchar(64) DEFAULT NULL COMMENT '当前实际占用订单号',
  `risk_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否风控禁用',
  `kyt_decision` varchar(32) DEFAULT NULL COMMENT 'KYT 决策',
  `kyt_provider` varchar(128) DEFAULT NULL COMMENT 'KYT 提供方',
  `kyt_risk_score` int DEFAULT NULL COMMENT 'KYT 风险分',
  `risk_reason` varchar(1024) DEFAULT NULL COMMENT '风控原因',
  `generated_at` datetime(3) DEFAULT NULL COMMENT '生成时间',
  `leased_at` datetime(3) DEFAULT NULL COMMENT '租用时间',
  `released_at` datetime(3) DEFAULT NULL COMMENT '释放时间',
  `last_used_at` datetime(3) DEFAULT NULL COMMENT '最近一次使用结束时间',
  `cooldown_until` datetime(3) DEFAULT NULL COMMENT '冷置到期时间，到期后才允许复用',
  `last_order_no` varchar(64) DEFAULT NULL COMMENT '最近一次占用订单号',
  `last_tx_hash` varchar(128) DEFAULT NULL COMMENT '最近一次入账交易哈希',
  `reuse_count` int NOT NULL DEFAULT 0 COMMENT '地址复用次数',
  `risk_marked_at` datetime(3) DEFAULT NULL COMMENT '风控标记时间',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_derived_address_pool_address` (`pool_key`, `address`),
  UNIQUE KEY `uk_derived_address_pool_lease` (`lease_id`),
  KEY `idx_derived_address_pool_status` (`status`),
  KEY `idx_derived_address_pool_cooldown` (`pool_key`, `status`, `cooldown_until`),
  KEY `idx_derived_address_pool_risk_flag` (`risk_flag`),
  KEY `idx_derived_address_pool_chain_token` (`chain`, `token`),
  KEY `idx_derived_address_pool_lease_order_no` (`lease_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派生地址池明细表';

CREATE TABLE `request_idempotency_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scope` varchar(64) NOT NULL COMMENT '幂等作用域',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `response_json` longtext DEFAULT NULL COMMENT '首次请求响应快照',
  `expires_at` datetime(3) DEFAULT NULL COMMENT '过期时间，过期后允许同一幂等键重新执行',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_idempotency_scope_key` (`scope`, `idempotency_key`),
  KEY `idx_request_idempotency_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等记录表';

CREATE TABLE `replay_transaction_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `chain` varchar(64) NOT NULL COMMENT '链编码',
    `tx_hash` varchar(128) NOT NULL COMMENT '交易哈希',
    `expires_at` datetime(3) NOT NULL COMMENT '过期时间',
    `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_replay_tx_chain_hash` (`chain`, `tx_hash`),
    KEY `idx_replay_tx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易重放防护表';

CREATE TABLE `payment_callback_delivery_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_type` varchar(64) NOT NULL COMMENT '回调事件类型',
  `chain` varchar(64) DEFAULT NULL COMMENT '链编码',
  `crypto_order_no` varchar(32) NOT NULL COMMENT '平台订单号',
  `merchant_order_no` varchar(64) NOT NULL COMMENT '商户订单号',
  `merchant_id` varchar(64) DEFAULT NULL COMMENT '商户号',
  `callback_url` varchar(512) NOT NULL COMMENT '回调地址',
  `callback_key` varchar(128) NOT NULL COMMENT '回调事件幂等键，同一业务事件只允许入队一次',
  `payload_json` longtext NOT NULL COMMENT '回调请求体',
  `request_headers_json` longtext DEFAULT NULL COMMENT '回调请求头',
  `status` varchar(32) NOT NULL COMMENT '投递状态',
  `attempt_count` int NOT NULL DEFAULT 0 COMMENT '已尝试次数',
  `max_attempts` int NOT NULL DEFAULT 6 COMMENT '最大尝试次数',
  `next_retry_at` datetime(3) DEFAULT NULL COMMENT '下次重试时间',
  `last_attempt_at` datetime(3) DEFAULT NULL COMMENT '上次尝试时间',
  `delivered_at` datetime(3) DEFAULT NULL COMMENT '成功投递时间',
  `dead_at` datetime(3) DEFAULT NULL COMMENT '进入死信时间',
  `last_http_status` int DEFAULT NULL COMMENT '最后 HTTP 状态码',
  `last_response_body` longtext DEFAULT NULL COMMENT '最后响应内容',
  `last_error` longtext DEFAULT NULL COMMENT '最后错误信息',
  `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_callback_delivery_callback_key` (`callback_key`),
  KEY `idx_callback_delivery_status_next_retry` (`status`, `next_retry_at`),
  KEY `idx_callback_delivery_merchant_status_retry` (`merchant_id`, `status`, `next_retry_at`),
  KEY `idx_callback_delivery_crypto_order_no` (`crypto_order_no`),
  KEY `idx_callback_delivery_merchant_order_no` (`merchant_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调投递记录表';

CREATE TABLE `payment_audit_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_type` varchar(64) NOT NULL COMMENT '审计事件类型',
    `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
    `biz_key` varchar(128) NOT NULL COMMENT '业务主键',
    `merchant_id` varchar(64) DEFAULT NULL COMMENT '商户号',
    `detail_schema_version` varchar(16) DEFAULT NULL COMMENT '明细结构版本',
    `status` varchar(32) NOT NULL COMMENT '事件状态',
    `detail_json` longtext DEFAULT NULL COMMENT '事件详情',
    `created_at` datetime(3) DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_payment_audit_event_type` (`event_type`),
  KEY `idx_payment_audit_biz_key` (`biz_type`, `biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计记录表';


