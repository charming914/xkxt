package com.school.teacher.service;

import com.school.common.exception.BusinessException;
import com.school.teacher.dto.CourseDropRequestTeacherReviewRequest;
import com.school.teacher.dto.ScoreInputRequest;
import com.school.teacher.mapper.TeacherMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherMapper teacherMapper;

    public List<Map<String, Object>> teachingClasses(String teacherId) {
        return teacherMapper.selectTeachingClasses(teacherId);
    }

    public List<Map<String, Object>> scores(String teacherId) {
        return teacherMapper.selectScores(teacherId);
    }

    public void saveScore(ScoreInputRequest request) {
        teacherMapper.saveOrUpdateScore(request, request.getIsSubmitted() == null ? 0 : request.getIsSubmitted());
    }

    public List<Map<String, Object>> courseDropRequests(String teacherId) {
        return teacherMapper.selectCourseDropRequests(teacherId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewCourseDropRequest(
        Long dropRequestId,
        CourseDropRequestTeacherReviewRequest request,
        String teacherId
    ) {
        Map<String, Object> dropRequest = teacherMapper.selectCourseDropRequestById(dropRequestId);
        if (dropRequest == null) {
            throw new BusinessException("退课申请不存在");
        }
        if (!teacherId.equals(dropRequest.get("teacherId"))) {
            throw new BusinessException("只能审核本人授课课程的退课申请");
        }
        if (!"待审核".equals(dropRequest.get("requestStatus"))) {
            throw new BusinessException("该退课申请已处理，请勿重复审核");
        }
        if (!"待审核".equals(dropRequest.get("teacherStatus"))) {
            throw new BusinessException("你已审核过该退课申请");
        }

        String teacherStatus = resolveReviewStatus(request.getAction());
        teacherMapper.updateTeacherDropReview(
            dropRequestId,
            teacherStatus,
            request.getReviewComment(),
            teacherId
        );
        dropCourseIfFullyApproved(dropRequestId, dropRequest);
    }

    private String resolveReviewStatus(String action) {
        if ("APPROVE".equalsIgnoreCase(action)) {
            return "已通过";
        }
        if ("REJECT".equalsIgnoreCase(action)) {
            return "已驳回";
        }
        throw new BusinessException("不支持的审核动作");
    }

    private void dropCourseIfFullyApproved(Long dropRequestId, Map<String, Object> dropRequest) {
        String requestStatus = teacherMapper.selectDropRequestStatus(dropRequestId);
        if (!"已通过".equals(requestStatus)) {
            return;
        }
        String studentId = (String) dropRequest.get("studentId");
        Integer teachingClassId = ((Number) dropRequest.get("teachingClassId")).intValue();
        try {
            teacherMapper.dropSelectedCourse(studentId, teachingClassId);
        } catch (RuntimeException e) {
            throw new BusinessException(resolveDropCourseMessage(e.getMessage()));
        }
    }

    private String resolveDropCourseMessage(String rawMessage) {
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
