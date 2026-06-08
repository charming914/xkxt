package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseSelectionSettingRequest {

    @NotBlank(message = "选课开始时间不能为空")
    private String courseSelectStart;

    @NotBlank(message = "选课结束时间不能为空")
    private String courseSelectEnd;

    private String makeupRequestStart;

    private String makeupRequestEnd;
}
