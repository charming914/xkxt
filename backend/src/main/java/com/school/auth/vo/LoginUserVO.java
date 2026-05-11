package com.school.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserVO {

    private Integer userId;
    private String username;
    private String realName;
    private String roleCode;
    private String token;
}
