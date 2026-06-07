-- 0608 考试管理功能数据库迁移脚本
-- 功能：考试安排表、考试修改申请表
-- 说明：考试周为学期结束后的两周，每场考试由系统自动排定时间地点，
--       教师可提出修改申请（须管理员审核），管理员可直接更改。

CREATE TABLE IF NOT EXISTS exam_arrangement (
    exam_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '考试安排编号',
    teaching_class_id INT NOT NULL COMMENT '教学班编号',
    exam_date DATE NOT NULL COMMENT '考试日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    classroom_id VARCHAR(10) NULL COMMENT '考试教室',
    exam_status VARCHAR(20) NOT NULL DEFAULT '已排定' COMMENT '考试状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (exam_id),
    UNIQUE KEY uk_exam_teaching_class (teaching_class_id),
    CONSTRAINT fk_exam_teaching_class
        FOREIGN KEY (teaching_class_id) REFERENCES teaching_class(teaching_class_id),
    CONSTRAINT fk_exam_classroom
        FOREIGN KEY (classroom_id) REFERENCES classroom(classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试安排表';

CREATE TABLE IF NOT EXISTS exam_change_request (
    exam_change_request_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '考试修改申请编号',
    exam_id BIGINT NOT NULL COMMENT '考试安排编号',
    teaching_class_id INT NOT NULL COMMENT '教学班编号',
    teacher_id VARCHAR(12) NOT NULL COMMENT '申请教师工号',
    original_exam_date DATE NOT NULL COMMENT '原考试日期',
    original_start_time TIME NOT NULL COMMENT '原开始时间',
    original_end_time TIME NOT NULL COMMENT '原结束时间',
    original_classroom_id VARCHAR(10) NULL COMMENT '原考试教室',
    requested_exam_date DATE NOT NULL COMMENT '申请后考试日期',
    requested_start_time TIME NOT NULL COMMENT '申请后开始时间',
    requested_end_time TIME NOT NULL COMMENT '申请后结束时间',
    requested_classroom_id VARCHAR(10) NULL COMMENT '申请后考试教室',
    request_reason VARCHAR(200) NOT NULL COMMENT '申请原因',
    request_status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '申请状态',
    review_comment VARCHAR(200) NULL COMMENT '审核备注',
    reviewed_by INT NULL COMMENT '审核管理员用户编号',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (exam_change_request_id),
    KEY idx_exam_change_exam (exam_id),
    KEY idx_exam_change_teacher (teacher_id),
    KEY idx_exam_change_status (request_status),
    CONSTRAINT fk_exam_change_exam
        FOREIGN KEY (exam_id) REFERENCES exam_arrangement(exam_id),
    CONSTRAINT fk_exam_change_teaching_class
        FOREIGN KEY (teaching_class_id) REFERENCES teaching_class(teaching_class_id),
    CONSTRAINT fk_exam_change_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id),
    CONSTRAINT fk_exam_change_original_classroom
        FOREIGN KEY (original_classroom_id) REFERENCES classroom(classroom_id),
    CONSTRAINT fk_exam_change_requested_classroom
        FOREIGN KEY (requested_classroom_id) REFERENCES classroom(classroom_id),
    CONSTRAINT fk_exam_change_admin
        FOREIGN KEY (reviewed_by) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试修改申请表';
