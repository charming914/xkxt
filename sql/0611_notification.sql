-- 0611 站内通知 / 公告
-- notification：每个用户的通知收件箱（审批结果、公告投递等）
-- announcement：管理员发布的公告主记录（发布历史 + 投递范围）

CREATE TABLE IF NOT EXISTS announcement (
    announcement_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告编号',
    title VARCHAR(100) NOT NULL COMMENT '公告标题',
    content VARCHAR(1000) NOT NULL COMMENT '公告内容',
    target_scope VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '投递范围：ALL全体，STUDENT全体学生，TEACHER全体教师，DEPARTMENT指定院系',
    target_department_id VARCHAR(10) NULL COMMENT '指定院系编号（target_scope=DEPARTMENT时使用）',
    recipient_count INT NOT NULL DEFAULT 0 COMMENT '投递人数',
    published_by INT NULL COMMENT '发布管理员用户编号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (announcement_id),
    KEY idx_announcement_created (created_at),
    CONSTRAINT fk_announcement_user FOREIGN KEY (published_by) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

CREATE TABLE IF NOT EXISTS notification (
    notification_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知编号',
    user_id INT NOT NULL COMMENT '接收用户编号',
    category VARCHAR(20) NOT NULL DEFAULT 'APPROVAL' COMMENT '通知类别：APPROVAL审批结果，ANNOUNCEMENT公告，SYSTEM系统',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content VARCHAR(1000) NOT NULL COMMENT '通知内容',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
    ref_type VARCHAR(30) NULL COMMENT '关联业务类型，如补选、退课、公告',
    ref_id VARCHAR(30) NULL COMMENT '关联业务编号',
    announcement_id BIGINT NULL COMMENT '关联公告编号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_at DATETIME NULL COMMENT '已读时间',
    PRIMARY KEY (notification_id),
    KEY idx_notification_user (user_id, is_read),
    KEY idx_notification_created (created_at),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';
