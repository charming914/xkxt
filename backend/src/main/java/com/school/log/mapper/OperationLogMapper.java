package com.school.log.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OperationLogMapper {

    @Insert("""
        INSERT INTO operation_log (user_id, operation_type, operation_content)
        VALUES (#{userId}, #{operationType}, #{operationContent})
        """)
    int insertLog(
        @Param("userId") Integer userId,
        @Param("operationType") String operationType,
        @Param("operationContent") String operationContent
    );

    @Select("SELECT user_id FROM sys_user WHERE username = #{username} LIMIT 1")
    Integer selectUserIdByUsername(@Param("username") String username);

    @Select("""
        <script>
        SELECT
            l.log_id AS logId,
            l.operation_type AS operationType,
            l.operation_content AS operationContent,
            l.operation_time AS operationTime,
            u.username AS username,
            u.real_name AS realName,
            u.user_type AS userType
        FROM operation_log l
        LEFT JOIN sys_user u ON l.user_id = u.user_id
        <where>
            <if test="keyword != null and keyword != ''">
                AND (l.operation_content LIKE CONCAT('%', #{keyword}, '%')
                     OR u.real_name LIKE CONCAT('%', #{keyword}, '%')
                     OR u.username LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="operationType != null and operationType != ''">
                AND l.operation_type = #{operationType}
            </if>
        </where>
        ORDER BY l.log_id DESC
        LIMIT #{offset}, #{size}
        </script>
        """)
    List<Map<String, Object>> selectLogs(
        @Param("keyword") String keyword,
        @Param("operationType") String operationType,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM operation_log l
        LEFT JOIN sys_user u ON l.user_id = u.user_id
        <where>
            <if test="keyword != null and keyword != ''">
                AND (l.operation_content LIKE CONCAT('%', #{keyword}, '%')
                     OR u.real_name LIKE CONCAT('%', #{keyword}, '%')
                     OR u.username LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="operationType != null and operationType != ''">
                AND l.operation_type = #{operationType}
            </if>
        </where>
        </script>
        """)
    long countLogs(
        @Param("keyword") String keyword,
        @Param("operationType") String operationType
    );

    @Select("SELECT DISTINCT operation_type FROM operation_log ORDER BY operation_type")
    List<String> selectOperationTypes();
}
