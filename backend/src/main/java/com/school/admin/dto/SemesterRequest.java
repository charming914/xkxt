package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SemesterRequest {

    /** 由数据库自增生成回填，无需前端传入 */
    private Integer semesterId;

    @NotBlank(message = "学期名称不能为空")
    private String semesterName;

    @NotBlank(message = "学年不能为空")
    private String schoolYear;

    @NotBlank(message = "学期代码不能为空")
    private String termCode;

    @NotBlank(message = "开始日期不能为空")
    private String startDate;

    @NotBlank(message = "结束日期不能为空")
    private String endDate;

    @NotBlank(message = "选课开始时间不能为空")
    private String courseSelectStart;

    @NotBlank(message = "选课结束时间不能为空")
    private String courseSelectEnd;

    /** 是否结转某学期的教学班，为空则不结转 */
    private Integer carryOverFromSemesterId;

    /** 是否在创建后立即设为当前学期 */
    private Boolean setAsCurrent;
}
