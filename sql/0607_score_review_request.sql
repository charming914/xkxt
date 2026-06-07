-- 0607 成绩复核申请数据库脚本
-- 功能：学生提交成绩复核申请，管理员审批并记录审核结果

CREATE TABLE IF NOT EXISTS score_review_request (
    request_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '成绩复核申请编号',
    student_id VARCHAR(12) NOT NULL COMMENT '学号',
    score_id BIGINT NOT NULL COMMENT '成绩编号',
    request_reason VARCHAR(200) NOT NULL COMMENT '复核原因',
    request_status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '申请状态',
    review_comment VARCHAR(200) NULL COMMENT '审核备注',
    reviewed_by INT NULL COMMENT '审核管理员用户编号',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (request_id),
    KEY idx_score_review_student (student_id),
    KEY idx_score_review_score (score_id),
    KEY idx_score_review_status (request_status),
    CONSTRAINT fk_score_review_student
        FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_score_review_score
        FOREIGN KEY (score_id) REFERENCES score(score_id),
    CONSTRAINT fk_score_review_admin
        FOREIGN KEY (reviewed_by) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩复核申请表';
