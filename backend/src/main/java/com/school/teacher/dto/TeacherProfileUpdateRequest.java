package com.school.teacher.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeacherProfileUpdateRequest {

    @Size(max = 20, message = "手机号长度不能超过20位")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50位")
    private String email;
}
