package com.school.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 退课申请审核请求（教师端、管理员端通用）。
 */
@Data
public class CourseDropReviewRequest {

    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String reviewComment;
}
