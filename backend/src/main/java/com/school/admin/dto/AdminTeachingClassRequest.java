package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminTeachingClassRequest {

    @NotNull(message = "学期不能为空")
    private Integer semesterId;

    @NotNull(message = "开课日期不能为空")
    private LocalDate courseStartDate;

    @NotBlank(message = "课程不能为空")
    private String courseId;

    @NotBlank(message = "任课教师不能为空")
    private String teacherId;

    private String classroomId;

    @NotBlank(message = "周次不能为空")
    private String weekRange;

    @NotNull(message = "容量不能为空")
    private Integer maxStudents;

    private Integer isRetakeClass;

    /** 由数据库回填的自增主键，用于新增后生成上课时间。 */
    private Integer generatedId;
}
