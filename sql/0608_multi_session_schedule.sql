-- 0608 多节课排课改造脚本（按学分排课 + 单双周）
-- 背景：原 teaching_class 表用 week_day/section_desc 单组字段记录排课，
--       导致一门课一周只能排一节。本脚本拆出 teaching_class_session 子表，
--       一行代表一周内的一次课，并按课程学分自动生成对应节数：
--         每节课 = 2 学时；偶数学分 = 学分/2 节全周课；
--         奇数学分 = floor(学分/2) 节全周课 + 1 节单/双周隔周课。
-- 说明：本脚本可重复执行（幂等）。

-- ---------------------------------------------------------------------
-- 1. 新建上课时间子表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS teaching_class_session (
    session_id INT NOT NULL AUTO_INCREMENT COMMENT '上课时间编号',
    teaching_class_id INT NOT NULL COMMENT '教学班编号',
    week_day TINYINT NOT NULL COMMENT '星期几',
    section_desc VARCHAR(20) NOT NULL COMMENT '节次',
    week_parity VARCHAR(10) NOT NULL DEFAULT '全周' COMMENT '周次类型：全周/单周/双周',
    sort_order INT NOT NULL DEFAULT 1 COMMENT '同教学班内排序',
    PRIMARY KEY (session_id),
    KEY idx_session_class (teaching_class_id),
    KEY idx_session_day_section (week_day, section_desc),
    CONSTRAINT fk_session_teaching_class
        FOREIGN KEY (teaching_class_id) REFERENCES teaching_class(teaching_class_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学班上课时间表';

-- ---------------------------------------------------------------------
-- 2. 按课程学分为每个教学班生成上课时间
--    冲突感知：避开同学期同教师 / 同教室的 星期+节次+单双周 重叠时段。
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_gen_sessions;
DELIMITER $$
CREATE PROCEDURE sp_gen_sessions()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_tc_id INT;
    DECLARE v_semester_id INT;
    DECLARE v_teacher_id VARCHAR(12);
    DECLARE v_classroom_id VARCHAR(10);
    DECLARE v_orig_week_day TINYINT;
    DECLARE v_orig_section VARCHAR(20);
    DECLARE v_credit DECIMAL(4,1);
    DECLARE v_full_sessions INT;        -- 全周节数
    DECLARE v_has_half INT;             -- 是否有单/双周半节
    DECLARE v_made INT;                 -- 已生成节数
    DECLARE v_parity VARCHAR(10);
    DECLARE v_try_day TINYINT;
    DECLARE v_try_section VARCHAR(20);
    DECLARE v_conflict INT;
    DECLARE v_attempt INT;

    DECLARE cur CURSOR FOR
        SELECT tc.teaching_class_id, tc.semester_id, tc.teacher_id, tc.classroom_id,
               COALESCE(tc.week_day, 1), COALESCE(tc.section_desc, '1-2节'), c.credit
        FROM teaching_class tc
        JOIN course c ON tc.course_id = c.course_id
        WHERE NOT EXISTS (
            SELECT 1 FROM teaching_class_session s
            WHERE s.teaching_class_id = tc.teaching_class_id
        );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    tc_loop: LOOP
        FETCH cur INTO v_tc_id, v_semester_id, v_teacher_id, v_classroom_id,
                       v_orig_week_day, v_orig_section, v_credit;
        IF done = 1 THEN LEAVE tc_loop; END IF;

        -- 学分 -> 节数：每节2学时。全周节数 = floor(round(credit)/2)，奇数再加一节单/双周。
        SET v_full_sessions = FLOOR(ROUND(v_credit) / 2);
        SET v_has_half = MOD(ROUND(v_credit), 2);
        IF v_full_sessions = 0 AND v_has_half = 0 THEN
            SET v_full_sessions = 1; -- 学分异常时至少排一节
        END IF;

        SET v_made = 0;

        -- 先用教学班原有的星期/节次作为第一节（若仍有全周节数）
        IF v_full_sessions >= 1 THEN
            INSERT INTO teaching_class_session (teaching_class_id, week_day, section_desc, week_parity, sort_order)
            VALUES (v_tc_id, v_orig_week_day, v_orig_section, '全周', 1);
            SET v_made = 1;
        END IF;

        -- 生成剩余全周节 + 单/双周半节
        WHILE v_made < (v_full_sessions + v_has_half) DO
            IF v_made < v_full_sessions THEN
                SET v_parity = '全周';
            ELSE
                -- 半节：按教学班编号奇偶决定单/双周，保证可复现
                SET v_parity = IF(MOD(v_tc_id, 2) = 1, '单周', '双周');
            END IF;

            SET v_attempt = 0;
            assign_loop: WHILE v_attempt < 60 DO
                SET v_try_day = 1 + MOD(v_tc_id + v_made + v_attempt, 5);
                SET v_try_section = ELT(1 + MOD(v_tc_id + v_made * 2 + v_attempt, 4),
                                        '1-2节', '3-4节', '5-6节', '7-8节');

                -- 与本教学班已排时段冲突？
                SELECT COUNT(*) INTO v_conflict
                FROM teaching_class_session s
                WHERE s.teaching_class_id = v_tc_id
                  AND s.week_day = v_try_day
                  AND s.section_desc = v_try_section;

                -- 与同学期同教师/同教室时段冲突？（单双周相交才算冲突）
                IF v_conflict = 0 THEN
                    SELECT COUNT(*) INTO v_conflict
                    FROM teaching_class_session s
                    JOIN teaching_class tc2 ON s.teaching_class_id = tc2.teaching_class_id
                    WHERE tc2.teaching_class_id <> v_tc_id
                      AND tc2.semester_id = v_semester_id
                      AND s.week_day = v_try_day
                      AND s.section_desc = v_try_section
                      AND (tc2.teacher_id = v_teacher_id
                           OR (v_classroom_id IS NOT NULL AND tc2.classroom_id = v_classroom_id))
                      AND (s.week_parity = '全周' OR v_parity = '全周' OR s.week_parity = v_parity);
                END IF;

                IF v_conflict = 0 THEN
                    LEAVE assign_loop;
                END IF;
                SET v_attempt = v_attempt + 1;
            END WHILE assign_loop;

            INSERT INTO teaching_class_session (teaching_class_id, week_day, section_desc, week_parity, sort_order)
            VALUES (v_tc_id, v_try_day, v_try_section, v_parity, v_made + 1);
            SET v_made = v_made + 1;
        END WHILE;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

-- 仅当旧列仍存在（即首次迁移）时才生成；重复执行时旧列已删除，自动跳过
SET @has_old_cols = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'teaching_class'
      AND column_name = 'week_day'
);
SET @gen_sql = IF(@has_old_cols > 0, 'CALL sp_gen_sessions()', 'DO 0');
PREPARE stmt FROM @gen_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
DROP PROCEDURE IF EXISTS sp_gen_sessions;

-- ---------------------------------------------------------------------
-- 3. 调课申请表增加 session_id（调课针对某一节课）
-- ---------------------------------------------------------------------
SET @add_session_col = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE schedule_adjust_request ADD COLUMN session_id INT NULL COMMENT ''目标上课时间编号'' AFTER teaching_class_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'schedule_adjust_request'
      AND column_name = 'session_id'
);
PREPARE stmt FROM @add_session_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 4. 重建学生课表视图（每节课一行，含单双周）
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_student_timetable AS
SELECT
    cs.student_id,
    s.student_name,
    sem.semester_name,
    c.course_name,
    t.teacher_name,
    sess.week_day,
    sess.section_desc,
    sess.week_parity,
    tc.week_range,
    cl.building_name,
    cl.room_name
FROM course_selection cs
JOIN student s ON cs.student_id = s.student_id
JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
JOIN teaching_class_session sess ON sess.teaching_class_id = tc.teaching_class_id
JOIN semester sem ON tc.semester_id = sem.semester_id
JOIN course c ON tc.course_id = c.course_id
JOIN teacher t ON tc.teacher_id = t.teacher_id
LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
WHERE cs.selection_status = '已选';

-- ---------------------------------------------------------------------
-- 5. 重建选课存储过程的冲突判断（基于上课时间子表 + 单双周）
-- ---------------------------------------------------------------------
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
    DECLARE v_exists_count INT DEFAULT 0;
    DECLARE v_dropped_count INT DEFAULT 0;
    DECLARE v_conflict_count INT DEFAULT 0;

    SELECT tc.max_students, tc.selected_count, tc.semester_id, tc.course_id
    INTO v_max_students, v_selected_count, v_semester_id, v_course_id
    FROM teaching_class tc
    WHERE tc.teaching_class_id = p_teaching_class_id;

    SELECT s.course_select_start, s.course_select_end
    INTO v_select_start, v_select_end
    FROM semester s
    WHERE s.semester_id = v_semester_id;

    IF NOW() < v_select_start OR NOW() > v_select_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '当前不在选课时间范围内';
    END IF;

    SELECT COUNT(*) INTO v_exists_count
    FROM course_selection
    WHERE student_id = p_student_id
      AND teaching_class_id = p_teaching_class_id
      AND selection_status = '已选';

    IF v_exists_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该学生已选该课程';
    END IF;

    SELECT COUNT(*) INTO v_dropped_count
    FROM course_selection
    WHERE student_id = p_student_id
      AND teaching_class_id = p_teaching_class_id
      AND selection_status = '已退';

    IF v_selected_count >= v_max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该教学班人数已满';
    END IF;

    SELECT COUNT(*) INTO v_conflict_count
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

-- ---------------------------------------------------------------------
-- 6. 删除 teaching_class 上的旧排课列（数据已迁移至子表）
-- ---------------------------------------------------------------------
SET @drop_week_day = (
    SELECT IF(COUNT(*) > 0, 'ALTER TABLE teaching_class DROP COLUMN week_day', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'teaching_class' AND column_name = 'week_day'
);
PREPARE stmt FROM @drop_week_day; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @drop_section = (
    SELECT IF(COUNT(*) > 0, 'ALTER TABLE teaching_class DROP COLUMN section_desc', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'teaching_class' AND column_name = 'section_desc'
);
PREPARE stmt FROM @drop_section; EXECUTE stmt; DEALLOCATE PREPARE stmt;


