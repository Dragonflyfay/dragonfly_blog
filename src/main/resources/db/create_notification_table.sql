-- 创建通知表
CREATE TABLE IF NOT EXISTS notification (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL COMMENT '接收通知的用户ID',
    from_user_id INT NOT NULL COMMENT '触发通知的用户ID',
    type TINYINT NOT NULL COMMENT '通知类型：1-点赞笔记 2-点赞评论 3-评论笔记 4-关注 5-系统通知',
    target_type TINYINT DEFAULT NULL COMMENT '目标类型：1-笔记 2-评论',
    target_id INT DEFAULT NULL COMMENT '目标ID（笔记ID或评论ID）',
    content VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user (user_id, is_read, create_time DESC),
    INDEX idx_from_user (from_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
