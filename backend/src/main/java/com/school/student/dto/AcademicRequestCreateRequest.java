package com.school.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcademicRequestCreateRequest {

    @NotNull(message = "成绩编号不能为空")
    private Long scoreId;

    @NotBlank(message = "申请原因不能为空")
    private String requestReason;
}
