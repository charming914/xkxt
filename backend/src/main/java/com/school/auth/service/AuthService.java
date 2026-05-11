package com.school.auth.service;

import com.school.auth.dto.LoginRequest;
import com.school.auth.dto.PasswordUpdateRequest;
import com.school.auth.mapper.AuthMapper;
import com.school.auth.vo.LoginUserVO;
import com.school.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthMapper authMapper;

    public LoginUserVO login(LoginRequest request) {
        LoginUserVO loginUser = authMapper.selectLoginUser(request.getUsername(), request.getPassword());
        if (loginUser == null) {
            throw new BusinessException("用户名或密码错误");
        }
        return loginUser;
    }

    public LoginUserVO profile(String username) {
        LoginUserVO loginUser = authMapper.selectProfileByUsername(username);
        if (loginUser == null) {
            throw new BusinessException("用户不存在");
        }
        return loginUser;
    }

    public void updatePassword(String username, PasswordUpdateRequest request) {
        Integer count = authMapper.countByUsernameAndPassword(username, request.getOldPassword());
        if (count == null || count == 0) {
            throw new BusinessException("旧密码不正确");
        }
    }
}
