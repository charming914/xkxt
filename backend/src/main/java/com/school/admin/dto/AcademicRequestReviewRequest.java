package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AcademicRequestReviewRequest {

    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String reviewComment;

    private String resultStatus;
}
