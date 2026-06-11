-- 选课手动控制：按默认时间、手动开启、强制关闭

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE semester ADD COLUMN course_selection_manual_mode VARCHAR(10) NOT NULL DEFAULT ''AUTO'' COMMENT ''选课手动控制：AUTO按默认时间，OPEN手动开启，CLOSED强制关闭'' AFTER course_select_end',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'semester'
      AND COLUMN_NAME = 'course_selection_manual_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE semester ADD COLUMN course_selection_manual_end DATETIME NULL COMMENT ''手动开启截止时间，为空表示一直开启'' AFTER course_selection_manual_mode',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'semester'
      AND COLUMN_NAME = 'course_selection_manual_end'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE semester
SET course_selection_manual_mode = 'AUTO'
WHERE course_selection_manual_mode IS NULL
   OR course_selection_manual_mode = '';

DROP PROCEDURE IF EXISTS sp_select_course;

DELIMITER $$
CREATE PROCEDURE sp_select_course (
    IN p_student_id VARCHAR(12),
    IN p_teaching_class_id INT
)
BEGIN
    DECLARE v_max_students INT;
    DECLARE v_selected_count INT;
    DECLARE v_semester_id INT;
    DECLARE v_course_id VARCHAR(12);
    DECLARE v_select_start DATETIME;
    DECLARE v_select_end DATETIME;
    DECLARE v_manual_mode VARCHAR(10) DEFAULT 'AUTO';
    DECLARE v_manual_end DATETIME;
    DECLARE v_exists_count INT DEFAULT 0;
    DECLARE v_dropped_count INT DEFAULT 0;
    DECLARE v_conflict_count INT DEFAULT 0;

    SELECT tc.max_students, tc.selected_count, tc.semester_id, tc.course_id
    INTO v_max_students, v_selected_count, v_semester_id, v_course_id
    FROM teaching_class tc
    WHERE tc.teaching_class_id = p_teaching_class_id;

    SELECT s.course_select_start,
           s.course_select_end,
           COALESCE(s.course_selection_manual_mode, 'AUTO'),
           s.course_selection_manual_end
    INTO v_select_start, v_select_end, v_manual_mode, v_manual_end
    FROM semester s
    WHERE s.semester_id = v_semester_id;

    IF v_manual_mode = 'CLOSED'
       OR NOT (
           NOW() BETWEEN v_select_start AND v_select_end
           OR (v_manual_mode = 'OPEN' AND (v_manual_end IS NULL OR NOW() <= v_manual_end))
       ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '当前不在选课时间范围内';
    END IF;

    SELECT COUNT(*)
    INTO v_exists_count
    FROM course_selection
    WHERE student_id = p_student_id
      AND teaching_class_id = p_teaching_class_id
      AND selection_status = '已选';

    IF v_exists_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该学生已选该课程';
    END IF;

    SELECT COUNT(*)
    INTO v_dropped_count
    FROM course_selection
    WHERE student_id = p_student_id
      AND teaching_class_id = p_teaching_class_id
      AND selection_status = '已退';

    IF v_selected_count >= v_max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该教学班人数已满';
    END IF;

    SELECT COUNT(*)
    INTO v_conflict_count
    FROM course_selection cs
    JOIN teaching_class_session s1 ON cs.teaching_class_id = s1.teaching_class_id
    JOIN teaching_class t1 ON cs.teaching_class_id = t1.teaching_class_id
    JOIN teaching_class t2 ON t2.teaching_class_id = p_teaching_class_id
    JOIN teaching_class_session s2 ON s2.teaching_class_id = t2.teaching_class_id
    WHERE cs.student_id = p_student_id
      AND cs.selection_status = '已选'
      AND t1.semester_id = t2.semester_id
      AND s1.week_day = s2.week_day
      AND CAST(SUBSTRING_INDEX(s1.section_desc, '-', 1) AS UNSIGNED)
          <= CAST(REPLACE(SUBSTRING_INDEX(s2.section_desc, '-', -1), '节', '') AS UNSIGNED)
      AND CAST(SUBSTRING_INDEX(s2.section_desc, '-', 1) AS UNSIGNED)
          <= CAST(REPLACE(SUBSTRING_INDEX(s1.section_desc, '-', -1), '节', '') AS UNSIGNED)
      AND (s1.week_parity = '全周' OR s2.week_parity = '全周' OR s1.week_parity = s2.week_parity);

    IF v_conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '选课时间冲突';
    END IF;

    IF v_dropped_count > 0 THEN
        DELETE FROM score
        WHERE student_id = p_student_id
          AND teaching_class_id = p_teaching_class_id;

        UPDATE course_selection
        SET selection_status = '已选',
            selection_time = NOW()
        WHERE student_id = p_student_id
          AND teaching_class_id = p_teaching_class_id
          AND selection_status = '已退';
    ELSE
        INSERT INTO course_selection (student_id, teaching_class_id, selection_status)
        VALUES (p_student_id, p_teaching_class_id, '已选');
    END IF;

    UPDATE teaching_class
    SET selected_count = selected_count + 1
    WHERE teaching_class_id = p_teaching_class_id;

    INSERT INTO operation_log (user_id, operation_type, operation_content)
    SELECT user_id, '选课', CONCAT('学生 ', p_student_id, ' 选课成功，教学班编号：', p_teaching_class_id)
    FROM sys_user
    WHERE ref_id = p_student_id
    LIMIT 1;
END$$
DELIMITER ;
