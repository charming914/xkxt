package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MakeupExamArrangeRequest {

    @NotBlank(message = "处理动作不能为空")
    private String action;

    private String reviewComment;

    private String classroomId;
}
