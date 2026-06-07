-- 0607 教师端增强功能数据库迁移脚本
-- 功能：成绩提交时间窗口、教师调课申请

SET @add_score_submit_start = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE semester ADD COLUMN score_submit_start DATETIME NULL COMMENT ''成绩提交开始时间''',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'semester'
      AND column_name = 'score_submit_start'
);
PREPARE stmt FROM @add_score_submit_start;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_score_submit_end = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE semester ADD COLUMN score_submit_end DATETIME NULL COMMENT ''成绩提交结束时间''',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'semester'
      AND column_name = 'score_submit_end'
);
PREPARE stmt FROM @add_score_submit_end;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE semester
SET score_submit_start = CONCAT(end_date, ' 08:00:00'),
    score_submit_end = DATE_ADD(CONCAT(end_date, ' 23:59:59'), INTERVAL 10 DAY)
WHERE score_submit_start IS NULL
  AND score_submit_end IS NULL;

CREATE TABLE IF NOT EXISTS schedule_adjust_request (
    adjust_request_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '调课申请编号',
    teaching_class_id INT NOT NULL COMMENT '教学班编号',
    teacher_id VARCHAR(12) NOT NULL COMMENT '申请教师工号',
    original_week_day TINYINT NOT NULL COMMENT '原星期',
    original_section_desc VARCHAR(20) NOT NULL COMMENT '原节次',
    original_week_range VARCHAR(30) NOT NULL COMMENT '原周次',
    original_classroom_id VARCHAR(10) NULL COMMENT '原教室',
    requested_week_day TINYINT NOT NULL COMMENT '申请后星期',
    requested_section_desc VARCHAR(20) NOT NULL COMMENT '申请后节次',
    requested_week_range VARCHAR(30) NOT NULL COMMENT '申请后周次',
    requested_classroom_id VARCHAR(10) NULL COMMENT '申请后教室',
    request_reason VARCHAR(200) NOT NULL COMMENT '申请原因',
    request_status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '申请状态',
    review_comment VARCHAR(200) NULL COMMENT '审核备注',
    reviewed_by INT NULL COMMENT '审核管理员用户编号',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (adjust_request_id),
    CONSTRAINT fk_schedule_adjust_teaching_class
        FOREIGN KEY (teaching_class_id) REFERENCES teaching_class(teaching_class_id),
    CONSTRAINT fk_schedule_adjust_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id),
    CONSTRAINT fk_schedule_adjust_original_classroom
        FOREIGN KEY (original_classroom_id) REFERENCES classroom(classroom_id),
    CONSTRAINT fk_schedule_adjust_requested_classroom
        FOREIGN KEY (requested_classroom_id) REFERENCES classroom(classroom_id),
    CONSTRAINT fk_schedule_adjust_admin
        FOREIGN KEY (reviewed_by) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调课申请表';

DELETE sc
FROM score sc
JOIN course_selection cs
  ON sc.student_id = cs.student_id
 AND sc.teaching_class_id = cs.teaching_class_id
WHERE cs.selection_status = '已退';

DELETE sc
FROM score sc
JOIN course_selection cs
  ON sc.student_id = cs.student_id
 AND sc.teaching_class_id = cs.teaching_class_id
WHERE cs.selection_status = '已选'
  AND sc.entered_at IS NOT NULL
  AND cs.selection_time IS NOT NULL
  AND sc.entered_at < cs.selection_time;
