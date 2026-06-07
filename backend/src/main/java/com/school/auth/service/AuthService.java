package com.school.auth.service;

import com.school.auth.dto.LoginRequest;
import com.school.auth.dto.PasswordUpdateRequest;
import com.school.auth.mapper.AuthMapper;
import com.school.auth.vo.LoginUserVO;
import com.school.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    public LoginUserVO login(LoginRequest request) {
        LoginUserVO loginUser = authMapper.selectLoginUser(request.getUsername());
        if (loginUser == null) {
            throw new BusinessException("用户名或密码错误");
        }
        String storedPassword = loginUser.getPassword();
        boolean matched = passwordEncoder.matches(request.getPassword(), storedPassword);
        if (!matched && request.getPassword().equals(storedPassword)) {
            authMapper.updatePassword(request.getUsername(), passwordEncoder.encode(request.getPassword()));
            matched = true;
        }
        if (!matched) {
            throw new BusinessException("用户名或密码错误");
        }
        loginUser.setPassword(null);
        return loginUser;
    }

    public LoginUserVO profile(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("未识别登录身份，请重新登录");
        }
        LoginUserVO loginUser = authMapper.selectProfileByUsername(username);
        if (loginUser == null) {
            throw new BusinessException("用户不存在");
        }
        return loginUser;
    }

    public void updatePassword(String username, PasswordUpdateRequest request) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("未识别登录身份，请重新登录");
        }
        String encodedPassword = authMapper.selectPasswordByUsername(username);
        boolean matched = encodedPassword != null && passwordEncoder.matches(request.getOldPassword(), encodedPassword);
        if (!matched && encodedPassword != null && request.getOldPassword().equals(encodedPassword)) {
            matched = true;
        }
        if (!matched) {
            throw new BusinessException("旧密码不正确");
        }
        authMapper.updatePassword(username, passwordEncoder.encode(request.getNewPassword()));
    }
}
