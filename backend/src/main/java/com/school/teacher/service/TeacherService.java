package com.school.teacher.service;

import static com.school.common.support.CourseDropReviewSupport.resolveDropCourseMessage;
import static com.school.common.support.CourseDropReviewSupport.resolveReviewStatus;

import com.school.common.dto.CourseDropReviewRequest;
import com.school.common.exception.BusinessException;
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
        CourseDropReviewRequest request,
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

}
