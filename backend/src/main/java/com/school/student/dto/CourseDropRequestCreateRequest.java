package com.school.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseDropRequestCreateRequest {

    @NotNull(message = "教学班编号不能为空")
    private Integer teachingClassId;

    @NotBlank(message = "退课原因不能为空")
    private String requestReason;
}

