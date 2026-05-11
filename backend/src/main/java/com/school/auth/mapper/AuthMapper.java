package com.school.auth.mapper;

import com.school.auth.vo.LoginUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthMapper {

    @Select("""
        SELECT
            u.user_id AS userId,
            u.username AS username,
            u.real_name AS realName,
            r.role_code AS roleCode,
            CONCAT('token-', u.user_id) AS token
        FROM sys_user u
        JOIN sys_user_role ur ON u.user_id = ur.user_id
        JOIN sys_role r ON ur.role_id = r.role_id
        WHERE u.username = #{username}
          AND u.password = #{password}
          AND u.is_enabled = 1
        LIMIT 1
        """)
    LoginUserVO selectLoginUser(@Param("username") String username, @Param("password") String password);

    @Select("""
        SELECT
            u.user_id AS userId,
            u.username AS username,
            u.real_name AS realName,
            r.role_code AS roleCode,
            CONCAT('token-', u.user_id) AS token
        FROM sys_user u
        JOIN sys_user_role ur ON u.user_id = ur.user_id
        JOIN sys_role r ON ur.role_id = r.role_id
        WHERE u.username = #{username}
          AND u.is_enabled = 1
        LIMIT 1
        """)
    LoginUserVO selectProfileByUsername(@Param("username") String username);

    @Select("""
        SELECT COUNT(1)
        FROM sys_user
        WHERE username = #{username}
          AND password = #{oldPassword}
          AND is_enabled = 1
        """)
    Integer countByUsernameAndPassword(@Param("username") String username, @Param("oldPassword") String oldPassword);
}
