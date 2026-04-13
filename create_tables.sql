-- MySQL 8.0.45 对话记忆存储表结构设计
-- 企业级设计，支持多层存储（内存 + 数据库）
-- 注意：此脚本仅创建表结构，不包含数据迁移

-- 如果数据库不存在则创建（根据需要修改数据库名）
-- CREATE DATABASE IF NOT EXISTS `ai_agent` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE `ai_agent`;

-- 1. 会话表：存储对话会话的元数据
DROP TABLE IF EXISTS `ai_conversation_session`;
CREATE TABLE `ai_conversation_session`  (
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID，格式：session-{uuid}',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '新对话' COMMENT '会话标题，自动生成或用户指定',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间，精确到毫秒',
  `last_active_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后活跃时间，自动更新',
  `message_count` int(0) UNSIGNED NOT NULL DEFAULT 0 COMMENT '消息总数',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '会话状态：1-活跃，2-归档，3-删除（逻辑删除），4-隐藏',
  `storage_level` tinyint(0) NOT NULL DEFAULT 1 COMMENT '存储层级：1-内存（近期活跃），2-数据库（完整存储），3-归档（冷存储）',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户ID，预留用于多用户系统',
  `model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用的AI模型',
  `temperature` decimal(3, 2) NULL DEFAULT NULL COMMENT '温度参数',
  `max_tokens` int(0) NULL DEFAULT NULL COMMENT '最大token数',
  `custom_attributes` json NULL COMMENT '自定义扩展属性，JSON格式',
  `metadata` json NULL COMMENT '其他元数据，JSON格式',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已删除（逻辑删除）',
  PRIMARY KEY (`session_id`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE,
  INDEX `idx_last_active_at`(`last_active_at`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_storage_level`(`storage_level`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_message_count`(`message_count`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI对话会话表' ROW_FORMAT = Dynamic;

-- 2. 消息表：存储每条对话消息
DROP TABLE IF EXISTS `ai_conversation_message`;
CREATE TABLE `ai_conversation_message`  (
  `message_id` bigint(0) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID，自增主键',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID，外键关联ai_conversation_session.session_id',
  `message_index` int(0) UNSIGNED NOT NULL COMMENT '在会话中的序号（从1开始）',
  `role` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '消息角色',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `tokens` int(0) UNSIGNED NULL DEFAULT NULL COMMENT '消息token数估计',
  `model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '生成消息的模型（针对assistant消息）',
  `temperature` decimal(3, 2) NULL DEFAULT NULL COMMENT '生成时的温度参数',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '消息创建时间',
  `metadata` json NULL COMMENT '消息元数据，JSON格式',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已删除（逻辑删除）',
  PRIMARY KEY (`message_id`) USING BTREE,
  UNIQUE INDEX `uk_session_message_index`(`session_id`, `message_index`) USING BTREE,
  INDEX `idx_session_id`(`session_id`) USING BTREE,
  INDEX `idx_session_role`(`session_id`, `role`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE,
  INDEX `idx_role`(`role`) USING BTREE,
  INDEX `idx_tokens`(`tokens`) USING BTREE,
  CONSTRAINT `fk_message_session` FOREIGN KEY (`session_id`) REFERENCES `ai_conversation_session` (`session_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI对话消息表' ROW_FORMAT = Dynamic;


-- 3. 记忆摘要表：存储会话的记忆摘要，用于长期记忆
DROP TABLE IF EXISTS `ai_memory_summary`;
CREATE TABLE `ai_memory_summary`  (
  `memory_id` bigint(0) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记忆ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联的会话ID',
  `summary_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '摘要类型：conversation_summary, entity_memory, user_preference等',
  `summary_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '摘要内容',
  `embedding_vector` json NULL COMMENT '向量嵌入，JSON数组格式，用于相似性搜索',
  `importance_score` decimal(3, 2) NULL DEFAULT 0.50 COMMENT '重要性评分，0-1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `expires_at` datetime(3) NULL DEFAULT NULL COMMENT '过期时间（如有）',
  `metadata` json NULL COMMENT '元数据',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已删除（逻辑删除）',
  PRIMARY KEY (`memory_id`) USING BTREE,
  INDEX `idx_session_id`(`session_id`) USING BTREE,
  INDEX `idx_summary_type`(`summary_type`) USING BTREE,
  INDEX `idx_importance_score`(`importance_score`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE,
  INDEX `idx_expires_at`(`expires_at`) USING BTREE,
  CONSTRAINT `fk_memory_session` FOREIGN KEY (`session_id`) REFERENCES `ai_conversation_session` (`session_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI记忆摘要表' ROW_FORMAT = Dynamic;

-- 4. 文档向量表：存储RAG文档的向量嵌入（如果使用数据库存储向量）
CREATE TABLE IF NOT EXISTS `ai_document_vector` (
    `document_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '关联的会话ID（如果与会话相关）',
    `document_type` VARCHAR(50) NOT NULL COMMENT '文档类型：text, pdf, url等',
    `document_content` LONGTEXT NOT NULL COMMENT '文档原始内容',
    `chunk_index` INT UNSIGNED DEFAULT 0 COMMENT '分块索引（如果文档被分块）',
    `chunk_content` TEXT NOT NULL COMMENT '分块内容',
    `embedding_vector` JSON NOT NULL COMMENT '向量嵌入，JSON数组格式',
    `metadata` JSON DEFAULT NULL COMMENT '文档元数据（来源、作者、创建时间等）',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否活跃',
    PRIMARY KEY (`document_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_document_type` (`document_type`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI文档向量表（RAG）';

-- 5. 技能执行记录表：存储技能调用历史
CREATE TABLE IF NOT EXISTS `ai_skill_execution` (
    `execution_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '执行记录ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '关联的会话ID',
    `skill_name` VARCHAR(100) NOT NULL COMMENT '技能名称',
    `skill_input` TEXT NOT NULL COMMENT '技能输入',
    `skill_output` TEXT NOT NULL COMMENT '技能输出',
    `execution_time_ms` INT UNSIGNED DEFAULT NULL COMMENT '执行耗时（毫秒）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '执行状态：1-成功，2-失败，3-部分成功',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息（如果失败）',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '执行时间',
    `metadata` JSON DEFAULT NULL COMMENT '执行元数据',
    PRIMARY KEY (`execution_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_skill_name` (`skill_name`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_status` (`status`),
    CONSTRAINT `fk_skill_session` FOREIGN KEY (`session_id`)
        REFERENCES `ai_conversation_session` (`session_id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI技能执行记录表';

-- 6. 操作审计表：记录关键操作日志
CREATE TABLE IF NOT EXISTS `ai_operation_audit` (
    `audit_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审计ID',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型：create_session, delete_session, update_title, upload_document等',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '关联的会话ID',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '操作用户ID',
    `operation_details` JSON NOT NULL COMMENT '操作详情，JSON格式',
    `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '操作IP地址',
    `user_agent` TEXT DEFAULT NULL COMMENT '用户代理',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
    PRIMARY KEY (`audit_id`),
    INDEX `idx_operation_type` (`operation_type`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI操作审计表';

-- 7. 统计表：用于快速查询的聚合统计（可选，可根据需求定期更新）
CREATE TABLE IF NOT EXISTS `ai_statistics_daily` (
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_sessions` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总会话数',
    `active_sessions` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '活跃会话数',
    `new_sessions` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '新增会话数',
    `total_messages` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总消息数',
    `user_messages` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户消息数',
    `assistant_messages` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '助理消息数',
    `avg_message_length` DECIMAL(8,2) DEFAULT 0 COMMENT '平均消息长度',
    `total_tokens` BIGINT UNSIGNED DEFAULT 0 COMMENT '总token数',
    `skill_executions` INT UNSIGNED DEFAULT 0 COMMENT '技能执行次数',
    `document_uploads` INT UNSIGNED DEFAULT 0 COMMENT '文档上传次数',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI每日统计表';

-- 8. 系统配置表：存储系统配置参数
CREATE TABLE IF NOT EXISTS `ai_system_config` (
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT NOT NULL COMMENT '配置值',
    `config_type` VARCHAR(50) NOT NULL DEFAULT 'string' COMMENT '配置类型：string, number, boolean, json',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最后更新人',
    PRIMARY KEY (`config_key`),
    INDEX `idx_config_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI系统配置表';

-- 初始化一些系统配置
INSERT INTO `ai_system_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('memory.max_sessions_in_memory', '100', 'number', '内存中最大会话数'),
('memory.max_messages_per_session', '20', 'number', '每个会话在内存中的最大消息数'),
('memory.sync_to_db_interval_seconds', '60', 'number', '内存同步到数据库的间隔秒数'),
('memory.cleanup_interval_hours', '24', 'number', '内存清理间隔小时数'),
('rag.enable_database_vector_store', 'false', 'boolean', '是否启用数据库向量存储'),
('rag.similarity_threshold', '0.7', 'number', 'RAG相似度阈值'),
('session.default_title_length', '30', 'number', '默认标题长度（字符）'),
('session.auto_archive_days', '30', 'number', '自动归档天数（不活跃会话）'),
('session.logical_delete_retention_days', '7', 'number', '逻辑删除保留天数'),
('statistics.update_interval_minutes', '5', 'number', '统计信息更新间隔分钟数')
ON DUPLICATE KEY UPDATE
    `config_value` = VALUES(`config_value`),
    `config_type` = VALUES(`config_type`),
    `description` = VALUES(`description`),
    `updated_at` = CURRENT_TIMESTAMP(3);

-- 创建存储过程：清理过期会话（逻辑删除超过保留期的会话）
DELIMITER $$
CREATE PROCEDURE `sp_cleanup_expired_sessions`()
BEGIN
    DECLARE retention_days INT;

    -- 获取配置的保留天数
    SELECT CAST(config_value AS UNSIGNED) INTO retention_days
    FROM ai_system_config
    WHERE config_key = 'session.logical_delete_retention_days';

    IF retention_days IS NULL THEN
        SET retention_days = 7; -- 默认7天
    END IF;

    -- 物理删除逻辑删除状态且超过保留期的会话
    DELETE FROM ai_conversation_session
    WHERE status = 3 -- 逻辑删除状态
      AND last_active_at < DATE_SUB(NOW(), INTERVAL retention_days DAY);

    SELECT ROW_COUNT() AS deleted_sessions;
END$$
DELIMITER ;

-- 创建存储过程：归档不活跃会话
DELIMITER $$
CREATE PROCEDURE `sp_archive_inactive_sessions`()
BEGIN
    DECLARE archive_days INT;

    -- 获取配置的归档天数
    SELECT CAST(config_value AS UNSIGNED) INTO archive_days
    FROM ai_system_config
    WHERE config_key = 'session.auto_archive_days';

    IF archive_days IS NULL THEN
        SET archive_days = 30; -- 默认30天
    END IF;

    -- 将不活跃的活跃会话标记为归档
    UPDATE ai_conversation_session
    SET status = 2, -- 归档状态
        storage_level = 3, -- 冷存储层级
        last_active_at = CURRENT_TIMESTAMP(3)
    WHERE status = 1 -- 活跃状态
      AND storage_level IN (1, 2) -- 内存或数据库存储
      AND last_active_at < DATE_SUB(NOW(), INTERVAL archive_days DAY);

    SELECT ROW_COUNT() AS archived_sessions;
END$$
DELIMITER ;

-- 创建视图：活跃会话视图（简化查询）
CREATE OR REPLACE VIEW `v_active_sessions` AS
SELECT
    session_id,
    title,
    created_at,
    last_active_at,
    message_count,
    model,
    user_id
FROM ai_conversation_session
WHERE status = 1 -- 活跃会话
  AND storage_level IN (1, 2) -- 内存或数据库存储
ORDER BY last_active_at DESC;

-- 创建视图：会话消息统计视图
CREATE OR REPLACE VIEW `v_session_message_stats` AS
SELECT
    s.session_id,
    s.title,
    s.message_count AS total_messages,
    COUNT(DISTINCT m.message_id) AS stored_messages,
    SUM(CASE WHEN m.role = 'user' THEN 1 ELSE 0 END) AS user_messages,
    SUM(CASE WHEN m.role = 'assistant' THEN 1 ELSE 0 END) AS assistant_messages,
    SUM(CASE WHEN m.role = 'system' THEN 1 ELSE 0 END) AS system_messages,
    AVG(LENGTH(m.content)) AS avg_message_length,
    MAX(m.created_at) AS last_message_time
FROM ai_conversation_session s
LEFT JOIN ai_conversation_message m ON s.session_id = m.session_id AND m.is_deleted = 0
WHERE s.status IN (1, 2) -- 活跃或归档会话
GROUP BY s.session_id, s.title, s.message_count;

-- 注释说明
/*
表设计说明：

1. 核心表：
   - ai_conversation_session: 会话元数据表，核心业务表
   - ai_conversation_message: 消息存储表，与会话一对多关系

2. 扩展表：
   - ai_memory_summary: 记忆摘要表，用于长期记忆存储
   - ai_document_vector: 文档向量表，支持RAG的数据库存储
   - ai_skill_execution: 技能执行记录，用于审计和分析
   - ai_operation_audit: 操作审计表，记录关键操作
   - ai_statistics_daily: 统计表，便于快速查询
   - ai_system_config: 系统配置表，灵活配置系统参数

3. 设计特点：
   - 支持多层存储：通过storage_level字段区分内存、数据库、归档存储
   - 逻辑删除：通过status字段控制，避免物理删除
   - JSON字段：用于灵活存储扩展属性
   - 外键约束：保证数据完整性
   - 索引优化：针对常用查询场景创建索引
   - 审计追踪：记录关键操作便于追溯

4. 与现有系统集成：
   - session_id格式兼容现有系统（session-{uuid}）
   - 现有内存存储可作为一级缓存（storage_level=1）
   - 消息表存储完整历史，内存只保留最近消息

5. 性能考虑：
   - 消息表使用自增主键，提高插入性能
   - 会话表使用session_id作为主键，便于直接查询
   - 大文本字段使用LONGTEXT类型
   - 定期归档和清理机制，避免表过大

使用建议：
1. 首先创建核心表（ai_conversation_session, ai_conversation_message）
2. 根据需求逐步创建扩展表
3. 运行初始化配置插入
4. 定期执行存储过程进行数据维护
*/

-- RAG v2 schema additions
CREATE TABLE IF NOT EXISTS `ai_document` (
    `knowledge_document_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文档实体ID',
    `external_document_id` VARCHAR(128) DEFAULT NULL COMMENT '业务侧文档ID',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '关联会话ID，为空表示全局知识',
    `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
    `source_type` VARCHAR(50) NOT NULL COMMENT '文档来源类型，如text、pdf、url',
    `source_uri` VARCHAR(512) DEFAULT NULL COMMENT '文档来源地址',
    `content_hash` CHAR(64) NOT NULL COMMENT '文档内容哈希',
    `parser_version` VARCHAR(32) NOT NULL DEFAULT 'text-v1' COMMENT '文档解析版本',
    `chunk_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文档分块数量',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '入库状态',
    `metadata` JSON DEFAULT NULL COMMENT '文档元数据',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    PRIMARY KEY (`knowledge_document_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_content_hash` (`content_hash`),
    INDEX `idx_status` (`status`),
    INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI文档主表';

SET @has_knowledge_document_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_document_vector'
      AND COLUMN_NAME = 'knowledge_document_id'
);
SET @add_knowledge_document_id_sql := IF(
    @has_knowledge_document_id = 0,
    'ALTER TABLE `ai_document_vector` ADD COLUMN `knowledge_document_id` BIGINT UNSIGNED NULL COMMENT ''关联文档实体ID'' AFTER `document_id`',
    'SELECT 1'
);
PREPARE stmt_add_knowledge_document_id FROM @add_knowledge_document_id_sql;
EXECUTE stmt_add_knowledge_document_id;
DEALLOCATE PREPARE stmt_add_knowledge_document_id;

SET @has_idx_knowledge_document_id := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_document_vector'
      AND INDEX_NAME = 'idx_knowledge_document_id'
);
SET @add_idx_knowledge_document_id_sql := IF(
    @has_idx_knowledge_document_id = 0,
    'ALTER TABLE `ai_document_vector` ADD INDEX `idx_knowledge_document_id` (`knowledge_document_id`)',
    'SELECT 1'
);
PREPARE stmt_add_idx_knowledge_document_id FROM @add_idx_knowledge_document_id_sql;
EXECUTE stmt_add_idx_knowledge_document_id;
DEALLOCATE PREPARE stmt_add_idx_knowledge_document_id;
