package com.school.teacher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScheduleAdjustRequestCreateRequest {

    @NotNull(message = "教学班编号不能为空")
    private Integer teachingClassId;

    @NotNull(message = "调整后星期不能为空")
    @Min(value = 1, message = "星期必须在1到7之间")
    @Max(value = 7, message = "星期必须在1到7之间")
    private Integer requestedWeekDay;

    @NotBlank(message = "调整后节次不能为空")
    @Size(max = 20, message = "节次长度不能超过20位")
    private String requestedSectionDesc;

    @NotBlank(message = "调整后周次不能为空")
    @Size(max = 30, message = "周次长度不能超过30位")
    private String requestedWeekRange;

    @NotBlank(message = "调整后教室不能为空")
    @Size(max = 10, message = "教室编号长度不能超过10位")
    private String requestedClassroomId;

    @NotBlank(message = "申请原因不能为空")
    @Size(max = 200, message = "申请原因长度不能超过200位")
    private String requestReason;
}
