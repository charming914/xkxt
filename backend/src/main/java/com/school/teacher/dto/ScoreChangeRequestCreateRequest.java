package com.school.teacher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScoreChangeRequestCreateRequest {

    @NotNull(message = "成绩编号不能为空")
    private Long scoreId;

    @NotBlank(message = "申请原因不能为空")
    @Size(max = 200, message = "申请原因长度不能超过200位")
    private String requestReason;
}
