package com.school.teacher.service;

import static com.school.common.support.CourseDropReviewSupport.resolveDropCourseMessage;
import static com.school.common.support.CourseDropReviewSupport.resolveReviewStatus;

import com.school.common.dto.CourseDropReviewRequest;
import com.school.common.exception.BusinessException;
import com.school.teacher.dto.ScheduleAdjustRequestCreateRequest;
import com.school.teacher.dto.ScoreChangeRequestCreateRequest;
import com.school.teacher.dto.ScoreInputRequest;
import com.school.teacher.dto.TeacherProfileUpdateRequest;
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

    public Map<String, Object> profile(String teacherId) {
        Map<String, Object> profile = teacherMapper.selectTeacherProfile(teacherId);
        if (profile == null) {
            throw new BusinessException("教师信息不存在");
        }
        return profile;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(String teacherId, TeacherProfileUpdateRequest request) {
        if (teacherMapper.updateTeacherProfile(teacherId, request.getPhone(), request.getEmail()) == 0) {
            throw new BusinessException("教师信息不存在");
        }
    }

    public List<Map<String, Object>> teachingClasses(String teacherId) {
        return teacherMapper.selectTeachingClasses(teacherId);
    }

    public List<Map<String, Object>> scores(String teacherId) {
        return teacherMapper.selectScores(teacherId);
    }

    public Map<String, Object> todoSummary(String teacherId) {
        return teacherMapper.selectTodoSummary(teacherId);
    }

    public Map<String, Object> scoreSubmitWindow(String teacherId, Integer teachingClassId) {
        ensureTeachingClassOwner(teacherId, teachingClassId);
        return teacherMapper.selectScoreSubmitWindow(teacherId, teachingClassId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveScore(ScoreInputRequest request, String teacherId) {
        ensureTeachingClassOwner(teacherId, request.getTeachingClassId());
        ensureScoreSubmitAllowed(teacherId, request);
        teacherMapper.saveOrUpdateScore(request, request.getIsSubmitted() == null ? 0 : request.getIsSubmitted());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveScores(List<ScoreInputRequest> requests, String teacherId) {
        for (ScoreInputRequest request : requests) {
            ensureTeachingClassOwner(teacherId, request.getTeachingClassId());
            ensureScoreSubmitAllowed(teacherId, request);
            teacherMapper.saveOrUpdateScore(request, request.getIsSubmitted() == null ? 0 : request.getIsSubmitted());
        }
    }

    public Map<String, Object> teachingClassDetail(String teacherId, Integer teachingClassId) {
        Map<String, Object> detail = teacherMapper.selectTeachingClassDetail(teacherId, teachingClassId);
        if (detail == null) {
            throw new BusinessException("教学班不存在或不属于当前教师");
        }
        return detail;
    }

    public List<Map<String, Object>> teachingClassStudents(String teacherId, Integer teachingClassId) {
        ensureTeachingClassOwner(teacherId, teachingClassId);
        return teacherMapper.selectTeachingClassStudents(teachingClassId);
    }

    public List<Map<String, Object>> courseDropRequests(String teacherId) {
        return teacherMapper.selectCourseDropRequests(teacherId);
    }

    public List<Map<String, Object>> classroomOptions() {
        return teacherMapper.selectClassroomOptions();
    }

    public List<Map<String, Object>> availableClassroomOptions(
        String teacherId,
        Integer teachingClassId,
        Integer weekDay,
        String sectionDesc
    ) {
        ensureTeachingClassOwner(teacherId, teachingClassId);
        return teacherMapper.selectAvailableClassroomOptions(teachingClassId, weekDay, sectionDesc);
    }

    public List<Map<String, Object>> scheduleAdjustRequests(String teacherId) {
        return teacherMapper.selectScheduleAdjustRequests(teacherId);
    }

    public List<Map<String, Object>> scoreChangeRequests(String teacherId) {
        return teacherMapper.selectScoreChangeRequests(teacherId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createScoreChangeRequest(String teacherId, ScoreChangeRequestCreateRequest request) {
        if (teacherMapper.countSubmittedScoreOwner(teacherId, request.getScoreId()) == 0) {
            throw new BusinessException("成绩不存在、未提交或不属于当前教师");
        }
        if (teacherMapper.countPendingScoreChangeRequest(request.getScoreId()) > 0) {
            throw new BusinessException("该成绩已有待审核修改申请");
        }
        if (teacherMapper.insertScoreChangeRequest(teacherId, request) == 0) {
            throw new BusinessException("成绩修改申请提交失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void createScheduleAdjustRequest(String teacherId, ScheduleAdjustRequestCreateRequest request) {
        Map<String, Object> teachingClass = teachingClassDetail(teacherId, request.getTeachingClassId());
        String originalWeekRange = (String) teachingClass.get("weekRange");
        String originalSectionDesc = (String) teachingClass.get("sectionDesc");
        if (!originalWeekRange.equals(request.getRequestedWeekRange())) {
            throw new BusinessException("调课申请不能调整周次范围");
        }
        if (resolveSectionLength(originalSectionDesc) != resolveSectionLength(request.getRequestedSectionDesc())) {
            throw new BusinessException("调整后节次长度必须与原节次一致");
        }
        if (teacherMapper.countPendingScheduleAdjustRequest(teacherId, request.getTeachingClassId()) > 0) {
            throw new BusinessException("该教学班已有待审核调课申请");
        }
        if (teacherMapper.insertScheduleAdjustRequest(teacherId, request) == 0) {
            throw new BusinessException("调课申请提交失败");
        }
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
            teacherMapper.deleteScore(studentId, teachingClassId);
        } catch (RuntimeException e) {
            throw new BusinessException(resolveDropCourseMessage(e.getMessage()));
        }
    }

    private void ensureTeachingClassOwner(String teacherId, Integer teachingClassId) {
        if (teacherMapper.countTeachingClassOwner(teacherId, teachingClassId) == 0) {
            throw new BusinessException("教学班不存在或不属于当前教师");
        }
    }

    private void ensureScoreSubmitAllowed(String teacherId, ScoreInputRequest request) {
        if (!Integer.valueOf(1).equals(request.getIsSubmitted())) {
            return;
        }
        if (teacherMapper.countOpenScoreSubmitWindow(teacherId, request.getTeachingClassId()) == 0) {
            throw new BusinessException("当前不在成绩提交时间范围内");
        }
    }

    private int resolveSectionLength(String sectionDesc) {
        if (sectionDesc == null || !sectionDesc.contains("-")) {
            return 0;
        }
        String normalized = sectionDesc.replace("节", "");
        String[] parts = normalized.split("-");
        if (parts.length != 2) {
            return 0;
        }
        try {
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            return end - start + 1;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
