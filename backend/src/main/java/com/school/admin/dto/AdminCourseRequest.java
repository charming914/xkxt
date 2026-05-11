package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCourseRequest {

    @NotBlank(message = "课程编号不能为空")
    private String courseId;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    @NotBlank(message = "课程类型不能为空")
    private String courseType;

    @NotNull(message = "学分不能为空")
    private Double credit;

    @NotNull(message = "总学时不能为空")
    private Integer totalHours;

    @NotBlank(message = "考核方式不能为空")
    private String assessmentMethod;

    @NotBlank(message = "开课院系不能为空")
    private String departmentId;

    @NotNull(message = "是否必修不能为空")
    private Integer isRequired;

    private String description;
}
