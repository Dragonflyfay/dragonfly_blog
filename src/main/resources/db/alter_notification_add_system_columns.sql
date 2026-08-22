-- 系统通知功能新增字段（title/priority/status/update_time）
-- 仅需执行一次；若列已存在请跳过或手动核对
ALTER TABLE notification

    ADD COLUMN priority VARCHAR(20) DEFAULT 'normal' COMMENT '优先级：normal/important/urgent' AFTER title,
    ADD COLUMN status VARCHAR(20) DEFAULT 'published' COMMENT '状态：draft/published' AFTER priority,
    ADD COLUMN update_time DATETIME DEFAULT NULL COMMENT '更新时间' AFTER create_time,
    ADD COLUMN batch_id INT DEFAULT NULL COMMENT '广播通知分组ID（同一广播所有用户行共享）' AFTER update_time;
