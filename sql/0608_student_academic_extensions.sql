-- 学生端扩展：选课计划、补考/重修申请、培养方案要求

CREATE TABLE IF NOT EXISTS student_course_plan (
    plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    teaching_class_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_course_plan (student_id, teaching_class_id),
    KEY idx_student_course_plan_student (student_id),
    KEY idx_student_course_plan_class (teaching_class_id)
);

CREATE TABLE IF NOT EXISTS makeup_exam_request (
    request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    score_id BIGINT NOT NULL,
    request_reason VARCHAR(500) NOT NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT '待审核',
    result_status VARCHAR(20) NOT NULL DEFAULT '未安排',
    review_comment VARCHAR(500) NULL,
    reviewed_by INT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_makeup_student (student_id),
    KEY idx_makeup_score (score_id),
    KEY idx_makeup_status (request_status)
);

CREATE TABLE IF NOT EXISTS course_retake_request (
    request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    score_id BIGINT NOT NULL,
    request_reason VARCHAR(500) NOT NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT '待审核',
    review_comment VARCHAR(500) NULL,
    reviewed_by INT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_retake_student (student_id),
    KEY idx_retake_score (score_id),
    KEY idx_retake_status (request_status)
);

CREATE TABLE IF NOT EXISTS cultivation_plan_requirement (
    requirement_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_year INT NOT NULL,
    major_id VARCHAR(20) NOT NULL,
    course_type VARCHAR(50) NOT NULL,
    required_credit DECIMAL(5,1) NOT NULL DEFAULT 0,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_requirement (enrollment_year, major_id, course_type),
    KEY idx_plan_major_year (major_id, enrollment_year)
);
