-- 0607 成绩修改申请迁移脚本
-- 功能：教师对已提交成绩发起修改申请，管理员审核通过后退回成绩。

CREATE TABLE IF NOT EXISTS score_change_request (
    score_change_request_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '成绩修改申请编号',
    score_id BIGINT NOT NULL COMMENT '成绩编号',
    teacher_id VARCHAR(12) NOT NULL COMMENT '申请教师工号',
    student_id VARCHAR(12) NOT NULL COMMENT '学号',
    teaching_class_id INT NOT NULL COMMENT '教学班编号',
    original_usual_score DECIMAL(5,2) NULL COMMENT '原平时成绩',
    original_exam_score DECIMAL(5,2) NULL COMMENT '原考试成绩',
    original_final_score DECIMAL(5,2) NULL COMMENT '原总评成绩',
    original_gpa_point DECIMAL(3,1) NULL COMMENT '原绩点',
    request_reason VARCHAR(200) NOT NULL COMMENT '申请原因',
    request_status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '申请状态',
    review_comment VARCHAR(200) NULL COMMENT '审核备注',
    reviewed_by INT NULL COMMENT '审核管理员用户编号',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (score_change_request_id),
    CONSTRAINT fk_score_change_score
        FOREIGN KEY (score_id) REFERENCES score(score_id),
    CONSTRAINT fk_score_change_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id),
    CONSTRAINT fk_score_change_student
        FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_score_change_teaching_class
        FOREIGN KEY (teaching_class_id) REFERENCES teaching_class(teaching_class_id),
    CONSTRAINT fk_score_change_admin
        FOREIGN KEY (reviewed_by) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩修改申请表';
