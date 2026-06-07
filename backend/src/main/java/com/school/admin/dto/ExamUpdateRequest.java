package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExamUpdateRequest {

    @NotBlank(message = "考试日期不能为空")
    private String examDate;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @Size(max = 10, message = "教室编号长度不能超过10位")
    private String classroomId;
}
