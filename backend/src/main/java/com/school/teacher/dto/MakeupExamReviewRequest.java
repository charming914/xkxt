package com.school.teacher.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MakeupExamReviewRequest {

    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String reviewComment;
}
