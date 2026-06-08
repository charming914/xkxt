-- =====================================================================
-- 0607_fix_schedule_conflicts.sql  修复演示数据中的排课与选课时间冲突
--
-- 背景：早期演示数据可能存在：
--         A. 教学班排课冲突：同学期同时段，同一教室或同一教师被排了多节课；
--         B. 学生选课冲突：同一学生在同学期同(星期+节次)选了多门课。
--
-- 改造说明：排课已拆分到 teaching_class_session 子表（一行=一周一次课，含单双周），
--          本脚本据此修复，且可安全重复执行。
--
-- 时段维度：week_day ∈ [1,5]，section_desc ∈ {'1-2节','3-4节','5-6节','7-8节'}。
-- 冲突判定：同(星期+节次)且单双周相交（全周与任意相交，单周仅与单周，双周仅与双周）。
-- =====================================================================

USE school;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 步骤 1：重排存在冲突的上课时间（session 粒度）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_fix_class_schedule;

DELIMITER $$
CREATE PROCEDURE sp_fix_class_schedule()
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_session_id INT;
    DECLARE v_tc_id INT;
    DECLARE v_semester INT;
    DECLARE v_teacher VARCHAR(12);
    DECLARE v_classroom VARCHAR(10);
    DECLARE v_parity VARCHAR(10);
    DECLARE v_new_week INT;
    DECLARE v_new_section VARCHAR(20);
    DECLARE v_try INT;
    DECLARE v_conflict INT;

    -- 找出与“更小 session_id”冲突的上课时间，保留先排的，重排其余
    DECLARE cur CURSOR FOR
        SELECT s.session_id, s.teaching_class_id, tc.semester_id, tc.teacher_id, tc.classroom_id, s.week_parity
        FROM teaching_class_session s
        JOIN teaching_class tc ON s.teaching_class_id = tc.teaching_class_id
        WHERE EXISTS (
            SELECT 1
            FROM teaching_class_session o
            JOIN teaching_class otc ON o.teaching_class_id = otc.teaching_class_id
            WHERE otc.semester_id = tc.semester_id
              AND o.week_day = s.week_day
              AND o.section_desc = s.section_desc
              AND o.session_id < s.session_id
              AND (o.week_parity = '全周' OR s.week_parity = '全周' OR o.week_parity = s.week_parity)
              AND (otc.teacher_id = tc.teacher_id
                   OR (otc.classroom_id IS NOT NULL AND otc.classroom_id = tc.classroom_id))
        );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN cur;
    fix_loop: LOOP
        FETCH cur INTO v_session_id, v_tc_id, v_semester, v_teacher, v_classroom, v_parity;
        IF v_done = 1 THEN LEAVE fix_loop; END IF;

        SET v_try = 0;
        retry: WHILE v_try < 60 DO
            SET v_try = v_try + 1;
            SET v_new_week = FLOOR(1 + RAND() * 5);
            SET v_new_section = ELT(FLOOR(1 + RAND() * 4), '1-2节', '3-4节', '5-6节', '7-8节');

            -- 同教学班内不可重复时段
            SELECT COUNT(*) INTO v_conflict
            FROM teaching_class_session s
            WHERE s.teaching_class_id = v_tc_id
              AND s.session_id <> v_session_id
              AND s.week_day = v_new_week
              AND s.section_desc = v_new_section;

            -- 同教师 / 同教室冲突（单双周相交）
            IF v_conflict = 0 THEN
                SELECT COUNT(*) INTO v_conflict
                FROM teaching_class_session o
                JOIN teaching_class otc ON o.teaching_class_id = otc.teaching_class_id
                WHERE otc.semester_id = v_semester
                  AND o.week_day = v_new_week
                  AND o.section_desc = v_new_section
                  AND o.session_id <> v_session_id
                  AND (o.week_parity = '全周' OR v_parity = '全周' OR o.week_parity = v_parity)
                  AND (otc.teacher_id = v_teacher
                       OR (v_classroom IS NOT NULL AND otc.classroom_id = v_classroom));
            END IF;

            IF v_conflict = 0 THEN
                UPDATE teaching_class_session
                SET week_day = v_new_week, section_desc = v_new_section
                WHERE session_id = v_session_id;
                LEAVE retry;
            END IF;
        END WHILE retry;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

-- 多跑几轮，确保因重排产生的新冲突也被消化
CALL sp_fix_class_schedule();
CALL sp_fix_class_schedule();
CALL sp_fix_class_schedule();
DROP PROCEDURE IF EXISTS sp_fix_class_schedule;

-- ---------------------------------------------------------------------
-- 步骤 2：清理学生选课时间冲突
--   同一学生同学期、任意两节课(星期+节次重叠且单双周相交)视为冲突。
--   保留先选上的一条，删除其余，并删除对应成绩、回填 selected_count。
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_conflict_selection;
CREATE TEMPORARY TABLE tmp_conflict_selection AS
SELECT DISTINCT cs2.selection_id, cs2.student_id, cs2.teaching_class_id
FROM course_selection cs1
JOIN teaching_class t1 ON cs1.teaching_class_id = t1.teaching_class_id
JOIN teaching_class_session s1 ON s1.teaching_class_id = t1.teaching_class_id
JOIN course_selection cs2 ON cs2.student_id = cs1.student_id
JOIN teaching_class t2 ON cs2.teaching_class_id = t2.teaching_class_id
JOIN teaching_class_session s2 ON s2.teaching_class_id = t2.teaching_class_id
WHERE cs1.selection_status = '已选'
  AND cs2.selection_status = '已选'
  AND t1.semester_id = t2.semester_id
  AND s1.week_day = s2.week_day
  AND CAST(SUBSTRING_INDEX(s1.section_desc, '-', 1) AS UNSIGNED)
      <= CAST(REPLACE(SUBSTRING_INDEX(s2.section_desc, '-', -1), '节', '') AS UNSIGNED)
  AND CAST(SUBSTRING_INDEX(s2.section_desc, '-', 1) AS UNSIGNED)
      <= CAST(REPLACE(SUBSTRING_INDEX(s1.section_desc, '-', -1), '节', '') AS UNSIGNED)
  AND (s1.week_parity = '全周' OR s2.week_parity = '全周' OR s1.week_parity = s2.week_parity)
  AND cs1.teaching_class_id <> cs2.teaching_class_id
  -- cs1 视为“先选上的保留项”：选课时间更早，相同则 id 更小
  AND (cs1.selection_time < cs2.selection_time
       OR (cs1.selection_time = cs2.selection_time AND cs1.selection_id < cs2.selection_id));

-- 删除冲突课程对应的成绩
DELETE sc FROM score sc
JOIN tmp_conflict_selection t
  ON sc.student_id = t.student_id AND sc.teaching_class_id = t.teaching_class_id;

-- 删除冲突的多余选课记录
DELETE cs FROM course_selection cs
JOIN tmp_conflict_selection t ON cs.selection_id = t.selection_id;

DROP TEMPORARY TABLE IF EXISTS tmp_conflict_selection;

-- 回填各教学班实际选课人数
UPDATE teaching_class tc
SET selected_count = (
    SELECT COUNT(*) FROM course_selection cs
    WHERE cs.teaching_class_id = tc.teaching_class_id AND cs.selection_status = '已选'
);
