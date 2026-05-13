package com.school.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaUpgradeRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS course_selection_request (
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补选申请表'
            """);
    }
}
