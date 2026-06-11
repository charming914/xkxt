package com.school.notification.service;

import com.school.common.exception.BusinessException;
import com.school.notification.mapper.NotificationMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationMapper notificationMapper;

    // ==================== 收件箱查询 ====================

    public List<Map<String, Object>> list(String username, boolean unreadOnly, int limit) {
        Integer userId = requireUserId(username);
        int safeLimit = limit <= 0 || limit > 200 ? 50 : limit;
        return notificationMapper.selectByUser(userId, unreadOnly ? 1 : 0, safeLimit);
    }

    public int unreadCount(String username) {
        Integer userId = notificationMapper.selectUserIdByUsername(username);
        return userId == null ? 0 : notificationMapper.countUnread(userId);
    }

    public void markRead(String username, Long notificationId) {
        Integer userId = requireUserId(username);
        notificationMapper.markRead(notificationId, userId);
    }

    public void markAllRead(String username) {
        Integer userId = requireUserId(username);
        notificationMapper.markAllRead(userId);
    }

    private Integer requireUserId(String username) {
        Integer userId = notificationMapper.selectUserIdByUsername(username);
        if (userId == null) {
            throw new BusinessException("用户不存在");
        }
        return userId;
    }

    // ==================== 审批结果通知（供 AdminService 调用） ====================

    /**
     * 给指定登录账号发送一条审批结果通知。通知发送失败不应影响主审批事务，
     * 因此异常仅记录日志，由调用方在同一事务内安全调用。
     */
    public void notifyByUsername(String username, String refType, String refId, String title, String content) {
        if (username == null || username.isBlank()) {
            return;
        }
        Integer userId = notificationMapper.selectUserIdByUsername(username);
        if (userId == null) {
            log.warn("发送通知失败：找不到用户 {}", username);
            return;
        }
        notifyByUserId(userId, refType, refId, title, content);
    }

    public void notifyByUserId(Integer userId, String refType, String refId, String title, String content) {
        if (userId == null) {
            return;
        }
        try {
            notificationMapper.insertNotification(userId, "APPROVAL", title, content, refType, refId, null);
        } catch (RuntimeException e) {
            log.warn("发送通知失败：{}", e.getMessage());
        }
    }

    public Integer resolveTeacherUserIdByTeachingClass(Integer teachingClassId) {
        return notificationMapper.selectTeacherUserIdByTeachingClass(teachingClassId);
    }

    public Integer resolveTeacherUserIdByScoreChange(Long scoreChangeRequestId) {
        return notificationMapper.selectTeacherUserIdByScoreChange(scoreChangeRequestId);
    }

    public Integer resolveTeacherUserIdByExamChange(Long examChangeRequestId) {
        return notificationMapper.selectTeacherUserIdByExamChange(examChangeRequestId);
    }

    // ==================== 公告发布 ====================

    @Transactional(rollbackFor = Exception.class)
    public int publishAnnouncement(
        String title,
        String content,
        String targetScope,
        String targetDepartmentId,
        String adminUsername
    ) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("公告标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException("公告内容不能为空");
        }
        String scope = targetScope == null || targetScope.isBlank() ? "ALL" : targetScope.trim().toUpperCase();
        if (!List.of("ALL", "STUDENT", "TEACHER", "DEPARTMENT").contains(scope)) {
            throw new BusinessException("不支持的投递范围");
        }
        if ("DEPARTMENT".equals(scope) && (targetDepartmentId == null || targetDepartmentId.isBlank())) {
            throw new BusinessException("按院系发布时必须选择院系");
        }
        Integer publishedBy = notificationMapper.selectUserIdByUsername(adminUsername);

        notificationMapper.insertAnnouncement(
            title.trim(), content.trim(), scope,
            "DEPARTMENT".equals(scope) ? targetDepartmentId : null,
            0, publishedBy
        );
        Long announcementId = notificationMapper.selectLastInsertId();
        int recipients = notificationMapper.fanoutAnnouncement(
            announcementId, title.trim(), content.trim(), scope,
            "DEPARTMENT".equals(scope) ? targetDepartmentId : null
        );
        notificationMapper.updateAnnouncementRecipientCount(announcementId, recipients);
        return recipients;
    }

    public List<Map<String, Object>> announcements() {
        return notificationMapper.selectAnnouncements();
    }
}
