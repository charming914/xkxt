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

    @NotNull(message = "星期不能为空")
    private Integer weekDay;

    @NotBlank(message = "节次不能为空")
    private String sectionDesc;

    @NotBlank(message = "周次不能为空")
    private String weekRange;

    @NotNull(message = "容量不能为空")
    private Integer maxStudents;

    @NotBlank(message = "状态不能为空")
    private String status;
}
