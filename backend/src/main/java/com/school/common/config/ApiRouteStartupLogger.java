package com.school.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiRouteStartupLogger implements ApplicationRunner {

    private static final String NOTIFICATION_UNREAD_COUNT_PATH = "/api/notifications/unread-count";

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public void run(ApplicationArguments args) {
        boolean registered = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
            .map(RequestMappingInfo::getPathPatternsCondition)
            .filter(condition -> condition != null)
            .flatMap(condition -> condition.getPatterns().stream())
            .anyMatch(pattern -> NOTIFICATION_UNREAD_COUNT_PATH.equals(pattern.getPatternString()));

        if (registered) {
            log.info("API route registered: {}", NOTIFICATION_UNREAD_COUNT_PATH);
        } else {
            log.warn("API route missing: {}. Check whether NotificationController is included in the running backend.", NOTIFICATION_UNREAD_COUNT_PATH);
        }
    }
}
