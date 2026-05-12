-- 创建库
create database if not exists `neko_ai_agent` default character set utf8mb4 collate utf8mb4_unicode_ci;

-- 切换库
use `neko_ai_agent`;


-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userEmail    varchar(256)                           null comment '用户邮箱',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 如果表已存在，增量添加 userEmail 字段
ALTER TABLE user ADD COLUMN IF NOT EXISTS userEmail varchar(256) null comment '用户邮箱' AFTER userPassword;

CREATE TABLE `ai_chat_memory` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `conversationId` varchar(64) NOT NULL COMMENT '会话唯一标识',
                                  `messageType` varchar(16) NOT NULL COMMENT '消息类型：SYSTEM/USER/ASSISTANT',
                                  `content` longtext NOT NULL COMMENT '消息内容',
                                  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `isDeleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标识：0-未删除，1-已删除',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_conversation_id_create_time` (`conversationId`,`createTime`),
                                  KEY `idx_conversation_id` (`conversationId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记忆表';

DROP TABLE IF EXISTS `love_chat_memory`;
DROP TABLE IF EXISTS `pet_chat_memory`;
DROP TABLE IF EXISTS `manus_chat_memory`;

-- Spring AI JDBC Chat Memory MySQL 表结构
-- 基于官方 SQL Server 脚本修改

CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY  (
                                                     conversation_id VARCHAR(36) NOT NULL COMMENT '对话ID',
                                                     content LONGTEXT NOT NULL COMMENT '消息内容',
                                                     type VARCHAR(10) NOT NULL COMMENT '消息类型 (USER/ASSISTANT/SYSTEM/TOOL)',
                                                     `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                                     CONSTRAINT chk_type CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Spring AI 对话记忆表';

-- 创建索引
CREATE INDEX SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
    ON SPRING_AI_CHAT_MEMORY(conversation_id, `timestamp` DESC);



-- 对话历史表
create table if not exists love_chat_history
(
    id         bigint auto_increment comment 'id' primary key,
    chatId     varchar(255)                       not null comment '对话id',
    userId     bigint                             not null comment '创建用户id',
    messages   text                               not null comment '对话记录（JSON格式存储）',
    lastMessage text                              null comment '最后一条消息内容（用于列表展示）',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    INDEX idx_user_chat (chatId, userId),  -- 新增索引，优化查询效率
    INDEX idx_user_time (userId, updateTime)  -- 按用户和时间查询的索引
) comment '上下文对话表' collate = utf8mb4_unicode_ci;

-- 对话历史表
create table if not exists pet_chat_history
(
    id         bigint auto_increment comment 'id' primary key,
    chatId     varchar(255)                       not null comment '对话id',
    userId     bigint                             not null comment '创建用户id',
    messages   text                               not null comment '对话记录（JSON格式存储）',
    lastMessage text                              null comment '最后一条消息内容（用于列表展示）',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    INDEX idx_user_chat (chatId, userId),  -- 新增索引，优化查询效率
    INDEX idx_user_time (userId, updateTime)  -- 按用户和时间查询的索引
) comment '上下文对话表' collate = utf8mb4_unicode_ci;

-- 对话历史表
create table if not exists manus_chat_history
(
    id         bigint auto_increment comment 'id' primary key,
    chatId     varchar(255)                       not null comment '对话id',
    userId     bigint                             not null comment '创建用户id',
    messages   text                               not null comment '对话记录（JSON格式存储）',
    lastMessage text                              null comment '最后一条消息内容（用于列表展示）',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    INDEX idx_user_chat (chatId, userId),  -- 新增索引，优化查询效率
    INDEX idx_user_time (userId, updateTime)  -- 按用户和时间查询的索引
) comment '上下文对话表' collate = utf8mb4_unicode_ci;