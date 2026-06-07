-- 0607 同步学生和教师登录账号
-- 功能：确保 student、teacher 表中所有人员都存在 sys_user 账号和角色关系，默认密码为 123456。

INSERT INTO sys_user (username, password, real_name, user_type, ref_id, is_enabled)
SELECT s.student_id, '123456', s.student_name, '学生', s.student_id, 1
FROM student s
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user u
    WHERE u.username = s.student_id
);

INSERT INTO sys_user (username, password, real_name, user_type, ref_id, is_enabled)
SELECT t.teacher_id, '123456', t.teacher_name, '教师', t.teacher_id, 1
FROM teacher t
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user u
    WHERE u.username = t.teacher_id
);

UPDATE sys_user u
JOIN student s ON u.username = s.student_id
SET u.password = '123456',
    u.real_name = s.student_name,
    u.user_type = '学生',
    u.ref_id = s.student_id,
    u.is_enabled = 1;

UPDATE sys_user u
JOIN teacher t ON u.username = t.teacher_id
SET u.password = '123456',
    u.real_name = t.teacher_name,
    u.user_type = '教师',
    u.ref_id = t.teacher_id,
    u.is_enabled = 1;

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN student s ON u.username = s.student_id
JOIN sys_role r ON r.role_code = 'STUDENT'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_role ur
    WHERE ur.user_id = u.user_id
      AND ur.role_id = r.role_id
);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN teacher t ON u.username = t.teacher_id
JOIN sys_role r ON r.role_code = 'TEACHER'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_role ur
    WHERE ur.user_id = u.user_id
      AND ur.role_id = r.role_id
);
