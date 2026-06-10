-- =============================================
-- 在线聊天系统 数据库初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS chat_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chat_system;

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT '/img/default-avatar.png' COMMENT '头像路径',
    `signature` VARCHAR(200) DEFAULT '' COMMENT '个性签名',
    `status` TINYINT DEFAULT 0 COMMENT '在线状态：0离线 1在线',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 好友分组表
DROP TABLE IF EXISTS `friend_group`;
CREATE TABLE `friend_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    `user_id` BIGINT NOT NULL COMMENT '分组所属用户ID',
    `group_name` VARCHAR(50) NOT NULL COMMENT '分组名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友分组表';

-- 好友关系表
DROP TABLE IF EXISTS `friend`;
CREATE TABLE `friend` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友用户ID',
    `group_id` BIGINT DEFAULT NULL COMMENT '好友分组ID',
    `remark` VARCHAR(50) DEFAULT '' COMMENT '好友备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- 好友申请表
DROP TABLE IF EXISTS `friend_request`;
CREATE TABLE `friend_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `from_user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `to_user_id` BIGINT NOT NULL COMMENT '被申请人ID',
    `message` VARCHAR(200) DEFAULT '' COMMENT '验证信息',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0待处理 1已同意 2已拒绝',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_to_user` (`to_user_id`, `status`),
    KEY `idx_from_user` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友申请表';

-- 私聊消息表
DROP TABLE IF EXISTS `private_message`;
CREATE TABLE `private_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `from_id` BIGINT NOT NULL COMMENT '发送者ID',
    `to_id` BIGINT NOT NULL COMMENT '接收者ID',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `msg_type` TINYINT DEFAULT 0 COMMENT '消息类型：0文字 1语音 2图片 3文件',
    `voice_url` VARCHAR(255) DEFAULT NULL COMMENT '附件路径（语音/图片/文件）',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_from_to` (`from_id`, `to_id`),
    KEY `idx_to_read` (`to_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息表';

-- 群组表
DROP TABLE IF EXISTS `chat_group`;
CREATE TABLE `chat_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群组ID',
    `group_name` VARCHAR(50) NOT NULL COMMENT '群名称',
    `avatar` VARCHAR(255) DEFAULT '/img/default-group.png' COMMENT '群头像',
    `owner_id` BIGINT NOT NULL COMMENT '群主ID',
    `description` VARCHAR(200) DEFAULT '' COMMENT '群简介',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

-- 群组成员表
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` TINYINT DEFAULT 0 COMMENT '角色：0普通成员 1管理员 2群主',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组成员表';

-- 群邀请表
DROP TABLE IF EXISTS `group_invite`;
CREATE TABLE `group_invite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '邀请ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `from_user_id` BIGINT NOT NULL COMMENT '邀请人ID（群主/管理员）',
    `to_user_id` BIGINT NOT NULL COMMENT '被邀请人ID',
    `message` VARCHAR(200) DEFAULT '' COMMENT '邀请信息',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0待处理 1已同意 2已拒绝',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_to_user` (`to_user_id`, `status`),
    KEY `idx_group` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群邀请表';

-- 群聊消息表
DROP TABLE IF EXISTS `group_message`;
CREATE TABLE `group_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `from_id` BIGINT NOT NULL COMMENT '发送者ID',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `msg_type` TINYINT DEFAULT 0 COMMENT '消息类型：0文字 1语音 2图片 3文件',
    `voice_url` VARCHAR(255) DEFAULT NULL COMMENT '附件路径（语音/图片/文件）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊消息表';

-- =============================================
-- 插入测试数据
-- =============================================

-- 测试用户（密码均为 123456 的BCrypt值）
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `signature`, `status`) VALUES
(1, 'zhangsan', '$2a$10$XnRC.axYiKMIz7SEuPJkWuWllyZlXyNGf2Gk/mPIcPMYBw5vqd.PW', '张三', '/img/default-avatar.png', '今天天气真好~', 0),
(2, 'lisi', '$2a$10$XnRC.axYiKMIz7SEuPJkWuWllyZlXyNGf2Gk/mPIcPMYBw5vqd.PW', '李四', '/img/default-avatar.png', '学习使我快乐', 0),
(3, 'wangwu', '$2a$10$XnRC.axYiKMIz7SEuPJkWuWllyZlXyNGf2Gk/mPIcPMYBw5vqd.PW', '王五', '/img/default-avatar.png', '加油加油！', 0);

-- 默认好友分组
INSERT INTO `friend_group` (`id`, `user_id`, `group_name`, `sort_order`) VALUES
(1, 1, '我的好友', 1),
(2, 1, '同学', 2),
(3, 2, '我的好友', 1),
(4, 3, '我的好友', 1);

-- 测试好友关系
INSERT INTO `friend` (`user_id`, `friend_id`, `group_id`, `remark`) VALUES
(1, 2, 1, '小李'),
(2, 1, 3, '老张');

-- 测试群组
INSERT INTO `chat_group` (`id`, `group_name`, `owner_id`, `description`) VALUES
(1, '学习交流群', 1, '一起学习Web开发');

-- 测试群成员
INSERT INTO `group_member` (`group_id`, `user_id`, `role`) VALUES
(1, 1, 2),
(1, 2, 0),
(1, 3, 0);

-- 测试私聊消息
INSERT INTO `private_message` (`from_id`, `to_id`, `content`, `msg_type`, `is_read`) VALUES
(1, 2, '你好，李四！', 0, 1),
(2, 1, '你好，张三！最近怎么样？', 0, 1),
(1, 2, '挺好的，在学Web开发呢', 0, 1);

-- 测试群聊消息
INSERT INTO `group_message` (`group_id`, `from_id`, `content`, `msg_type`) VALUES
(1, 1, '大家好，欢迎加入学习交流群！', 0),
(1, 2, '谢谢群主！', 0),
(1, 3, '一起加油！', 0);
