package com.school.auth.controller;

import com.school.auth.dto.LoginRequest;
import com.school.auth.dto.PasswordUpdateRequest;
import com.school.auth.service.AuthService;
import com.school.auth.vo.LoginUserVO;
import com.school.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginUserVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @GetMapping("/profile")
    public ApiResponse<LoginUserVO> profile(@RequestHeader(value = "X-Username", required = false) String username) {
        return ApiResponse.success(authService.profile(username));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(
        @RequestHeader(value = "X-Username", required = false) String username,
        @Valid @RequestBody PasswordUpdateRequest request
    ) {
        authService.updatePassword(username, request);
        return ApiResponse.success("密码修改成功", null);
    }
}
