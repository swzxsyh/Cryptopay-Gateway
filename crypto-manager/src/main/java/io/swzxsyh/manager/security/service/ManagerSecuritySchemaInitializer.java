package io.swzxsyh.manager.security.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 初始化管理端安全相关数据库表。 */
@Component
@Order(0)
public class ManagerSecuritySchemaInitializer implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  public ManagerSecuritySchemaInitializer(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** 启动时确保管理员账号表存在，便于本地和测试环境直接切到 DB 模式。 */
  @Override
  public void run(ApplicationArguments args) {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `manager_admin_user` (
          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
          `username` varchar(64) NOT NULL COMMENT '登录名',
          `password_hash` varchar(120) NOT NULL COMMENT 'BCrypt 密码摘要',
          `display_name` varchar(64) DEFAULT NULL COMMENT '显示名称',
          `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
          `account_non_locked` tinyint(1) NOT NULL DEFAULT 1 COMMENT '账号是否未锁定',
          `failed_login_count` int NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
          `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
          `roles` varchar(255) NOT NULL DEFAULT '' COMMENT '兼容字段：角色编码，逗号分隔，不含 ROLE_ 前缀',
          `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
          `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_manager_admin_user_username` (`username`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端管理员账号';
        """);
    jdbcTemplate.execute(
        """
        ALTER TABLE `manager_admin_user`
          MODIFY COLUMN `roles` varchar(255) NOT NULL DEFAULT '' COMMENT '兼容字段：角色编码，逗号分隔，不含 ROLE_ 前缀'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `manager_role` (
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
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `manager_user_role` (
          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
          `user_id` bigint NOT NULL COMMENT '管理员账号ID',
          `role_code` varchar(64) NOT NULL COMMENT '角色编码',
          `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_manager_user_role` (`user_id`, `role_code`),
          KEY `idx_manager_user_role_code` (`role_code`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端用户角色关系表';
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `manager_role_function_permission` (
          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
          `role_code` varchar(64) NOT NULL COMMENT '角色编码',
          `permission_code` varchar(128) NOT NULL COMMENT '功能权限编码',
          `permission_name` varchar(128) DEFAULT NULL COMMENT '功能权限名称',
          `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
          `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
          `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_manager_role_function` (`role_code`, `permission_code`),
          KEY `idx_manager_function_permission_code` (`permission_code`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端角色功能权限表';
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `manager_role_data_permission` (
          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
          `role_code` varchar(64) NOT NULL COMMENT '角色编码',
          `scope_type` varchar(64) NOT NULL COMMENT '数据范围类型，如 ALL/MERCHANT/CHAIN',
          `scope_value` varchar(128) NOT NULL DEFAULT '*' COMMENT '数据范围值，ALL 使用 *',
          `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
          `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
          `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_manager_role_data_scope` (`role_code`, `scope_type`, `scope_value`),
          KEY `idx_manager_role_data_scope` (`scope_type`, `scope_value`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端角色数据权限表';
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `manager_user_data_permission` (
          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
          `user_id` bigint NOT NULL COMMENT '管理员账号ID',
          `scope_type` varchar(64) NOT NULL COMMENT '数据范围类型，如 ALL/MERCHANT/CHAIN',
          `scope_value` varchar(128) NOT NULL DEFAULT '*' COMMENT '数据范围值，MERCHANT 使用商户号',
          `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
          `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
          `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_manager_user_data_scope` (`user_id`, `scope_type`, `scope_value`),
          KEY `idx_manager_user_data_scope` (`scope_type`, `scope_value`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端用户数据权限表';
        """);
    jdbcTemplate.update(
        """
        INSERT INTO `manager_role`
          (`role_code`, `role_name`, `super_admin`, `enabled`, `created_at`, `updated_at`)
        VALUES
          ('ADMIN', '超级管理员', 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
          `role_name` = VALUES(`role_name`),
          `super_admin` = 1,
          `enabled` = 1,
          `updated_at` = NOW()
        """);
    jdbcTemplate.update(
        """
        INSERT INTO `manager_role_data_permission`
          (`role_code`, `scope_type`, `scope_value`, `enabled`, `created_at`, `updated_at`)
        VALUES
          ('ADMIN', 'ALL', '*', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
          `enabled` = 1,
          `updated_at` = NOW()
        """);
  }
}
