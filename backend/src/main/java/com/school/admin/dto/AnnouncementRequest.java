package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员发布公告请求。
 */
@Data
public class AnnouncementRequest {

    @NotBlank(message = "公告标题不能为空")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    /** ALL 全体，STUDENT 全体学生，TEACHER 全体教师，DEPARTMENT 指定院系 */
    private String targetScope;

    /** targetScope=DEPARTMENT 时使用 */
    private String targetDepartmentId;
}
