-- 创建收藏记录表
CREATE TABLE IF NOT EXISTS favorite_record (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL COMMENT '用户ID',
    target_type TINYINT NOT NULL COMMENT '目标类型：1-笔记，2-评论',
    target_id INT NOT NULL COMMENT '目标ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏记录表';
