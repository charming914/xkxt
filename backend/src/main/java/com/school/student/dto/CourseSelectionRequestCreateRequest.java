package com.school.student.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseSelectionRequestCreateRequest {

    @NotNull(message = "教学班编号不能为空")
    private Integer teachingClassId;

    @NotBlank(message = "申请原因不能为空")
    private String requestReason;
}
