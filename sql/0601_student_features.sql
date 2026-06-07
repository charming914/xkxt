-- 0601 学生端新增功能数据库脚本
-- 功能：退课开课日期、退课申请表、退课触发器

SET @add_course_start_date = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE teaching_class ADD COLUMN course_start_date DATE NULL COMMENT ''课程实际开课日期''',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'teaching_class'
      AND column_name = 'course_start_date'
);
PREPARE stmt FROM @add_course_start_date;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

UPDATE teaching_class tc
JOIN semester sem ON tc.semester_id = sem.semester_id
SET tc.course_start_date = sem.start_date
WHERE tc.course_start_date IS NULL;

CREATE TABLE course_drop_request (
    drop_request_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '退课申请编号',
    student_id VARCHAR(12) NOT NULL COMMENT '学号',
    teaching_class_id INT NOT NULL COMMENT '教学班编号',
    request_reason VARCHAR(200) NOT NULL COMMENT '退课原因',
    request_status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '申请状态',
    teacher_status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '教师审核状态',
    teacher_comment VARCHAR(200) NULL COMMENT '教师审核备注',
    teacher_reviewed_by VARCHAR(12) NULL COMMENT '审核教师工号',
    teacher_reviewed_at DATETIME NULL COMMENT '教师审核时间',
    admin_status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '管理员审核状态',
    admin_comment VARCHAR(200) NULL COMMENT '管理员审核备注',
    admin_reviewed_by INT NULL COMMENT '审核管理员用户编号',
    admin_reviewed_at DATETIME NULL COMMENT '管理员审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (drop_request_id),
    CONSTRAINT fk_drop_request_student
        FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_drop_request_teaching_class
        FOREIGN KEY (teaching_class_id) REFERENCES teaching_class(teaching_class_id),
    CONSTRAINT fk_drop_request_teacher
        FOREIGN KEY (teacher_reviewed_by) REFERENCES teacher(teacher_id),
    CONSTRAINT fk_drop_request_admin
        FOREIGN KEY (admin_reviewed_by) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退课申请表';

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

DELIMITER $$

CREATE TRIGGER trg_course_selection_before_drop_update
BEFORE UPDATE ON course_selection
FOR EACH ROW
BEGIN
    DECLARE v_course_start_date DATE;
    DECLARE v_approved_count INT DEFAULT 0;

    IF OLD.selection_status = '已选' AND NEW.selection_status = '已退' THEN
        SELECT tc.course_start_date
        INTO v_course_start_date
        FROM teaching_class tc
        WHERE tc.teaching_class_id = OLD.teaching_class_id;

        IF v_course_start_date IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '课程开课日期未设置，无法退课';
        END IF;

        SELECT COUNT(*)
        INTO v_approved_count
        FROM course_drop_request cdr
        WHERE cdr.student_id = OLD.student_id
          AND cdr.teaching_class_id = OLD.teaching_class_id
          AND cdr.request_status = '已通过'
          AND cdr.teacher_status = '已通过'
          AND cdr.admin_status = '已通过'
          AND cdr.created_at <= DATE_ADD(v_course_start_date, INTERVAL 21 DAY);

        IF CURDATE() > DATE_ADD(v_course_start_date, INTERVAL 21 DAY)
           AND v_approved_count = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已超过开课三周，不能退课';
        END IF;

        IF CURDATE() > DATE_ADD(v_course_start_date, INTERVAL 7 DAY)
           AND v_approved_count = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '开课超过一周，需教师和管理员均同意后才能退课';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_course_drop_request_before_insert
BEFORE INSERT ON course_drop_request
FOR EACH ROW
BEGIN
    DECLARE v_course_start_date DATE;
    DECLARE v_selected_count INT DEFAULT 0;
    DECLARE v_pending_count INT DEFAULT 0;

    SELECT tc.course_start_date
    INTO v_course_start_date
    FROM teaching_class tc
    WHERE tc.teaching_class_id = NEW.teaching_class_id;

    IF v_course_start_date IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '课程开课日期未设置，无法提交退课申请';
    END IF;

    SELECT COUNT(*)
    INTO v_selected_count
    FROM course_selection cs
    WHERE cs.student_id = NEW.student_id
      AND cs.teaching_class_id = NEW.teaching_class_id
      AND cs.selection_status = '已选';

    IF v_selected_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '未选该课程，不能提交退课申请';
    END IF;

    IF CURDATE() <= DATE_ADD(v_course_start_date, INTERVAL 7 DAY) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '开课一周内可直接退课，无需提交申请';
    END IF;

    IF CURDATE() > DATE_ADD(v_course_start_date, INTERVAL 21 DAY) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已超过开课三周，不能提交退课申请';
    END IF;

    SELECT COUNT(*)
    INTO v_pending_count
    FROM course_drop_request cdr
    WHERE cdr.student_id = NEW.student_id
      AND cdr.teaching_class_id = NEW.teaching_class_id
      AND cdr.request_status = '待审核';

    IF v_pending_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该课程已有待审核退课申请';
    END IF;
END$$

CREATE TRIGGER trg_course_selection_after_drop_update
AFTER UPDATE ON course_selection
FOR EACH ROW
BEGIN
    IF OLD.selection_status = '已选' AND NEW.selection_status = '已退' THEN
        DELETE FROM score
        WHERE student_id = NEW.student_id
          AND teaching_class_id = NEW.teaching_class_id;

        UPDATE teaching_class
        SET selected_count = GREATEST(selected_count - 1, 0)
        WHERE teaching_class_id = NEW.teaching_class_id;
    END IF;
END$$

DELIMITER ;

