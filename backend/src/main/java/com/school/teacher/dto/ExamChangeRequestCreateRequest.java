package com.school.teacher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExamChangeRequestCreateRequest {

    @NotNull(message = "考试安排编号不能为空")
    private Long examId;

    @NotBlank(message = "申请后考试日期不能为空")
    private String requestedExamDate;

    @NotBlank(message = "申请后开始时间不能为空")
    private String requestedStartTime;

    @NotBlank(message = "申请后结束时间不能为空")
    private String requestedEndTime;

    @Size(max = 10, message = "教室编号长度不能超过10位")
    private String requestedClassroomId;

    @NotBlank(message = "申请原因不能为空")
    @Size(max = 200, message = "申请原因长度不能超过200位")
    private String requestReason;
}
