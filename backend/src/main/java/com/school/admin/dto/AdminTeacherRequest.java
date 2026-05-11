package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminTeacherRequest {

    @NotBlank(message = "工号不能为空")
    private String teacherId;

    @NotBlank(message = "姓名不能为空")
    private String teacherName;

    @NotBlank(message = "性别不能为空")
    private String gender;

    private String birthDate;

    @NotBlank(message = "职称不能为空")
    private String titleName;

    private String education;
    private String phone;
    private String email;

    @NotBlank(message = "院系不能为空")
    private String departmentId;

    @NotBlank(message = "状态不能为空")
    private String status;
}
