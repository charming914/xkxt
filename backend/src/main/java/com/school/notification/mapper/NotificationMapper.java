package com.school.notification.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper {

    @Insert("""
        INSERT INTO notification (user_id, category, title, content, ref_type, ref_id, announcement_id)
        VALUES (#{userId}, #{category}, #{title}, #{content}, #{refType}, #{refId}, #{announcementId})
        """)
    int insertNotification(
        @Param("userId") Integer userId,
        @Param("category") String category,
        @Param("title") String title,
        @Param("content") String content,
        @Param("refType") String refType,
        @Param("refId") String refId,
        @Param("announcementId") Long announcementId
    );

    @Select("""
        SELECT
            notification_id AS notificationId,
            category,
            title,
            content,
            is_read AS isRead,
            ref_type AS refType,
            ref_id AS refId,
            created_at AS createdAt,
            read_at AS readAt
        FROM notification
        WHERE user_id = #{userId}
          AND (#{unreadOnly} = 0 OR is_read = 0)
        ORDER BY is_read ASC, created_at DESC, notification_id DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> selectByUser(
        @Param("userId") Integer userId,
        @Param("unreadOnly") int unreadOnly,
        @Param("limit") int limit
    );

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Integer userId);

    @Update("""
        UPDATE notification
        SET is_read = 1, read_at = NOW()
        WHERE notification_id = #{notificationId} AND user_id = #{userId}
        """)
    int markRead(@Param("notificationId") Long notificationId, @Param("userId") Integer userId);

    @Update("""
        UPDATE notification
        SET is_read = 1, read_at = NOW()
        WHERE user_id = #{userId} AND is_read = 0
        """)
    int markAllRead(@Param("userId") Integer userId);

    // ==================== 用户/接收人解析 ====================

    @Select("SELECT user_id FROM sys_user WHERE username = #{username} LIMIT 1")
    Integer selectUserIdByUsername(@Param("username") String username);

    @Select("""
        SELECT u.user_id
        FROM teaching_class tc
        JOIN sys_user u ON u.username = tc.teacher_id
        WHERE tc.teaching_class_id = #{teachingClassId}
        LIMIT 1
        """)
    Integer selectTeacherUserIdByTeachingClass(@Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT u.user_id
        FROM score_change_request scr
        JOIN sys_user u ON u.username = scr.teacher_id
        WHERE scr.score_change_request_id = #{scoreChangeRequestId}
        LIMIT 1
        """)
    Integer selectTeacherUserIdByScoreChange(@Param("scoreChangeRequestId") Long scoreChangeRequestId);

    @Select("""
        SELECT u.user_id
        FROM exam_change_request ecr
        JOIN sys_user u ON u.username = ecr.teacher_id
        WHERE ecr.exam_change_request_id = #{examChangeRequestId}
        LIMIT 1
        """)
    Integer selectTeacherUserIdByExamChange(@Param("examChangeRequestId") Long examChangeRequestId);

    // ==================== 公告 ====================

    @Insert("""
        INSERT INTO announcement (title, content, target_scope, target_department_id, recipient_count, published_by)
        VALUES (#{title}, #{content}, #{targetScope}, #{targetDepartmentId}, #{recipientCount}, #{publishedBy})
        """)
    int insertAnnouncement(
        @Param("title") String title,
        @Param("content") String content,
        @Param("targetScope") String targetScope,
        @Param("targetDepartmentId") String targetDepartmentId,
        @Param("recipientCount") int recipientCount,
        @Param("publishedBy") Integer publishedBy
    );

    @Select("SELECT LAST_INSERT_ID()")
    Long selectLastInsertId();

    @Update("UPDATE announcement SET recipient_count = #{recipientCount} WHERE announcement_id = #{announcementId}")
    int updateAnnouncementRecipientCount(
        @Param("announcementId") Long announcementId,
        @Param("recipientCount") int recipientCount
    );

    /**
     * 按投递范围把公告群发到 notification 表。
     * scope: ALL 全体（学生+教师），STUDENT 全体学生，TEACHER 全体教师，DEPARTMENT 指定院系学生与教师。
     */
    @Insert("""
        INSERT INTO notification (user_id, category, title, content, ref_type, ref_id, announcement_id)
        SELECT u.user_id, 'ANNOUNCEMENT', #{title}, #{content}, '公告', NULL, #{announcementId}
        FROM sys_user u
        WHERE u.is_enabled = 1
          AND (
                (#{targetScope} = 'ALL' AND u.user_type IN ('学生', '教师'))
             OR (#{targetScope} = 'STUDENT' AND u.user_type = '学生')
             OR (#{targetScope} = 'TEACHER' AND u.user_type = '教师')
             OR (#{targetScope} = 'DEPARTMENT' AND (
                    u.ref_id IN (SELECT student_id FROM student WHERE department_id = #{targetDepartmentId})
                 OR u.ref_id IN (SELECT teacher_id FROM teacher WHERE department_id = #{targetDepartmentId})
                ))
          )
        """)
    int fanoutAnnouncement(
        @Param("announcementId") Long announcementId,
        @Param("title") String title,
        @Param("content") String content,
        @Param("targetScope") String targetScope,
        @Param("targetDepartmentId") String targetDepartmentId
    );

    @Select("""
        SELECT
            a.announcement_id AS announcementId,
            a.title,
            a.content,
            a.target_scope AS targetScope,
            a.target_department_id AS targetDepartmentId,
            d.department_name AS targetDepartmentName,
            a.recipient_count AS recipientCount,
            u.real_name AS publishedByName,
            a.created_at AS createdAt
        FROM announcement a
        LEFT JOIN sys_user u ON u.user_id = a.published_by
        LEFT JOIN department d ON d.department_id = a.target_department_id
        ORDER BY a.created_at DESC, a.announcement_id DESC
        LIMIT 200
        """)
    List<Map<String, Object>> selectAnnouncements();
}
