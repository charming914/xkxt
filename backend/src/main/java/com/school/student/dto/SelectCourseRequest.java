package com.school.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectCourseRequest {

    @NotBlank(message = "学号不能为空")
    private String studentId;

    @NotNull(message = "教学班编号不能为空")
    private Integer teachingClassId;
}
