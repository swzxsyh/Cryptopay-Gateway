CREATE TABLE IF NOT EXISTS `manager_admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) NOT NULL COMMENT '登录名',
  `password_hash` varchar(120) NOT NULL COMMENT 'BCrypt 密码摘要',
  `display_name` varchar(64) DEFAULT NULL COMMENT '显示名称',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `account_non_locked` tinyint(1) NOT NULL DEFAULT 1 COMMENT '账号是否未锁定',
  `failed_login_count` int NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `roles` varchar(255) NOT NULL DEFAULT 'ADMIN' COMMENT '角色，逗号分隔，不含 ROLE_ 前缀',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_admin_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端管理员账号';
