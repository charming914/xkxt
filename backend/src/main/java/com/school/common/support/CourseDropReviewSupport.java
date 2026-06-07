package com.school.common.support;

import com.school.common.exception.BusinessException;

/**
 * 退课审核相关的通用逻辑，供教师端、管理员端复用。
 */
public final class CourseDropReviewSupport {

    private CourseDropReviewSupport() {
    }

    public static String resolveReviewStatus(String action) {
        if ("APPROVE".equalsIgnoreCase(action)) {
            return "已通过";
        }
        if ("REJECT".equalsIgnoreCase(action)) {
            return "已驳回";
        }
        throw new BusinessException("不支持的审核动作");
    }

    public static String resolveDropCourseMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "退课审核通过，但执行退课失败";
        }
        if (rawMessage.contains("课程开课日期未设置")) {
            return "退课失败：课程开课日期未设置";
        }
        if (rawMessage.contains("已超过开课三周")) {
            return "退课失败：已超过开课三周，且没有合法双审核通过记录";
        }
        if (rawMessage.contains("开课超过一周")) {
            return "退课失败：开课超过一周，需教师和管理员均同意后才能退课";
        }
        return "退课失败：" + rawMessage;
    }
}
