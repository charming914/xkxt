package com.school.notification.controller;

import com.school.common.api.ApiResponse;
import com.school.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知收件箱：学生与教师共用，按当前登录账号（X-Username）返回通知。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
        @RequestHeader(value = "X-Username", defaultValue = "") String username,
        @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly,
        @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return ApiResponse.success(notificationService.list(username, unreadOnly, limit));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount(
        @RequestHeader(value = "X-Username", defaultValue = "") String username
    ) {
        return ApiResponse.success(notificationService.unreadCount(username));
    }

    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(
        @PathVariable Long notificationId,
        @RequestHeader(value = "X-Username", defaultValue = "") String username
    ) {
        notificationService.markRead(username, notificationId);
        return ApiResponse.success("已标记为已读", null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(
        @RequestHeader(value = "X-Username", defaultValue = "") String username
    ) {
        notificationService.markAllRead(username);
        return ApiResponse.success("已全部标记为已读", null);
    }
}
