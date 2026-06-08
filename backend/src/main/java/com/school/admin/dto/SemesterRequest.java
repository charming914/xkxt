package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SemesterRequest {

    /** 由数据库自增生成回填，无需前端传入；编辑时由路径参数回填 */
    private Integer semesterId;

    /**
     * 管理员唯一需要填写的字段：学期开始日期（yyyy-MM-dd）。
     * 学期名称、学年、学期代码、结束日期、选课时间均由后端按
     * 固定的「16 教学周 + 2 考试周」规则自动推导。
     */
    @NotBlank(message = "学期开始日期不能为空")
    private String startDate;

    /** 以下字段由后端推导后回填，无需前端传入 */
    private String semesterName;
    private String schoolYear;
    private String termCode;
    private String endDate;
    private String courseSelectStart;
    private String courseSelectEnd;

    /** 是否结转某学期的教学班，为空则不结转（仅新建时生效） */
    private Integer carryOverFromSemesterId;

    /** 是否在创建后立即设为当前学期（仅新建时生效） */
    private Boolean setAsCurrent;
}
