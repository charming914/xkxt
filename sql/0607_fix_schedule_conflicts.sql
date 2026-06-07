-- =====================================================================
-- 0607_fix_schedule_conflicts.sql  修复演示数据中的排课与选课时间冲突
--
-- 背景：早期演示数据通过 INSERT ... SELECT 直接写表，绕过了选课存储过程
--       与冲突校验，导致：
--         A. 教学班排课冲突：同学期同时段，同一教室或同一教师被排了多个班；
--         B. 学生选课冲突：同一学生在同学期同一(星期+节次)选了多门课。
--
-- 本脚本分两步修复，且可安全重复执行（修完后再跑不会有副作用）：
--   1. 为冲突的教学班重排到“该教师、该教室均空闲”的时段；
--   2. 删除学生选课中冲突的多余记录（保留先选上的），并清理对应成绩、回填计数。
--
-- 时段维度：week_day ∈ [1,5]，section_desc ∈ {'1-2节','3-4节','5-6节','7-8节'}，
--          共 20 个标准时段格子，足够 37 个教学班错峰排布。
-- =====================================================================

USE school;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 步骤 1：重排存在冲突的教学班
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_fix_class_schedule;

DELIMITER $$
CREATE PROCEDURE sp_fix_class_schedule()
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_tc_id INT;
    DECLARE v_semester INT;
    DECLARE v_teacher VARCHAR(12);
    DECLARE v_classroom VARCHAR(10);
    DECLARE v_new_week INT;
    DECLARE v_new_section VARCHAR(20);
    DECLARE v_try INT;
    DECLARE v_conflict INT;

    -- 找出所有“非该时段第一个”的冲突教学班：
    -- 按 (学期, 星期, 节次) 分组，同教室或同教师出现重复时，保留 id 最小者，
    -- 其余的列为待重排对象。
    DECLARE cur CURSOR FOR
        SELECT tc.teaching_class_id, tc.semester_id, tc.teacher_id, tc.classroom_id
        FROM teaching_class tc
        WHERE EXISTS (
            SELECT 1 FROM teaching_class o
            WHERE o.semester_id = tc.semester_id
              AND o.week_day = tc.week_day
              AND o.section_desc = tc.section_desc
              AND o.teaching_class_id < tc.teaching_class_id
              AND (o.teacher_id = tc.teacher_id
                   OR (o.classroom_id IS NOT NULL AND o.classroom_id = tc.classroom_id))
        );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN cur;
    fix_loop: LOOP
        FETCH cur INTO v_tc_id, v_semester, v_teacher, v_classroom;
        IF v_done = 1 THEN LEAVE fix_loop; END IF;

        SET v_try = 0;
        retry: WHILE v_try < 60 DO
            SET v_try = v_try + 1;
            SET v_new_week = FLOOR(1 + RAND() * 5);
            SET v_new_section = ELT(FLOOR(1 + RAND() * 4), '1-2节', '3-4节', '5-6节', '7-8节');

            -- 检查目标时段：该教师 / 该教室是否已被其它班占用
            SELECT COUNT(*) INTO v_conflict
            FROM teaching_class o
            WHERE o.semester_id = v_semester
              AND o.week_day = v_new_week
              AND o.section_desc = v_new_section
              AND o.teaching_class_id <> v_tc_id
              AND (o.teacher_id = v_teacher
                   OR (v_classroom IS NOT NULL AND o.classroom_id = v_classroom));

            IF v_conflict = 0 THEN
                UPDATE teaching_class
                SET week_day = v_new_week, section_desc = v_new_section
                WHERE teaching_class_id = v_tc_id;
                LEAVE retry;
            END IF;
        END WHILE retry;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

-- 多跑几轮，确保因重排产生的新冲突也被消化（通常 1-2 轮即可收敛）
CALL sp_fix_class_schedule();
CALL sp_fix_class_schedule();
CALL sp_fix_class_schedule();
DROP PROCEDURE IF EXISTS sp_fix_class_schedule;

-- ---------------------------------------------------------------------
-- 步骤 2：清理学生选课时间冲突
--   同一学生在同学期、同星期、且“节次区间重叠”的多门课视为冲突
--   （兼容 "1-2节" 与 "1-4节" 这类跨段重叠）。保留先选上的一条，删除其余，
--   同时删除被删课程对应的成绩，并回填 selected_count。
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_conflict_selection;
CREATE TEMPORARY TABLE tmp_conflict_selection AS
SELECT DISTINCT cs2.selection_id, cs2.student_id, cs2.teaching_class_id
FROM course_selection cs1
JOIN teaching_class t1 ON cs1.teaching_class_id = t1.teaching_class_id
JOIN course_selection cs2 ON cs2.student_id = cs1.student_id
JOIN teaching_class t2 ON cs2.teaching_class_id = t2.teaching_class_id
WHERE cs1.selection_status = '已选'
  AND cs2.selection_status = '已选'
  AND t1.semester_id = t2.semester_id
  AND t1.week_day = t2.week_day
  -- 节次区间重叠
  AND CAST(SUBSTRING_INDEX(t1.section_desc, '-', 1) AS UNSIGNED)
      <= CAST(REPLACE(SUBSTRING_INDEX(t2.section_desc, '-', -1), '节', '') AS UNSIGNED)
  AND CAST(SUBSTRING_INDEX(t2.section_desc, '-', 1) AS UNSIGNED)
      <= CAST(REPLACE(SUBSTRING_INDEX(t1.section_desc, '-', -1), '节', '') AS UNSIGNED)
  -- cs1 视为“先选上的保留项”：选课时间更早，时间相同则 id 更小
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
