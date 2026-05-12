package com.school.auth.config;

import com.school.auth.mapper.AuthMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordUpgradeRunner implements CommandLineRunner {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<Map<String, Object>> users = authMapper.selectAllUserPasswords();
        for (Map<String, Object> user : users) {
            Integer userId = ((Number) user.get("userId")).intValue();
            String password = (String) user.get("password");
            if (password == null || isBcryptPassword(password)) {
                continue;
            }
            authMapper.updatePasswordByUserId(userId, passwordEncoder.encode(password));
        }
    }

    private boolean isBcryptPassword(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
