package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScoreSubmitSettingRequest {

    @NotBlank(message = "成绩提交开始时间不能为空")
    private String scoreSubmitStart;

    @NotBlank(message = "成绩提交结束时间不能为空")
    private String scoreSubmitEnd;
}
