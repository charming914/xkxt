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

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE semester ADD COLUMN makeup_request_start DATETIME NULL COMMENT ''补考申请开放开始时间''',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'semester'
      AND COLUMN_NAME = 'makeup_request_start'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE semester ADD COLUMN makeup_request_end DATETIME NULL COMMENT ''补考申请开放结束时间''',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'semester'
      AND COLUMN_NAME = 'makeup_request_end'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE semester
SET makeup_request_start = DATE_ADD(end_date, INTERVAL 1 DAY),
    makeup_request_end = DATE_ADD(end_date, INTERVAL 8 DAY)
WHERE makeup_request_start IS NULL
  AND makeup_request_end IS NULL;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE teaching_class ADD COLUMN is_retake_class TINYINT NOT NULL DEFAULT 0 COMMENT ''是否重修班：0否，1是''',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'teaching_class'
      AND COLUMN_NAME = 'is_retake_class'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS makeup_exam_request (
    request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    score_id BIGINT NOT NULL,
    request_reason VARCHAR(500) NOT NULL,
    requested_exam_time DATETIME NULL COMMENT '学生申请补考时间',
    confirmed_exam_time DATETIME NULL COMMENT '教师确认补考时间',
    request_status VARCHAR(20) NOT NULL DEFAULT '待教师确认',
    teacher_status VARCHAR(20) NOT NULL DEFAULT '待确认',
    teacher_comment VARCHAR(500) NULL,
    teacher_reviewed_by VARCHAR(20) NULL,
    teacher_reviewed_at DATETIME NULL,
    admin_status VARCHAR(20) NOT NULL DEFAULT '待安排',
    admin_comment VARCHAR(500) NULL,
    admin_reviewed_by INT NULL,
    admin_reviewed_at DATETIME NULL,
    classroom_id VARCHAR(20) NULL,
    result_status VARCHAR(20) NOT NULL DEFAULT '未安排',
    review_comment VARCHAR(500) NULL,
    reviewed_by INT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_makeup_student (student_id),
    KEY idx_makeup_score (score_id),
    KEY idx_makeup_status (request_status),
    KEY idx_makeup_teacher_status (teacher_status),
    KEY idx_makeup_admin_status (admin_status),
    KEY idx_makeup_room_time (classroom_id, confirmed_exam_time)
);

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN requested_exam_time DATETIME NULL COMMENT ''学生申请补考时间''',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'requested_exam_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN confirmed_exam_time DATETIME NULL COMMENT ''教师确认补考时间''',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'confirmed_exam_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN teacher_status VARCHAR(20) NOT NULL DEFAULT ''待确认''',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'teacher_status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN teacher_comment VARCHAR(500) NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'teacher_comment'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN teacher_reviewed_by VARCHAR(20) NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'teacher_reviewed_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN teacher_reviewed_at DATETIME NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'teacher_reviewed_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN admin_status VARCHAR(20) NOT NULL DEFAULT ''待安排''',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'admin_status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN admin_comment VARCHAR(500) NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'admin_comment'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN admin_reviewed_by INT NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'admin_reviewed_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN admin_reviewed_at DATETIME NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'admin_reviewed_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE makeup_exam_request ADD COLUMN classroom_id VARCHAR(20) NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'makeup_exam_request'
      AND COLUMN_NAME = 'classroom_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE makeup_exam_request
SET request_status = '待教师确认',
    teacher_status = '待确认',
    admin_status = '待安排'
WHERE request_status = '待审核';

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
