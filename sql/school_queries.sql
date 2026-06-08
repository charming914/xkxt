USE school;

-- 1. 学生查看当前学期可选课程
SELECT
    tc.teaching_class_id,
    sem.semester_name,
    c.course_id,
    c.course_name,
    c.credit,
    c.total_hours,
    t.teacher_name,
    GROUP_CONCAT(
        CONCAT('周', sess.week_day, ' ', sess.section_desc,
               IF(sess.week_parity = '全周', '', CONCAT('(', sess.week_parity, ')')))
        ORDER BY sess.week_day, sess.section_desc SEPARATOR '；'
    ) AS schedule_text,
    tc.week_range,
    cl.building_name,
    cl.room_name,
    tc.max_students,
    tc.selected_count
FROM teaching_class tc
JOIN semester sem ON tc.semester_id = sem.semester_id
JOIN course c ON tc.course_id = c.course_id
JOIN teacher t ON tc.teacher_id = t.teacher_id
LEFT JOIN teaching_class_session sess ON sess.teaching_class_id = tc.teaching_class_id
LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
WHERE sem.is_current = 1
  AND tc.status = '选课中'
GROUP BY tc.teaching_class_id;

-- 2. 调用存储过程进行选课
CALL sp_select_course('20240001', 1);

-- 3. 学生查看个人课表
SELECT *
FROM v_student_timetable
WHERE student_id = '20240001'
ORDER BY semester_name, week_day, section_desc;
-- 4. 学生查看个人成绩
SELECT *
FROM v_student_score
WHERE student_id = '20240002'
ORDER BY semester_name, course_name;

-- 5. 学生查看本学期已修学分
SELECT
    cs.student_id,
    sem.semester_name,
    SUM(c.credit) AS selected_credits
FROM course_selection cs
JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
JOIN semester sem ON tc.semester_id = sem.semester_id
JOIN course c ON tc.course_id = c.course_id
WHERE cs.student_id = '20240002'
  AND cs.selection_status = '已选'
GROUP BY cs.student_id, sem.semester_name;

-- 6. 教师查看自己本学期授课列表
SELECT
    tc.teaching_class_id,
    sem.semester_name,
    c.course_name,
    GROUP_CONCAT(
        CONCAT('周', sess.week_day, ' ', sess.section_desc,
               IF(sess.week_parity = '全周', '', CONCAT('(', sess.week_parity, ')')))
        ORDER BY sess.week_day, sess.section_desc SEPARATOR '；'
    ) AS schedule_text,
    tc.week_range,
    tc.selected_count
FROM teaching_class tc
JOIN semester sem ON tc.semester_id = sem.semester_id
JOIN course c ON tc.course_id = c.course_id
LEFT JOIN teaching_class_session sess ON sess.teaching_class_id = tc.teaching_class_id
WHERE tc.teacher_id = 'T0101'
GROUP BY tc.teaching_class_id
ORDER BY sem.semester_name;

-- 7. 教师查看某教学班学生名单
SELECT
    tc.teaching_class_id,
    c.course_name,
    s.student_id,
    s.student_name,
    ci.class_name,
    s.phone
FROM course_selection cs
JOIN student s ON cs.student_id = s.student_id
JOIN class_info ci ON s.class_id = ci.class_id
JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
JOIN course c ON tc.course_id = c.course_id
WHERE cs.teaching_class_id = 1
  AND cs.selection_status = '已选'
ORDER BY s.student_id;

-- 8. 教师录入或修改成绩
INSERT INTO score (
    student_id, teaching_class_id, usual_score, exam_score, is_submitted
) VALUES
('20240001', 3, 85, 90, 1)
ON DUPLICATE KEY UPDATE
    usual_score = VALUES(usual_score),
    exam_score = VALUES(exam_score),
    is_submitted = VALUES(is_submitted),
    entered_at = NOW();

-- 9. 管理员查看每门课选课人数统计
SELECT
    sem.semester_name,
    c.course_name,
    tc.teaching_class_id,
    t.teacher_name,
    tc.max_students,
    tc.selected_count
FROM teaching_class tc
JOIN semester sem ON tc.semester_id = sem.semester_id
JOIN course c ON tc.course_id = c.course_id
JOIN teacher t ON tc.teacher_id = t.teacher_id
ORDER BY sem.semester_name, c.course_name;

-- 10. 管理员查看各院系开课数量统计
SELECT
    d.department_name,
    COUNT(tc.teaching_class_id) AS teaching_class_count
FROM teaching_class tc
JOIN course c ON tc.course_id = c.course_id
JOIN department d ON c.department_id = d.department_id
GROUP BY d.department_id, d.department_name
ORDER BY teaching_class_count DESC;

-- 11. 管理员查看课程平均分、最高分、最低分
SELECT
    c.course_name,
    ROUND(AVG(sc.final_score), 2) AS avg_final_score,
    MAX(sc.final_score) AS max_final_score,
    MIN(sc.final_score) AS min_final_score
FROM score sc
JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
JOIN course c ON tc.course_id = c.course_id
GROUP BY c.course_id, c.course_name
ORDER BY avg_final_score DESC;

-- 12. 管理员查看学生成绩分布统计
SELECT
    c.course_name,
    SUM(CASE WHEN sc.final_score >= 90 THEN 1 ELSE 0 END) AS excellent_count,
    SUM(CASE WHEN sc.final_score >= 80 AND sc.final_score < 90 THEN 1 ELSE 0 END) AS good_count,
    SUM(CASE WHEN sc.final_score >= 70 AND sc.final_score < 80 THEN 1 ELSE 0 END) AS medium_count,
    SUM(CASE WHEN sc.final_score >= 60 AND sc.final_score < 70 THEN 1 ELSE 0 END) AS pass_count,
    SUM(CASE WHEN sc.final_score < 60 THEN 1 ELSE 0 END) AS fail_count
FROM score sc
JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
JOIN course c ON tc.course_id = c.course_id
GROUP BY c.course_id, c.course_name;

-- 13. 管理员查看教师工作量统计
SELECT
    t.teacher_id,
    t.teacher_name,
    COUNT(tc.teaching_class_id) AS class_count,
    SUM(c.total_hours) AS total_hours,
    SUM(c.credit) AS total_credits
FROM teaching_class tc
JOIN teacher t ON tc.teacher_id = t.teacher_id
JOIN course c ON tc.course_id = c.course_id
GROUP BY t.teacher_id, t.teacher_name
ORDER BY total_hours DESC;

-- 14. 管理员查看学生学分完成情况
SELECT
    s.student_id,
    s.student_name,
    ROUND(SUM(CASE WHEN sc.final_score >= 60 THEN c.credit ELSE 0 END), 1) AS earned_credits,
    ROUND(SUM(c.credit), 1) AS attempted_credits
FROM score sc
JOIN student s ON sc.student_id = s.student_id
JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
JOIN course c ON tc.course_id = c.course_id
GROUP BY s.student_id, s.student_name
ORDER BY earned_credits DESC;

-- 15. 管理员查看操作日志
SELECT
    l.log_id,
    u.username,
    u.real_name,
    l.operation_type,
    l.operation_content,
    l.operation_time
FROM operation_log l
LEFT JOIN sys_user u ON l.user_id = u.user_id
ORDER BY l.operation_time DESC;
