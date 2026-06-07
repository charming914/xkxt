package com.school.log;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 拦截标注了 {@link OperationLog} 的方法，在方法成功返回后写入操作日志。
 * 日志写入失败只记录告警，绝不影响主业务流程。
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogService operationLogService;

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Pointcut("@annotation(operationLog)")
    public void logPointcut(OperationLog operationLog) {
    }

    @AfterReturning(pointcut = "logPointcut(operationLog)", argNames = "joinPoint,operationLog")
    public void afterReturning(JoinPoint joinPoint, OperationLog operationLog) {
        try {
            String username = resolveUsername();
            String content = buildContent(operationLog.action(), joinPoint.getArgs());
            operationLogService.record(username, operationLog.type(), content);
        } catch (RuntimeException e) {
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

    private String resolveUsername() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String username = request.getHeader("X-Username");
        return (username == null || username.isBlank()) ? "admin" : username;
    }

    /**
     * 在动作描述后附加一个关键标识参数（路径上的编号），方便日志定位对象。
     */
    private String buildContent(String action, Object[] args) {
        if (args == null) {
            return action;
        }
        for (Object arg : args) {
            if (arg instanceof String str && !str.isBlank()) {
                return action + "：" + str;
            }
            if (arg instanceof Number number) {
                return action + "：" + number;
            }
        }
        return action;
    }
}
