package com.school.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminStudentRequest {

    @NotBlank(message = "学号不能为空")
    private String studentId;

    @NotBlank(message = "姓名不能为空")
    private String studentName;

    @NotBlank(message = "性别不能为空")
    private String gender;

    private String birthDate;
    private String hometown;
    private String phone;
    private String email;

    @NotBlank(message = "入学年份不能为空")
    private String enrollmentYear;

    @NotBlank(message = "院系不能为空")
    private String departmentId;

    @NotBlank(message = "专业不能为空")
    private String majorId;

    @NotBlank(message = "班级不能为空")
    private String classId;

    @NotBlank(message = "状态不能为空")
    private String status;
}
