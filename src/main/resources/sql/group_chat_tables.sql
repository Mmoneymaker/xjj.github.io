-- 群聊表
CREATE TABLE IF NOT EXISTS tb_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL COMMENT '群聊名称',
    group_desc VARCHAR(500) COMMENT '群聊描述',
    owner_id VARCHAR(50) NOT NULL COMMENT '群主用户名',
    avatar_url VARCHAR(500) COMMENT '群头像URL',
    max_members INT DEFAULT 200 COMMENT '最大成员数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status TINYINT DEFAULT 1 COMMENT '群状态：1-正常，0-解散',
    INDEX idx_owner_id (owner_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊表';

-- 群成员表
CREATE TABLE IF NOT EXISTS tb_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '群聊ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    nickname VARCHAR(100) COMMENT '群内昵称',
    role TINYINT DEFAULT 0 COMMENT '角色：0-普通成员，1-管理员，2-群主',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    mute_until DATETIME COMMENT '禁言到期时间，NULL表示未禁言',
    last_read_time DATETIME COMMENT '最后阅读时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-已退出',
    UNIQUE KEY uk_group_user (group_id, username),
    INDEX idx_group_id (group_id),
    INDEX idx_username (username),
    FOREIGN KEY (group_id) REFERENCES tb_group(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员表';

-- 群聊消息表（可以复用现有的chat_message表，但需要添加group_id字段）
ALTER TABLE chat_message ADD COLUMN group_id BIGINT NULL COMMENT '群聊ID，群聊消息时使用';
ALTER TABLE chat_message ADD INDEX idx_group_id (group_id);
ALTER TABLE chat_message ADD INDEX idx_create_time (create_time);