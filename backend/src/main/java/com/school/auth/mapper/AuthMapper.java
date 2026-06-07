package com.school.auth.mapper;

import com.school.auth.vo.LoginUserVO;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthMapper {

    @Select("""
        SELECT
            u.user_id AS userId,
            u.username AS username,
            u.real_name AS realName,
            r.role_code AS roleCode,
            u.ref_id AS refId,
            CONCAT('token-', u.user_id) AS token,
            u.password AS password
        FROM sys_user u
        JOIN sys_user_role ur ON u.user_id = ur.user_id
        JOIN sys_role r ON ur.role_id = r.role_id
        WHERE u.username = #{username}
          AND u.is_enabled = 1
        LIMIT 1
        """)
    LoginUserVO selectLoginUser(@Param("username") String username);

    @Select("""
        SELECT
            u.user_id AS userId,
            u.username AS username,
            u.real_name AS realName,
            r.role_code AS roleCode,
            u.ref_id AS refId,
            CONCAT('token-', u.user_id) AS token
        FROM sys_user u
        JOIN sys_user_role ur ON u.user_id = ur.user_id
        JOIN sys_role r ON ur.role_id = r.role_id
        WHERE u.username = #{username}
          AND u.is_enabled = 1
        LIMIT 1
        """)
    LoginUserVO selectProfileByUsername(@Param("username") String username);

    @Select("SELECT password FROM sys_user WHERE username = #{username} AND is_enabled = 1 LIMIT 1")
    String selectPasswordByUsername(@Param("username") String username);

    @Update("UPDATE sys_user SET password = #{password} WHERE username = #{username}")
    int updatePassword(@Param("username") String username, @Param("password") String password);

    @Select("""
        SELECT user_id AS userId, password AS password
        FROM sys_user
        """)
    List<Map<String, Object>> selectAllUserPasswords();

    @Update("UPDATE sys_user SET password = #{password} WHERE user_id = #{userId}")
    int updatePasswordByUserId(@Param("userId") Integer userId, @Param("password") String password);
}
