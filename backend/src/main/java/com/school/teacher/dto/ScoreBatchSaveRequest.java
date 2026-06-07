package com.school.teacher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class ScoreBatchSaveRequest {

    @Valid
    @NotEmpty(message = "成绩列表不能为空")
    private List<ScoreInputRequest> scores;
}
