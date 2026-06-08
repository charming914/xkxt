-- =====================================================================
-- 0607_demo_seed_data.sql  演示数据生成脚本
-- 目的：批量生成约 250 名学生、30 名教师、30 门课程，以及配套的院系、
--       专业、班级、教室、教学班、选课与成绩数据，用于功能演示。
--
-- 设计要点：
--   - 上一学期(2025-2026-1)的教学班状态为“已结束”，并录入了已提交成绩，
--     用于“成绩图表分析”和学生成绩/绩点展示。
--   - 当前学期(2025-2026-2)的教学班状态为“选课中”，仅有选课数据，
--     用于“统计分析”和选课相关演示。
--   - 所有新生成的学生/教师都会同步 sys_user 登录账号，默认密码 123456。
--   - 选课通过系统存储过程 sp_select_course 逐条完成，天然满足容量上限与
--     上课时间不冲突；可安全重复执行（基础数据用 NOT EXISTS/INSERT IGNORE
--     去重，选课的去重/容量/冲突由存储过程把关）。
--
-- 导入顺序：
--   1) school.sql                      建表、触发器、存储过程、基础数据
--   2) 0607_demo_seed_data.sql         本脚本，生成批量演示数据
--   3) 0607_fix_schedule_conflicts.sql 消解教学班“同教师/同教室同时段”排课
--                                      冲突（本脚本随机排课可能产生少量冲突）
-- =====================================================================

USE school;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 院系 / 专业 / 班级 / 教室 等基础维度数据
-- ---------------------------------------------------------------------
INSERT IGNORE INTO department (department_id, department_name, office_address, contact_phone) VALUES
('D04', '外国语学院', '上大东校区五号楼', '65348001'),
('D05', '理学院', '上大东校区六号楼', '65348002');

INSERT IGNORE INTO major (major_id, major_name, department_id) VALUES
('M05', '人工智能', 'D01'),
('M06', '电子信息工程', 'D02'),
('M07', '材料物理', 'D03'),
('M08', '英语', 'D04'),
('M09', '数学与应用数学', 'D05'),
('M10', '应用物理学', 'D05');

INSERT IGNORE INTO class_info (class_id, class_name, grade_year, major_id, counselor_name) VALUES
('C2404', '计科2402班', '2024', 'M01', '王老师'),
('C2405', '软工2402班', '2024', 'M02', '李老师'),
('C2406', '人工智能2401班', '2024', 'M05', '赵老师'),
('C2407', '通信2402班', '2024', 'M03', '周老师'),
('C2408', '电子信息2401班', '2024', 'M06', '钱老师'),
('C2409', '材料2401班', '2024', 'M04', '孙老师'),
('C2410', '材料物理2401班', '2024', 'M07', '吴老师'),
('C2411', '英语2401班', '2024', 'M08', '郑老师'),
('C2412', '数学2401班', '2024', 'M09', '冯老师'),
('C2413', '物理2401班', '2024', 'M10', '陈老师');

INSERT IGNORE INTO classroom (classroom_id, building_name, room_name, capacity) VALUES
('R103', '一教', '103', 70),
('R202', '二教', '202', 90),
('R203', '二教', '203', 80),
('R301', '三教', '301', 120),
('R302', '三教', '302', 100),
('LAB2', '实验楼', '网络实验室', 60),
('LAB3', '实验楼', '人工智能实验室', 50),
('LAB4', '实验楼', '材料实验室', 45);

-- ---------------------------------------------------------------------
-- 2. 批量生成教师（30 名）、课程（30 门）、学生（250 名）
--    使用临时存储过程 + 循环生成顺序编号与随机姓名。
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_gen_demo_data;

DELIMITER $$
CREATE PROCEDURE sp_gen_demo_data()
BEGIN
    DECLARE i INT;
    DECLARE v_teacher_id VARCHAR(12);
    DECLARE v_dept VARCHAR(10);
    DECLARE v_major VARCHAR(10);
    DECLARE v_class VARCHAR(10);
    DECLARE v_name VARCHAR(20);
    DECLARE v_gender CHAR(1);
    DECLARE v_course_id VARCHAR(12);
    DECLARE v_student_id VARCHAR(12);

    -- 姓氏与名字字库，用于拼装随机姓名
    -- 院系数组（含原有 D01-D03 与新增 D04/D05）
    -- 通过 ELT + FLOOR(RAND()*N+1) 随机取值

    -- ===== 2.1 生成 30 名教师：工号 T1001 ~ T1030 =====
    SET i = 1;
    WHILE i <= 30 DO
        SET v_teacher_id = CONCAT('T10', LPAD(i, 2, '0'));
        SET v_dept = ELT(FLOOR(1 + RAND() * 5), 'D01', 'D02', 'D03', 'D04', 'D05');
        SET v_gender = IF(RAND() < 0.5, '男', '女');
        SET v_name = CONCAT(
            ELT(FLOOR(1 + RAND() * 12), '王','李','张','刘','陈','杨','黄','赵','周','吴','徐','孙'),
            ELT(FLOOR(1 + RAND() * 12), '建国','志强','秀英','敏','静','丽','强','磊','军','洋','勇','艳')
        );
        IF NOT EXISTS (SELECT 1 FROM teacher WHERE teacher_id = v_teacher_id) THEN
            INSERT INTO teacher (teacher_id, teacher_name, gender, birth_date, title_name, education, phone, email, department_id, status)
            VALUES (
                v_teacher_id, v_name, v_gender,
                DATE_ADD('1965-01-01', INTERVAL FLOOR(RAND() * 7300) DAY),
                ELT(FLOOR(1 + RAND() * 4), '助教', '讲师', '副教授', '教授'),
                ELT(FLOOR(1 + RAND() * 3), '硕士', '博士', '博士'),
                CONCAT('138', LPAD(FLOOR(RAND() * 100000000), 8, '0')),
                CONCAT(LOWER(v_teacher_id), '@school.edu.cn'),
                v_dept, '在职'
            );
        END IF;
        SET i = i + 1;
    END WHILE;

    -- ===== 2.2 生成 30 门课程：课程编号 09xxxxxx =====
    SET i = 1;
    WHILE i <= 30 DO
        SET v_course_id = CONCAT('0840', LPAD(i, 4, '0'));
        SET v_dept = ELT(FLOOR(1 + RAND() * 5), 'D01', 'D02', 'D03', 'D04', 'D05');
        IF NOT EXISTS (SELECT 1 FROM course WHERE course_id = v_course_id) THEN
            INSERT INTO course (course_id, course_name, course_type, credit, total_hours, assessment_method, department_id, is_required, description)
            VALUES (
                v_course_id,
                CONCAT(
                    ELT(FLOOR(1 + RAND() * 10), '高等','线性','概率','大学','现代','应用','计算','工程','专业','综合'),
                    ELT(FLOOR(1 + RAND() * 10), '数学','代数','统计','物理','英语','力学','导论','基础','原理','实践'),
                    LPAD(i, 2, '0')
                ),
                ELT(FLOOR(1 + RAND() * 4), '公共基础课', '专业基础课', '专业核心课', '专业选修课'),
                ELT(FLOOR(1 + RAND() * 4), 2.0, 3.0, 4.0, 4.0),
                ELT(FLOOR(1 + RAND() * 4), 32, 40, 48, 50),
                IF(RAND() < 0.7, '考试', '考查'),
                v_dept,
                IF(RAND() < 0.6, 1, 0),
                '演示数据自动生成课程'
            );
        END IF;
        SET i = i + 1;
    END WHILE;

    -- ===== 2.3 生成 250 名学生：学号 20240006 ~ 20240255 =====
    SET i = 6;
    WHILE i <= 255 DO
        SET v_student_id = CONCAT('2024', LPAD(i, 4, '0'));
        -- 随机选一个专业，并据此确定院系与班级
        SET v_major = ELT(FLOOR(1 + RAND() * 10), 'M01','M02','M03','M04','M05','M06','M07','M08','M09','M10');
        SET v_dept = CASE v_major
            WHEN 'M01' THEN 'D01' WHEN 'M02' THEN 'D01' WHEN 'M05' THEN 'D01'
            WHEN 'M03' THEN 'D02' WHEN 'M06' THEN 'D02'
            WHEN 'M04' THEN 'D03' WHEN 'M07' THEN 'D03'
            WHEN 'M08' THEN 'D04'
            ELSE 'D05' END;
        SET v_class = CASE v_major
            WHEN 'M01' THEN ELT(FLOOR(1 + RAND() * 2), 'C2401', 'C2404')
            WHEN 'M02' THEN ELT(FLOOR(1 + RAND() * 2), 'C2402', 'C2405')
            WHEN 'M03' THEN ELT(FLOOR(1 + RAND() * 2), 'C2403', 'C2407')
            WHEN 'M04' THEN 'C2409'
            WHEN 'M05' THEN 'C2406'
            WHEN 'M06' THEN 'C2408'
            WHEN 'M07' THEN 'C2410'
            WHEN 'M08' THEN 'C2411'
            WHEN 'M09' THEN 'C2412'
            ELSE 'C2413' END;
        SET v_gender = IF(RAND() < 0.5, '男', '女');
        SET v_name = CONCAT(
            ELT(FLOOR(1 + RAND() * 12), '王','李','张','刘','陈','杨','黄','赵','周','吴','徐','孙'),
            ELT(FLOOR(1 + RAND() * 16), '宇航','梓涵','子轩','欣怡','浩然','一诺','雨萱','思源','梦琪','志远','佳怡','俊杰','晓彤','文博','嘉怡','子墨')
        );
        IF NOT EXISTS (SELECT 1 FROM student WHERE student_id = v_student_id) THEN
            INSERT INTO student (student_id, student_name, gender, birth_date, hometown, phone, email, enrollment_year, department_id, major_id, class_id, status)
            VALUES (
                v_student_id, v_name, v_gender,
                DATE_ADD('2005-01-01', INTERVAL FLOOR(RAND() * 730) DAY),
                ELT(FLOOR(1 + RAND() * 10), '上海','江苏','浙江','安徽','山东','广东','四川','湖北','河南','北京'),
                CONCAT('139', LPAD(FLOOR(RAND() * 100000000), 8, '0')),
                CONCAT(v_student_id, '@school.edu.cn'),
                '2024', v_dept, v_major, v_class, '正常'
            );
        END IF;
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL sp_gen_demo_data();
DROP PROCEDURE IF EXISTS sp_gen_demo_data;

-- ---------------------------------------------------------------------
-- 3. 为新增学生 / 教师同步登录账号（sys_user）与角色关系
--    默认密码 123456（明文写入，首次登录时由后端自动升级为 BCrypt）。
-- ---------------------------------------------------------------------
INSERT INTO sys_user (username, password, real_name, user_type, ref_id, is_enabled)
SELECT s.student_id, '123456', s.student_name, '学生', s.student_id, 1
FROM student s
WHERE NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = s.student_id);

INSERT INTO sys_user (username, password, real_name, user_type, ref_id, is_enabled)
SELECT t.teacher_id, '123456', t.teacher_name, '教师', t.teacher_id, 1
FROM teacher t
WHERE NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = t.teacher_id);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN sys_role r ON (
    (u.user_type = '学生' AND r.role_code = 'STUDENT') OR
    (u.user_type = '教师' AND r.role_code = 'TEACHER')
)
WHERE u.ref_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- 4. 生成教学班
--    每门课程在两个学期各开 1 个教学班：
--      - 学期1(semester_id=1)：状态“已结束”，用于成绩分析
--      - 学期2(semester_id=2)：状态“选课中”，用于选课统计
--    教师、教室、上课时间均随机分配。
--    注意：随机分配可能产生少量“同教师/同教室同时段”的排课冲突，
--          导入本脚本后请再执行 0607_fix_schedule_conflicts.sql 自动消解冲突。
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_gen_teaching_classes;
DELIMITER $$
CREATE PROCEDURE sp_gen_teaching_classes()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_course_id VARCHAR(12);
    DECLARE cur CURSOR FOR SELECT course_id FROM course;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    course_loop: LOOP
        FETCH cur INTO v_course_id;
        IF done = 1 THEN LEAVE course_loop; END IF;

        -- 学期1：已结束的历史教学班
        IF NOT EXISTS (SELECT 1 FROM teaching_class WHERE semester_id = 1 AND course_id = v_course_id) THEN
            INSERT INTO teaching_class (semester_id, course_id, teacher_id, classroom_id, course_start_date, week_range, max_students, selected_count, status)
            VALUES (
                1, v_course_id,
                (SELECT teacher_id FROM teacher ORDER BY RAND() LIMIT 1),
                (SELECT classroom_id FROM classroom ORDER BY RAND() LIMIT 1),
                '2025-09-01', '1-16周',
                FLOOR(40 + RAND() * 41), 0, '已结束'
            );
        END IF;

        -- 学期2：当前学期，选课中
        IF NOT EXISTS (SELECT 1 FROM teaching_class WHERE semester_id = 2 AND course_id = v_course_id) THEN
            INSERT INTO teaching_class (semester_id, course_id, teacher_id, classroom_id, course_start_date, week_range, max_students, selected_count, status)
            VALUES (
                2, v_course_id,
                (SELECT teacher_id FROM teacher ORDER BY RAND() LIMIT 1),
                (SELECT classroom_id FROM classroom ORDER BY RAND() LIMIT 1),
                '2026-03-01', '1-16周',
                FLOOR(40 + RAND() * 41), 0, '选课中'
            );
        END IF;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL sp_gen_teaching_classes();
DROP PROCEDURE IF EXISTS sp_gen_teaching_classes;

-- 按课程学分为新建教学班生成上课时间（每节2学时，奇数学分排单/双周半节）
DROP PROCEDURE IF EXISTS sp_demo_gen_sessions;
DELIMITER $$
CREATE PROCEDURE sp_demo_gen_sessions()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_tc_id INT;
    DECLARE v_semester_id INT;
    DECLARE v_teacher_id VARCHAR(12);
    DECLARE v_classroom_id VARCHAR(10);
    DECLARE v_credit DECIMAL(4,1);
    DECLARE v_full_sessions INT;
    DECLARE v_has_half INT;
    DECLARE v_made INT;
    DECLARE v_parity VARCHAR(10);
    DECLARE v_try_day TINYINT;
    DECLARE v_try_section VARCHAR(20);
    DECLARE v_conflict INT;
    DECLARE v_attempt INT;

    DECLARE cur CURSOR FOR
        SELECT tc.teaching_class_id, tc.semester_id, tc.teacher_id, tc.classroom_id, c.credit
        FROM teaching_class tc
        JOIN course c ON tc.course_id = c.course_id
        WHERE NOT EXISTS (
            SELECT 1 FROM teaching_class_session s WHERE s.teaching_class_id = tc.teaching_class_id
        );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    tc_loop: LOOP
        FETCH cur INTO v_tc_id, v_semester_id, v_teacher_id, v_classroom_id, v_credit;
        IF done = 1 THEN LEAVE tc_loop; END IF;

        SET v_full_sessions = FLOOR(ROUND(v_credit) / 2);
        SET v_has_half = MOD(ROUND(v_credit), 2);
        IF v_full_sessions = 0 AND v_has_half = 0 THEN SET v_full_sessions = 1; END IF;

        SET v_made = 0;
        WHILE v_made < (v_full_sessions + v_has_half) DO
            SET v_parity = IF(v_made < v_full_sessions, '全周', IF(MOD(v_tc_id, 2) = 1, '单周', '双周'));

            SET v_attempt = 0;
            assign_loop: WHILE v_attempt < 60 DO
                SET v_try_day = 1 + MOD(v_tc_id + v_made + v_attempt, 5);
                SET v_try_section = ELT(1 + MOD(v_tc_id + v_made * 2 + v_attempt, 4), '1-2节', '3-4节', '5-6节', '7-8节');

                SELECT COUNT(*) INTO v_conflict
                FROM teaching_class_session s
                WHERE s.teaching_class_id = v_tc_id AND s.week_day = v_try_day AND s.section_desc = v_try_section;

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

                IF v_conflict = 0 THEN LEAVE assign_loop; END IF;
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

CALL sp_demo_gen_sessions();
DROP PROCEDURE IF EXISTS sp_demo_gen_sessions;

-- ---------------------------------------------------------------------
-- 5. 生成选课记录（通过系统存储过程 sp_select_course 逐条选课）
--    设计思路：不再用 INSERT 直接写表，而是模拟学生逐个抢课，
--    调用与学生端完全相同的 sp_select_course。该存储过程内置四重校验：
--      - 选课时间窗口；
--      - 是否已选该课（去重）；
--      - 教学班容量上限；
--      - 同一学生的上课时间(星期+节次)冲突。
--    满员或冲突时存储过程会抛异常，驱动过程用 CONTINUE HANDLER 捕获后跳过，
--    因此最终数据天然满足容量与时间不冲突，且可安全重复执行。
--
--    注意：当前真实日期通常已不在历史学期的选课窗口内，故在调用前临时把
--    两个学期的选课窗口放宽到覆盖当下，跑完后立即恢复原值。
-- ---------------------------------------------------------------------

-- 5.1 备份并临时放宽选课时间窗口
DROP TEMPORARY TABLE IF EXISTS tmp_semester_window;
CREATE TEMPORARY TABLE tmp_semester_window AS
SELECT semester_id, course_select_start, course_select_end FROM semester;

UPDATE semester
SET course_select_start = '2000-01-01 00:00:00',
    course_select_end = '2099-12-31 23:59:59';

-- 5.2 驱动过程：为每名学生在每个学期随机挑选若干教学班并尝试选课
DROP PROCEDURE IF EXISTS sp_seed_enrollment;
DELIMITER $$
CREATE PROCEDURE sp_seed_enrollment(IN p_semester_id INT, IN p_max_per_student INT)
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_student_id VARCHAR(12);
    DECLARE v_teaching_class_id INT;

    -- 候选集：每名学生在该学期随机排序的教学班，仅取前 N 个尝试选课。
    -- 真正能否选上由 sp_select_course 判定（容量/冲突/去重）。
    DECLARE cur CURSOR FOR
        SELECT student_id, teaching_class_id FROM (
            SELECT s.student_id, tc.teaching_class_id,
                   ROW_NUMBER() OVER (PARTITION BY s.student_id ORDER BY RAND()) AS rn
            FROM student s
            JOIN teaching_class tc ON tc.semester_id = p_semester_id
        ) ranked
        WHERE rn <= p_max_per_student;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;
    -- 选课失败（满员/冲突/已选/不在窗口）时跳过该条，继续下一条
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;

    OPEN cur;
    enroll_loop: LOOP
        FETCH cur INTO v_student_id, v_teaching_class_id;
        IF v_done = 1 THEN LEAVE enroll_loop; END IF;
        CALL sp_select_course(v_student_id, v_teaching_class_id);
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

-- 5.3 执行选课：每个学期每名学生最多尝试 6 个教学班（实际选上数受容量/冲突限制）
CALL sp_seed_enrollment(1, 6);
CALL sp_seed_enrollment(2, 6);
DROP PROCEDURE IF EXISTS sp_seed_enrollment;

-- 5.4 恢复选课时间窗口原值
UPDATE semester sem
JOIN tmp_semester_window w ON sem.semester_id = w.semester_id
SET sem.course_select_start = w.course_select_start,
    sem.course_select_end = w.course_select_end;
DROP TEMPORARY TABLE IF EXISTS tmp_semester_window;

-- ---------------------------------------------------------------------
-- 6. 生成历史学期成绩（仅 semester_id=1 的教学班）
--    只插入平时成绩与考试成绩，总评成绩与绩点由触发器 trg_score_before_insert 自动计算。
--    分数围绕 70 分波动并裁剪到 [40,100]，形成自然的分数段分布。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO score (student_id, teaching_class_id, usual_score, exam_score, is_submitted, entered_at)
SELECT
    cs.student_id,
    cs.teaching_class_id,
    LEAST(100, GREATEST(45, ROUND(75 + (RAND() * 40 - 18)))) AS usual_score,
    LEAST(100, GREATEST(35, ROUND(72 + (RAND() * 46 - 22)))) AS exam_score,
    1,
    NOW()
FROM course_selection cs
JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
WHERE tc.semester_id = 1
  AND cs.selection_status = '已选';

-- ---------------------------------------------------------------------
-- 7. 兜底校正各教学班的 selected_count
--    正常情况下 sp_select_course 已实时维护该字段，这里按实际选课记录
--    重算一次，确保与 course_selection 完全一致（重复执行该脚本时尤为重要）。
-- ---------------------------------------------------------------------
UPDATE teaching_class tc
SET selected_count = (
    SELECT COUNT(*)
    FROM course_selection cs
    WHERE cs.teaching_class_id = tc.teaching_class_id
      AND cs.selection_status = '已选'
);

-- ---------------------------------------------------------------------
-- 8. 演示用：补充少量待审批申请与操作日志
--    让管理员仪表盘的“待办审批”和“操作日志”页面有初始数据。
-- ---------------------------------------------------------------------

-- 8.1 待审核的补选申请（取当前学期 3 个未选课的学生 + 教学班组合）
INSERT INTO course_selection_request (student_id, teaching_class_id, request_reason, request_status)
SELECT student_id, teaching_class_id, '希望补选该课程，丰富本学期学习内容。', '待审核'
FROM (
    SELECT s.student_id, tc.teaching_class_id,
           ROW_NUMBER() OVER (ORDER BY RAND()) AS rn
    FROM student s
    JOIN teaching_class tc ON tc.semester_id = 2 AND tc.status = '选课中'
    WHERE NOT EXISTS (
        SELECT 1 FROM course_selection cs
        WHERE cs.student_id = s.student_id AND cs.teaching_class_id = tc.teaching_class_id
    )
) pick
WHERE rn <= 3;

-- 8.2 说明：退课申请受触发器约束（开课满三周后不能提交退课申请），
--     当前学期开课日期为 2026-03-01，已超过三周，故不在此插入演示退课申请。
--     如需演示退课审批，可将某个教学班的 course_start_date 临时改为近期日期后再提交。

-- 8.3 初始操作日志（管理员账号）
INSERT INTO operation_log (user_id, operation_type, operation_content, operation_time)
SELECT u.user_id, t.operation_type, t.operation_content,
       DATE_SUB(NOW(), INTERVAL t.minutes_ago MINUTE)
FROM sys_user u
CROSS JOIN (
    SELECT '学生管理' AS operation_type, '批量导入演示学生数据' AS operation_content, 600 AS minutes_ago
    UNION ALL SELECT '教师管理', '批量导入演示教师数据', 580
    UNION ALL SELECT '课程管理', '批量导入演示课程数据', 560
    UNION ALL SELECT '教学班管理', '为当前学期开设教学班', 540
    UNION ALL SELECT '选课设置', '更新选课设置', 300
    UNION ALL SELECT '成绩设置', '更新成绩提交时间', 200
) t
WHERE u.username = 'admin';
