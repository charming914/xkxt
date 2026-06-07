package com.school.student.service;

import com.school.common.exception.BusinessException;
import com.school.student.dto.CourseDropRequestCreateRequest;
import com.school.student.dto.CourseSelectionRequestCreateRequest;
import com.school.student.dto.ScoreReviewRequestCreateRequest;
import com.school.student.dto.SelectCourseRequest;
import com.school.student.dto.StudentCourseQueryRequest;
import com.school.student.mapper.StudentMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;

    public List<Map<String, Object>> availableCourses(
        StudentCourseQueryRequest query,
        String username,
        String fallbackStudentId
    ) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        return studentMapper.selectAvailableCourses(query, studentId);
    }

    public List<Map<String, Object>> selectedCourses(String username, String fallbackStudentId) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        return studentMapper.selectSelectedCourses(studentId);
    }

    public List<Map<String, Object>> dropCourses(String username, String fallbackStudentId) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        return studentMapper.selectDropCourses(studentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void selectCourse(String username, String fallbackStudentId, SelectCourseRequest request) {
        String studentId = resolveStudentId(username, firstNonBlank(fallbackStudentId, request.getStudentId()));
        Integer teachingClassId = request.getTeachingClassId();
        if (studentMapper.countSelectedCourse(studentId, teachingClassId) > 0) {
            throw new BusinessException("选课失败：你已选过该课程");
        }
        if (studentMapper.countSelectableTeachingClass(teachingClassId) == 0) {
            throw new BusinessException("选课失败：当前不在选课开放时间内或该教学班不可选");
        }
        if (studentMapper.countAvailableCapacity(teachingClassId) == 0) {
            throw new BusinessException("选课失败：该教学班人数已满");
        }
        if (studentMapper.countStudentTimeConflict(studentId, teachingClassId) > 0) {
            throw new BusinessException("选课失败：该课程与已选课程时间冲突");
        }
        if (studentMapper.countDroppedCourse(studentId, teachingClassId) > 0) {
            studentMapper.deleteScore(studentId, teachingClassId);
            studentMapper.restoreDroppedCourse(studentId, teachingClassId);
        } else {
            studentMapper.insertSelectedCourse(studentId, teachingClassId);
        }
        studentMapper.increaseSelectedCount(teachingClassId);
        studentMapper.insertOperationLog(
            studentId,
            "选课",
            "学生 " + studentId + " 选课成功，教学班编号：" + teachingClassId
        );
    }

    public List<Map<String, Object>> timetable(String username, String fallbackStudentId) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        return studentMapper.selectTimetable(studentId);
    }

    public List<Map<String, Object>> scores(String username, String fallbackStudentId) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        try {
            return studentMapper.selectScores(studentId);
        } catch (RuntimeException e) {
            if (isMissingScoreReviewTable(e)) {
                return studentMapper.selectScoresWithoutReview(studentId);
            }
            throw e;
        }
    }

    public void createCourseSelectionRequest(
        String username,
        String fallbackStudentId,
        CourseSelectionRequestCreateRequest request
    ) {
        String studentId = resolveStudentId(username, firstNonBlank(fallbackStudentId, request.getStudentId()));
        Integer teachingClassId = request.getTeachingClassId();

        if (studentMapper.countSelectedCourse(studentId, teachingClassId) > 0) {
            throw new BusinessException("你已选上该课程，无需重复申请补选");
        }
        if (studentMapper.countPendingRequest(studentId, teachingClassId) > 0) {
            throw new BusinessException("该课程已有待审核的补选申请");
        }
        studentMapper.insertCourseSelectionRequest(studentId, teachingClassId, request.getRequestReason());
    }

    public List<Map<String, Object>> courseSelectionRequests(String username, String fallbackStudentId) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        return studentMapper.selectCourseSelectionRequests(studentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dropCourse(String username, String fallbackStudentId, Integer teachingClassId) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        if (studentMapper.countSelectedCourse(studentId, teachingClassId) == 0) {
            throw new BusinessException("未选该课程，不能退课");
        }
        try {
            int updated = studentMapper.dropCourse(studentId, teachingClassId);
            if (updated == 0) {
                throw new BusinessException("退课失败：未找到可退课程");
            }
            studentMapper.deleteScore(studentId, teachingClassId);
            studentMapper.insertOperationLog(
                studentId,
                "退课",
                "学生 " + studentId + " 退课成功，教学班编号：" + teachingClassId
            );
        } catch (RuntimeException e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(resolveDropCourseMessage(e.getMessage()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void createCourseDropRequest(
        String username,
        String fallbackStudentId,
        CourseDropRequestCreateRequest request
    ) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        Integer teachingClassId = request.getTeachingClassId();
        if (studentMapper.countSelectedCourse(studentId, teachingClassId) == 0) {
            throw new BusinessException("未选该课程，不能提交退课申请");
        }
        if (studentMapper.countPendingDropRequest(studentId, teachingClassId) > 0) {
            throw new BusinessException("该课程已有待审核退课申请");
        }
        try {
            studentMapper.insertCourseDropRequest(studentId, teachingClassId, request.getRequestReason());
            studentMapper.insertOperationLog(
                studentId,
                "退课申请",
                "学生 " + studentId + " 提交退课申请，教学班编号：" + teachingClassId
            );
        } catch (RuntimeException e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(resolveDropCourseMessage(e.getMessage()));
        }
    }

    public List<Map<String, Object>> courseDropRequests(String username, String fallbackStudentId) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        return studentMapper.selectCourseDropRequests(studentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createScoreReviewRequest(
        String username,
        String fallbackStudentId,
        ScoreReviewRequestCreateRequest request
    ) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        Long scoreId = request.getScoreId();
        if (studentMapper.countReviewableScore(studentId, scoreId) == 0) {
            throw new BusinessException("该成绩不存在或尚未提交，不能申请复核");
        }
        try {
            if (studentMapper.countPendingScoreReviewRequest(studentId, scoreId) > 0) {
                throw new BusinessException("该成绩已有待审核复核申请");
            }
            studentMapper.insertScoreReviewRequest(studentId, scoreId, request.getRequestReason());
        } catch (RuntimeException e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            if (isMissingScoreReviewTable(e)) {
                throw new BusinessException("成绩复核功能尚未初始化，请先执行 sql/0607_score_review_request.sql");
            }
            throw e;
        }
        studentMapper.insertOperationLog(
            studentId,
            "成绩复核申请",
            "学生 " + studentId + " 提交成绩复核申请，成绩编号：" + scoreId
        );
    }

    public List<Map<String, Object>> scoreReviewRequests(String username, String fallbackStudentId) {
        String studentId = resolveStudentId(username, fallbackStudentId);
        try {
            return studentMapper.selectScoreReviewRequests(studentId);
        } catch (RuntimeException e) {
            if (isMissingScoreReviewTable(e)) {
                return List.of();
            }
            throw e;
        }
    }

    private String resolveStudentId(String username, String fallbackStudentId) {
        if (username != null && !username.isBlank()) {
            String studentId = studentMapper.selectStudentIdByUsername(username);
            if (studentId != null && !studentId.isBlank()) {
                return studentId;
            }
            throw new BusinessException("当前账号不是学生身份");
        }
        if (fallbackStudentId != null && !fallbackStudentId.isBlank()) {
            return fallbackStudentId;
        }
        throw new BusinessException("未识别登录身份，请重新登录");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private boolean isMissingScoreReviewTable(RuntimeException e) {
        StringBuilder messages = new StringBuilder();
        Throwable current = e;
        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(current.getMessage()).append('\n');
            }
            current = current.getCause();
        }
        String message = messages.toString();
        return message.contains("score_review_request")
            && (message.contains("doesn't exist")
                || message.contains("does not exist")
                || message.contains("不存在"));
    }

    private String resolveDropCourseMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "退课失败，请稍后重试";
        }
        if (rawMessage.contains("课程开课日期未设置")) {
            return "退课失败：课程开课日期未设置";
        }
        if (rawMessage.contains("已超过开课三周")) {
            return "退课失败：已超过开课三周，不能退课或提交申请";
        }
        if (rawMessage.contains("开课超过一周")) {
            return "退课失败：开课超过一周，需教师和管理员均同意后才能退课";
        }
        if (rawMessage.contains("开课一周内可直接退课")) {
            return "提交失败：开课一周内可直接退课，无需提交申请";
        }
        if (rawMessage.contains("未选该课程")) {
            return "退课失败：未选该课程";
        }
        if (rawMessage.contains("已有待审核退课申请")) {
            return "提交失败：该课程已有待审核退课申请";
        }
        return "退课失败：" + rawMessage;
    }
}
