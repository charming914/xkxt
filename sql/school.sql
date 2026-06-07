DROP DATABASE IF EXISTS school;
CREATE DATABASE school DEFAULT CHARACTER SET utf8mb4;
USE school;

SET NAMES utf8mb4;

CREATE TABLE department (
    department_id VARCHAR(10) NOT NULL COMMENT '院系编号',
    department_name VARCHAR(50) NOT NULL COMMENT '院系名称',
    office_address VARCHAR(100) NULL COMMENT '办公地点',
    contact_phone VARCHAR(20) NULL COMMENT '联系电话',
    PRIMARY KEY (department_id),
    UNIQUE KEY uk_department_name (department_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院系表';

CREATE TABLE major (
    major_id VARCHAR(10) NOT NULL COMMENT '专业编号',
    major_name VARCHAR(50) NOT NULL COMMENT '专业名称',
    department_id VARCHAR(10) NOT NULL COMMENT '所属院系',
    PRIMARY KEY (major_id),
    UNIQUE KEY uk_major_name (major_name),
    CONSTRAINT fk_major_department
        FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业表';

CREATE TABLE class_info (
    class_id VARCHAR(10) NOT NULL COMMENT '班级编号',
    class_name VARCHAR(50) NOT NULL COMMENT '班级名称',
    grade_year CHAR(4) NOT NULL COMMENT '年级',
    major_id VARCHAR(10) NOT NULL COMMENT '专业编号',
    counselor_name VARCHAR(20) NULL COMMENT '辅导员',
    PRIMARY KEY (class_id),
    UNIQUE KEY uk_class_name (class_name),
    CONSTRAINT fk_class_major
        FOREIGN KEY (major_id) REFERENCES major(major_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政班表';

CREATE TABLE student (
    student_id VARCHAR(12) NOT NULL COMMENT '学号',
    student_name VARCHAR(20) NOT NULL COMMENT '姓名',
    gender CHAR(1) NOT NULL COMMENT '性别',
    birth_date DATE NULL COMMENT '出生日期',
    hometown VARCHAR(50) NULL COMMENT '籍贯',
    phone VARCHAR(20) NULL COMMENT '手机号',
    email VARCHAR(50) NULL COMMENT '邮箱',
    enrollment_year CHAR(4) NOT NULL COMMENT '入学年份',
    department_id VARCHAR(10) NOT NULL COMMENT '院系编号',
    major_id VARCHAR(10) NOT NULL COMMENT '专业编号',
    class_id VARCHAR(10) NOT NULL COMMENT '班级编号',
    status VARCHAR(20) NOT NULL DEFAULT '正常' COMMENT '学籍状态',
    PRIMARY KEY (student_id),
    CONSTRAINT fk_student_department
        FOREIGN KEY (department_id) REFERENCES department(department_id),
    CONSTRAINT fk_student_major
        FOREIGN KEY (major_id) REFERENCES major(major_id),
    CONSTRAINT fk_student_class
        FOREIGN KEY (class_id) REFERENCES class_info(class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

CREATE TABLE teacher (
    teacher_id VARCHAR(12) NOT NULL COMMENT '工号',
    teacher_name VARCHAR(20) NOT NULL COMMENT '姓名',
    gender CHAR(1) NOT NULL COMMENT '性别',
    birth_date DATE NULL COMMENT '出生日期',
    title_name VARCHAR(20) NOT NULL COMMENT '职称',
    education VARCHAR(20) NULL COMMENT '学历',
    phone VARCHAR(20) NULL COMMENT '手机号',
    email VARCHAR(50) NULL COMMENT '邮箱',
    department_id VARCHAR(10) NOT NULL COMMENT '所属院系',
    status VARCHAR(20) NOT NULL DEFAULT '在职' COMMENT '教师状态',
    PRIMARY KEY (teacher_id),
    CONSTRAINT fk_teacher_department
        FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

CREATE TABLE sys_role (
    role_id INT NOT NULL AUTO_INCREMENT COMMENT '角色编号',
    role_code VARCHAR(30) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(30) NOT NULL COMMENT '角色名称',
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE sys_user (
    user_id INT NOT NULL AUTO_INCREMENT COMMENT '用户编号',
    username VARCHAR(30) NOT NULL COMMENT '登录账号',
    password VARCHAR(100) NOT NULL COMMENT '登录密码',
    real_name VARCHAR(20) NOT NULL COMMENT '真实姓名',
    user_type VARCHAR(20) NOT NULL COMMENT '用户类型',
    ref_id VARCHAR(12) NULL COMMENT '关联学生或教师编号',
    is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE sys_user_role (
    user_id INT NOT NULL COMMENT '用户编号',
    role_id INT NOT NULL COMMENT '角色编号',
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id) REFERENCES sys_role(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE semester (
    semester_id INT NOT NULL AUTO_INCREMENT COMMENT '学期编号',
    semester_name VARCHAR(30) NOT NULL COMMENT '学期名称',
    school_year VARCHAR(20) NOT NULL COMMENT '学年',
    term_code VARCHAR(10) NOT NULL COMMENT '学期代码',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    course_select_start DATETIME NOT NULL COMMENT '选课开始时间',
    course_select_end DATETIME NOT NULL COMMENT '选课结束时间',
    score_submit_start DATETIME NULL COMMENT '成绩提交开始时间',
    score_submit_end DATETIME NULL COMMENT '成绩提交结束时间',
    is_current TINYINT NOT NULL DEFAULT 0 COMMENT '是否当前学期',
    PRIMARY KEY (semester_id),
    UNIQUE KEY uk_semester_name (semester_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期表';

CREATE TABLE course (
    course_id VARCHAR(12) NOT NULL COMMENT '课程编号',
    course_name VARCHAR(50) NOT NULL COMMENT '课程名称',
    course_type VARCHAR(20) NOT NULL COMMENT '课程类型',
    credit DECIMAL(4,1) NOT NULL COMMENT '学分',
    total_hours INT NOT NULL COMMENT '总学时',
    assessment_method VARCHAR(20) NOT NULL DEFAULT '考试' COMMENT '考核方式',
    department_id VARCHAR(10) NOT NULL COMMENT '开课院系',
    is_required TINYINT NOT NULL DEFAULT 1 COMMENT '是否必修',
    description VARCHAR(200) NULL COMMENT '课程说明',
    PRIMARY KEY (course_id),
    UNIQUE KEY uk_course_name (course_name),
    CONSTRAINT fk_course_department
        FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

CREATE TABLE classroom (
    classroom_id VARCHAR(10) NOT NULL COMMENT '教室编号',
    building_name VARCHAR(30) NOT NULL COMMENT '教学楼',
    room_name VARCHAR(30) NOT NULL COMMENT '教室名称',
    capacity INT NOT NULL COMMENT '容量',
    PRIMARY KEY (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室表';

CREATE TABLE teaching_class (
    teaching_class_id INT NOT NULL AUTO_INCREMENT COMMENT '教学班编号',
    semester_id INT NOT NULL COMMENT '学期编号',
    course_id VARCHAR(12) NOT NULL COMMENT '课程编号',
    teacher_id VARCHAR(12) NOT NULL COMMENT '任课教师工号',
    classroom_id VARCHAR(10) NULL COMMENT '教室编号',
    course_start_date DATE NULL COMMENT '课程实际开课日期',
    week_day TINYINT NOT NULL COMMENT '星期几',
    section_desc VARCHAR(20) NOT NULL COMMENT '节次',
    week_range VARCHAR(30) NOT NULL COMMENT '周次范围',
    max_students INT NOT NULL DEFAULT 60 COMMENT '容量上限',
    selected_count INT NOT NULL DEFAULT 0 COMMENT '已选人数',
    status VARCHAR(20) NOT NULL DEFAULT '未开始' COMMENT '教学班状态',
    PRIMARY KEY (teaching_class_id),
    CONSTRAINT fk_teaching_class_semester
        FOREIGN KEY (semester_id) REFERENCES semester(semester_id),
    CONSTRAINT fk_teaching_class_course
        FOREIGN KEY (course_id) REFERENCES course(course_id),
    CONSTRAINT fk_teaching_class_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id),
    CONSTRAINT fk_teaching_class_classroom
        FOREIGN KEY (classroom_id) REFERENCES classroom(classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学班表';

CREATE TABLE course_selection (
    selection_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '选课记录编号',
    student_id VARCHAR(12) NOT NULL COMMENT '学号',
    teaching_class_id INT NOT NULL COMMENT '教学班编号',
    selection_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    selection_status VARCHAR(20) NOT NULL DEFAULT '已选' COMMENT '选课状态',
    PRIMARY KEY (selection_id),
    UNIQUE KEY uk_student_teaching_class (student_id, teaching_class_id),
    CONSTRAINT fk_selection_student
        FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_selection_teaching_class
        FOREIGN KEY (teaching_class_id) REFERENCES teaching_class(teaching_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课表';

CREATE TABLE course_selection_request (
    request_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '补选申请编号',
    student_id VARCHAR(12) NOT NULL COMMENT '学号',
    teaching_class_id INT NOT NULL COMMENT '教学班编号',
    request_reason VARCHAR(200) NOT NULL COMMENT '申请原因',
    request_status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '申请状态',
    review_comment VARCHAR(200) NULL COMMENT '审核备注',
    reviewed_by INT NULL COMMENT '审核人',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (request_id),
    CONSTRAINT fk_request_student
        FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_request_teaching_class
        FOREIGN KEY (teaching_class_id) REFERENCES teaching_class(teaching_class_id),
    CONSTRAINT fk_request_user
        FOREIGN KEY (reviewed_by) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补选申请表';

CREATE TABLE score (
    score_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '成绩编号',
    student_id VARCHAR(12) NOT NULL COMMENT '学号',
    teaching_class_id INT NOT NULL COMMENT '教学班编号',
    usual_score DECIMAL(5,2) NULL COMMENT '平时成绩',
    exam_score DECIMAL(5,2) NULL COMMENT '考试成绩',
    final_score DECIMAL(5,2) NULL COMMENT '总评成绩',
    gpa_point DECIMAL(3,1) NULL COMMENT '绩点',
    is_submitted TINYINT NOT NULL DEFAULT 0 COMMENT '是否提交',
    entered_at DATETIME NULL COMMENT '录入时间',
    PRIMARY KEY (score_id),
    UNIQUE KEY uk_score_student_class (student_id, teaching_class_id),
    CONSTRAINT fk_score_student
        FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_score_teaching_class
        FOREIGN KEY (teaching_class_id) REFERENCES teaching_class(teaching_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';

CREATE TABLE score_review_request (
    request_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '成绩复核申请编号',
    student_id VARCHAR(12) NOT NULL COMMENT '学号',
    score_id BIGINT NOT NULL COMMENT '成绩编号',
    request_reason VARCHAR(200) NOT NULL COMMENT '复核原因',
    request_status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '申请状态',
    review_comment VARCHAR(200) NULL COMMENT '审核备注',
    reviewed_by INT NULL COMMENT '审核管理员用户编号',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (request_id),
    KEY idx_score_review_student (student_id),
    KEY idx_score_review_score (score_id),
    KEY idx_score_review_status (request_status),
    CONSTRAINT fk_score_review_student
        FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_score_review_score
        FOREIGN KEY (score_id) REFERENCES score(score_id),
    CONSTRAINT fk_score_review_admin
        FOREIGN KEY (reviewed_by) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩复核申请表';

CREATE TABLE operation_log (
    log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志编号',
    user_id INT NULL COMMENT '操作人',
    operation_type VARCHAR(30) NOT NULL COMMENT '操作类型',
    operation_content VARCHAR(200) NOT NULL COMMENT '操作内容',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (log_id),
    CONSTRAINT fk_log_user
        FOREIGN KEY (user_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

INSERT INTO department (department_id, department_name, office_address, contact_phone) VALUES
('D01', '计算机学院', '上大东校区三号楼', '65347567'),
('D02', '通信学院', '上大东校区二号楼', '65341234'),
('D03', '材料学院', '上大东校区四号楼', '65347890');

INSERT INTO major (major_id, major_name, department_id) VALUES
('M01', '计算机科学与技术', 'D01'),
('M02', '软件工程', 'D01'),
('M03', '通信工程', 'D02'),
('M04', '材料科学与工程', 'D03');

INSERT INTO class_info (class_id, class_name, grade_year, major_id, counselor_name) VALUES
('C2401', '计科2401班', '2024', 'M01', '王老师'),
('C2402', '软工2401班', '2024', 'M02', '李老师'),
('C2403', '通信2401班', '2024', 'M03', '周老师');

INSERT INTO student (
    student_id, student_name, gender, birth_date, hometown, phone, email,
    enrollment_year, department_id, major_id, class_id, status
) VALUES
('20240001', '李明', '男', '2005-03-06', '上海', '13613005486', '20240001@school.edu.cn', '2024', 'D02', 'M03', 'C2403', '正常'),
('20240002', '刘晓明', '男', '2004-12-08', '安徽', '18913457890', '20240002@school.edu.cn', '2024', 'D01', 'M01', 'C2401', '正常'),
('20240003', '张颖', '女', '2005-01-05', '江苏', '18826490423', '20240003@school.edu.cn', '2024', 'D01', 'M01', 'C2401', '正常'),
('20240004', '刘晶晶', '女', '2005-11-06', '上海', '13331934111', '20240004@school.edu.cn', '2024', 'D01', 'M02', 'C2402', '正常'),
('20240005', '刘成刚', '男', '2004-06-07', '上海', '18015872567', '20240005@school.edu.cn', '2024', 'D01', 'M02', 'C2402', '正常');

INSERT INTO teacher (
    teacher_id, teacher_name, gender, birth_date, title_name, education, phone, email, department_id, status
) VALUES
('T0101', '陈迪茂', '男', '1973-03-06', '副教授', '硕士', '13800010001', 't0101@school.edu.cn', 'D01', '在职'),
('T0102', '马小红', '女', '1972-12-08', '讲师', '硕士', '13800010002', 't0102@school.edu.cn', 'D01', '在职'),
('T0201', '张心颖', '女', '1960-01-05', '教授', '博士', '13800010003', 't0201@school.edu.cn', 'D02', '在职'),
('T0103', '吴宝钢', '男', '1980-11-06', '讲师', '硕士', '13800010004', 't0103@school.edu.cn', 'D01', '在职');

INSERT INTO sys_role (role_code, role_name) VALUES
('ADMIN', '系统管理员'),
('TEACHER', '教师'),
('STUDENT', '学生');

INSERT INTO sys_user (username, password, real_name, user_type, ref_id, is_enabled) VALUES
('admin', '123456', '系统管理员', '管理员', NULL, 1),
('20240001', '123456', '李明', '学生', '20240001', 1),
('20240002', '123456', '刘晓明', '学生', '20240002', 1),
('20240003', '123456', '张颖', '学生', '20240003', 1),
('T0101', '123456', '陈迪茂', '教师', 'T0101', 1),
('T0102', '123456', '马小红', '教师', 'T0102', 1),
('T0201', '123456', '张心颖', '教师', 'T0201', 1),
('T0103', '123456', '吴宝钢', '教师', 'T0103', 1);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN sys_role r ON (
    (u.username = 'admin' AND r.role_code = 'ADMIN') OR
    (u.user_type = '学生' AND r.role_code = 'STUDENT') OR
    (u.user_type = '教师' AND r.role_code = 'TEACHER')
);

INSERT INTO semester (
    semester_name, school_year, term_code, start_date, end_date,
    course_select_start, course_select_end, score_submit_start, score_submit_end, is_current
) VALUES
('2025-2026-1', '2025-2026', '1', '2025-09-01', '2026-01-20', '2025-08-20 08:00:00', '2025-09-10 23:59:59', '2026-01-01 08:00:00', '2026-01-31 23:59:59', 0),
('2025-2026-2', '2025-2026', '2', '2026-02-23', '2026-07-10', '2026-02-10 08:00:00', '2026-03-05 23:59:59', '2026-06-01 08:00:00', '2026-07-20 23:59:59', 1);

INSERT INTO course (
    course_id, course_name, course_type, credit, total_hours,
    assessment_method, department_id, is_required, description
) VALUES
('08305001', '离散数学', '专业基础课', 4.0, 40, '考试', 'D01', 1, '计算机专业基础课程'),
('08305002', '数据库原理', '专业核心课', 4.0, 50, '考试', 'D01', 1, '数据库系统基础与应用'),
('08305003', '数据结构', '专业核心课', 4.0, 50, '考试', 'D01', 1, '数据结构与算法基础'),
('08305004', '系统结构', '专业选修课', 3.0, 48, '考查', 'D01', 0, '计算机系统结构入门'),
('08302001', '通信学', '专业核心课', 3.0, 30, '考试', 'D02', 1, '通信工程专业基础课程'),
('08301001', '分子物理学', '专业基础课', 4.0, 40, '考试', 'D03', 1, '材料学院基础课程');

INSERT INTO classroom (classroom_id, building_name, room_name, capacity) VALUES
('R101', '一教', '101', 80),
('R102', '一教', '102', 60),
('R201', '二教', '201', 100),
('LAB1', '实验楼', '数据库实验室1', 50);

INSERT INTO teaching_class (
    semester_id, course_id, teacher_id, classroom_id,
    course_start_date, week_day, section_desc, week_range, max_students, selected_count, status
) VALUES
(2, '08305002', 'T0101', 'LAB1', '2026-03-01', 3, '1-2节', '1-16周', 50, 0, '选课中'),
(2, '08305003', 'T0102', 'R102', '2026-03-01', 5, '5-6节', '1-16周', 60, 0, '选课中'),
(2, '08302001', 'T0201', 'R201', '2026-03-01', 1, '5-6节', '1-16周', 80, 0, '选课中'),
(2, '08305001', 'T0103', 'R101', '2026-03-01', 2, '1-2节', '1-16周', 60, 0, '选课中');

INSERT INTO course_selection (student_id, teaching_class_id, selection_time, selection_status) VALUES
('20240001', 3, NOW(), '已选'),
('20240002', 1, NOW(), '已选'),
('20240002', 2, NOW(), '已选'),
('20240003', 1, NOW(), '已选'),
('20240003', 4, NOW(), '已选');

UPDATE teaching_class tc
SET selected_count = (
    SELECT COUNT(*)
    FROM course_selection cs
    WHERE cs.teaching_class_id = tc.teaching_class_id
      AND cs.selection_status = '已选'
);

INSERT INTO score (
    student_id, teaching_class_id, usual_score, exam_score, final_score, gpa_point, is_submitted, entered_at
) VALUES
('20240002', 1, 82, 86, 84.40, 3.3, 1, NOW()),
('20240002', 2, 88, 90, 89.20, 3.7, 1, NOW()),
('20240003', 1, 75, 80, 78.00, 3.0, 1, NOW());

DELIMITER $$

CREATE TRIGGER trg_score_before_insert
BEFORE INSERT ON score
FOR EACH ROW
BEGIN
    IF NEW.usual_score IS NOT NULL AND (NEW.usual_score < 0 OR NEW.usual_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '平时成绩必须在0到100之间';
    END IF;

    IF NEW.exam_score IS NOT NULL AND (NEW.exam_score < 0 OR NEW.exam_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考试成绩必须在0到100之间';
    END IF;

    IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN
        SET NEW.final_score = ROUND(NEW.usual_score * 0.4 + NEW.exam_score * 0.6, 2);
        SET NEW.gpa_point = CASE
            WHEN NEW.final_score >= 90 THEN 4.0
            WHEN NEW.final_score >= 85 THEN 3.7
            WHEN NEW.final_score >= 82 THEN 3.3
            WHEN NEW.final_score >= 78 THEN 3.0
            WHEN NEW.final_score >= 75 THEN 2.7
            WHEN NEW.final_score >= 72 THEN 2.3
            WHEN NEW.final_score >= 68 THEN 2.0
            WHEN NEW.final_score >= 66 THEN 1.7
            WHEN NEW.final_score >= 64 THEN 1.5
            WHEN NEW.final_score >= 60 THEN 1.0
            ELSE 0.0
        END;
    END IF;

    IF NEW.entered_at IS NULL THEN
        SET NEW.entered_at = NOW();
    END IF;
END$$

CREATE TRIGGER trg_score_before_update
BEFORE UPDATE ON score
FOR EACH ROW
BEGIN
    IF NEW.usual_score IS NOT NULL AND (NEW.usual_score < 0 OR NEW.usual_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '平时成绩必须在0到100之间';
    END IF;

    IF NEW.exam_score IS NOT NULL AND (NEW.exam_score < 0 OR NEW.exam_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考试成绩必须在0到100之间';
    END IF;

    IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN
        SET NEW.final_score = ROUND(NEW.usual_score * 0.4 + NEW.exam_score * 0.6, 2);
        SET NEW.gpa_point = CASE
            WHEN NEW.final_score >= 90 THEN 4.0
            WHEN NEW.final_score >= 85 THEN 3.7
            WHEN NEW.final_score >= 82 THEN 3.3
            WHEN NEW.final_score >= 78 THEN 3.0
            WHEN NEW.final_score >= 75 THEN 2.7
            WHEN NEW.final_score >= 72 THEN 2.3
            WHEN NEW.final_score >= 68 THEN 2.0
            WHEN NEW.final_score >= 66 THEN 1.7
            WHEN NEW.final_score >= 64 THEN 1.5
            WHEN NEW.final_score >= 60 THEN 1.0
            ELSE 0.0
        END;
    END IF;
END$$

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
    JOIN teaching_class t1 ON cs.teaching_class_id = t1.teaching_class_id
    JOIN teaching_class t2 ON t2.teaching_class_id = p_teaching_class_id
    WHERE cs.student_id = p_student_id
      AND cs.selection_status = '已选'
      AND t1.semester_id = t2.semester_id
      AND t1.week_day = t2.week_day
      -- 按节次区间是否重叠判断冲突（兼容 "1-2节" 与 "1-4节" 这类跨段课程）
      AND CAST(SUBSTRING_INDEX(t1.section_desc, '-', 1) AS UNSIGNED)
          <= CAST(REPLACE(SUBSTRING_INDEX(t2.section_desc, '-', -1), '节', '') AS UNSIGNED)
      AND CAST(SUBSTRING_INDEX(t2.section_desc, '-', 1) AS UNSIGNED)
          <= CAST(REPLACE(SUBSTRING_INDEX(t1.section_desc, '-', -1), '节', '') AS UNSIGNED);

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

CREATE OR REPLACE VIEW v_student_timetable AS
SELECT
    cs.student_id,
    s.student_name,
    sem.semester_name,
    c.course_name,
    t.teacher_name,
    tc.week_day,
    tc.section_desc,
    tc.week_range,
    cl.building_name,
    cl.room_name
FROM course_selection cs
JOIN student s ON cs.student_id = s.student_id
JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
JOIN semester sem ON tc.semester_id = sem.semester_id
JOIN course c ON tc.course_id = c.course_id
JOIN teacher t ON tc.teacher_id = t.teacher_id
LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
WHERE cs.selection_status = '已选';

CREATE OR REPLACE VIEW v_student_score AS
SELECT
    sc.score_id,
    sc.student_id,
    s.student_name,
    sem.semester_id,
    sem.semester_name,
    tc.teaching_class_id,
    c.course_id,
    c.course_name,
    c.course_type,
    c.credit,
    t.teacher_name,
    sc.usual_score,
    sc.exam_score,
    sc.final_score,
    sc.gpa_point,
    CASE
        WHEN sc.final_score IS NULL THEN NULL
        WHEN sc.final_score >= 90 THEN 'A'
        WHEN sc.final_score >= 85 THEN 'A-'
        WHEN sc.final_score >= 82 THEN 'B+'
        WHEN sc.final_score >= 78 THEN 'B'
        WHEN sc.final_score >= 75 THEN 'B-'
        WHEN sc.final_score >= 72 THEN 'C+'
        WHEN sc.final_score >= 68 THEN 'C'
        WHEN sc.final_score >= 66 THEN 'C-'
        WHEN sc.final_score >= 64 THEN 'D'
        WHEN sc.final_score >= 60 THEN 'D-'
        ELSE 'F'
    END AS grade_level,
    sc.is_submitted,
    sc.entered_at
FROM score sc
JOIN student s ON sc.student_id = s.student_id
JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
JOIN semester sem ON tc.semester_id = sem.semester_id
JOIN course c ON tc.course_id = c.course_id
JOIN teacher t ON tc.teacher_id = t.teacher_id;
