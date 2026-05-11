package com.school.teacher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreInputRequest {

    @NotBlank(message = "学号不能为空")
    private String studentId;

    @NotNull(message = "教学班编号不能为空")
    private Integer teachingClassId;

    @NotNull(message = "平时成绩不能为空")
    @Min(value = 0, message = "平时成绩不能小于0")
    @Max(value = 100, message = "平时成绩不能大于100")
    private Double usualScore;

    @NotNull(message = "考试成绩不能为空")
    @Min(value = 0, message = "考试成绩不能小于0")
    @Max(value = 100, message = "考试成绩不能大于100")
    private Double examScore;

    private Integer isSubmitted;
}
