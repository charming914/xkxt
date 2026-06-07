package com.school.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreReviewRequestCreateRequest {

    @NotNull(message = "成绩编号不能为空")
    private Long scoreId;

    @NotBlank(message = "复核原因不能为空")
    private String requestReason;
}
