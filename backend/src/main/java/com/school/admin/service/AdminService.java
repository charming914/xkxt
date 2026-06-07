package com.school.admin.service;

import com.school.admin.dto.AdminCourseRequest;
import com.school.admin.dto.AdminStudentRequest;
import com.school.admin.dto.AdminTeacherRequest;
import com.school.admin.dto.AdminTeachingClassRequest;
import com.school.admin.dto.CourseSelectionSettingRequest;
import com.school.admin.dto.ScoreReviewRequestReviewRequest;
import com.school.admin.dto.ScoreSubmitSettingRequest;
import com.school.admin.dto.SemesterRequest;
import static com.school.common.support.CourseDropReviewSupport.resolveDropCourseMessage;
import static com.school.common.support.CourseDropReviewSupport.resolveReviewStatus;

import com.school.common.dto.CourseDropReviewRequest;
import com.school.admin.dto.CourseSelectionRequestReviewRequest;
import com.school.admin.mapper.AdminMapper;
import com.school.common.exception.BusinessException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    public List<Map<String, Object>> students() {
        return adminMapper.selectStudents();
    }

    public List<Map<String, Object>> teachers() {
        return adminMapper.selectTeachers();
    }

    public List<Map<String, Object>> courses() {
        return adminMapper.selectCourses();
    }

    public List<Map<String, Object>> teachingClasses() {
        return adminMapper.selectTeachingClasses();
    }

    public List<Map<String, Object>> teachingClassStudents(Integer teachingClassId) {
        return adminMapper.selectTeachingClassStudents(teachingClassId);
    }

    public List<Map<String, Object>> courseSelectionStatistics() {
        return adminMapper.selectCourseSelectionStatistics();
    }

    public Map<String, Object> dashboardOverview() {
        return adminMapper.selectDashboardOverview();
    }

    public List<Map<String, Object>> scoredTeachingClasses() {
        return adminMapper.selectScoredTeachingClasses();
    }

    public Map<String, Object> scoreDistribution(Integer teachingClassId) {
        Map<String, Object> distribution = adminMapper.selectScoreDistribution(teachingClassId);
        if (distribution == null || distribution.get("totalCount") == null
            || ((Number) distribution.get("totalCount")).intValue() == 0) {
            throw new BusinessException("该教学班暂无已提交成绩");
        }
        return distribution;
    }

    public List<Map<String, Object>> departments() {
        return adminMapper.selectDepartmentOptions();
    }

    public List<Map<String, Object>> majors() {
        return adminMapper.selectMajorOptions();
    }

    public List<Map<String, Object>> classes() {
        return adminMapper.selectClassOptions();
    }

    public List<Map<String, Object>> semesters() {
        return adminMapper.selectSemesterOptions();
    }

    public List<Map<String, Object>> allSemesters() {
        return adminMapper.selectAllSemesters();
    }

    @Transactional
    public void createSemester(SemesterRequest request) {
        if (request.getCourseSelectStart().compareTo(request.getCourseSelectEnd()) >= 0) {
            throw new BusinessException("选课开始时间必须早于结束时间");
        }
        Integer carryOverFrom = request.getCarryOverFromSemesterId();
        if (carryOverFrom != null && adminMapper.countSemesterById(carryOverFrom) == 0) {
            throw new BusinessException("结转来源学期不存在");
        }
        adminMapper.insertSemester(request);
        Integer newSemesterId = request.getSemesterId();
        if (carryOverFrom != null) {
            adminMapper.carryOverTeachingClasses(carryOverFrom, newSemesterId);
        }
        if (Boolean.TRUE.equals(request.getSetAsCurrent())) {
            adminMapper.clearCurrentSemester();
            adminMapper.markCurrentSemester(newSemesterId);
        }
    }

    @Transactional
    public void setCurrentSemester(Integer semesterId) {
        if (adminMapper.countSemesterById(semesterId) == 0) {
            throw new BusinessException("学期不存在");
        }
        adminMapper.clearCurrentSemester();
        adminMapper.markCurrentSemester(semesterId);
    }

    public List<Map<String, Object>> teacherOptions() {
        return adminMapper.selectTeacherOptions();
    }

    public List<Map<String, Object>> classroomOptions() {
        return adminMapper.selectClassroomOptions();
    }

    public List<Map<String, Object>> courseOptions() {
        return adminMapper.selectCourseOptions();
    }

    @Transactional
    public void createStudent(AdminStudentRequest request) {
        adminMapper.insertStudent(request);
        createUserIfNecessary(request.getStudentId(), request.getStudentName(), "学生", "STUDENT");
    }

    @Transactional
    public void updateStudent(String studentId, AdminStudentRequest request) {
        adminMapper.updateStudent(studentId, request);
        adminMapper.updateSysUserRealName(studentId, request.getStudentName());
    }

    @Transactional
    public void deleteStudent(String studentId) {
        adminMapper.deleteUserRoleByRefId(studentId);
        adminMapper.deleteSysUserByRefId(studentId);
        adminMapper.deleteStudent(studentId);
    }

    @Transactional
    public void createTeacher(AdminTeacherRequest request) {
        adminMapper.insertTeacher(request);
        createUserIfNecessary(request.getTeacherId(), request.getTeacherName(), "教师", "TEACHER");
    }

    @Transactional
    public void updateTeacher(String teacherId, AdminTeacherRequest request) {
        adminMapper.updateTeacher(teacherId, request);
        adminMapper.updateSysUserRealName(teacherId, request.getTeacherName());
    }

    @Transactional
    public void deleteTeacher(String teacherId) {
        adminMapper.deleteUserRoleByRefId(teacherId);
        adminMapper.deleteSysUserByRefId(teacherId);
        adminMapper.deleteTeacher(teacherId);
    }

    public void createCourse(AdminCourseRequest request) {
        adminMapper.insertCourse(request);
    }

    public void updateCourse(String courseId, AdminCourseRequest request) {
        adminMapper.updateCourse(courseId, request);
    }

    public void deleteCourse(String courseId) {
        adminMapper.deleteCourse(courseId);
    }

    public void createTeachingClass(AdminTeachingClassRequest request) {
        adminMapper.insertTeachingClass(request);
    }

    public void updateTeachingClass(Integer teachingClassId, AdminTeachingClassRequest request) {
        adminMapper.updateTeachingClass(teachingClassId, request);
    }

    public void deleteTeachingClass(Integer teachingClassId) {
        if (adminMapper.countSelectedByTeachingClassId(teachingClassId) > 0) {
            throw new BusinessException("该教学班已有学生选课，不能删除");
        }
        adminMapper.deleteTeachingClass(teachingClassId);
    }

    public Map<String, Object> currentSemesterSetting() {
        Map<String, Object> setting = adminMapper.selectCurrentSemesterSetting();
        if (setting == null) {
            throw new BusinessException("当前学期不存在");
        }
        return setting;
    }

    @Transactional
    public void updateCourseSelectionSetting(CourseSelectionSettingRequest request) {
        Map<String, Object> setting = adminMapper.selectCurrentSemesterSetting();
        if (setting == null) {
            throw new BusinessException("当前学期不存在");
        }
        Integer semesterId = ((Number) setting.get("semesterId")).intValue();
        adminMapper.updateCourseSelectionTime(semesterId, request);
    }

    public Map<String, Object> currentScoreSubmitSetting() {
        Map<String, Object> setting = adminMapper.selectCurrentScoreSubmitSetting();
        if (setting == null) {
            throw new BusinessException("当前学期不存在");
        }
        return setting;
    }

    @Transactional
    public void updateScoreSubmitSetting(ScoreSubmitSettingRequest request) {
        Map<String, Object> setting = adminMapper.selectCurrentScoreSubmitSetting();
        if (setting == null) {
            throw new BusinessException("当前学期不存在");
        }
        Integer semesterId = ((Number) setting.get("semesterId")).intValue();
        adminMapper.updateScoreSubmitTime(semesterId, request);
    }

    public List<Map<String, Object>> submittedScores() {
        return adminMapper.selectSubmittedScores();
    }

    public List<Map<String, Object>> scoreChangeRequests() {
        return adminMapper.selectScoreChangeRequests();
    }

    public List<Map<String, Object>> scoreReviewRequests() {
        return adminMapper.selectScoreReviewRequests();
    }

    @Transactional
    public void rollbackSubmittedScore(Long scoreId) {
        if (adminMapper.rollbackSubmittedScore(scoreId) == 0) {
            throw new BusinessException("成绩不存在或已处于可修改状态");
        }
    }

    public List<Map<String, Object>> courseSelectionRequests() {
        return adminMapper.selectCourseSelectionRequests();
    }

    public List<Map<String, Object>> courseDropRequests() {
        return adminMapper.selectCourseDropRequests();
    }

    public List<Map<String, Object>> scheduleAdjustRequests() {
        return adminMapper.selectScheduleAdjustRequests();
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewScoreChangeRequest(
        Long scoreChangeRequestId,
        CourseSelectionRequestReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> scoreChangeRequest = adminMapper.selectScoreChangeRequestById(scoreChangeRequestId);
        if (scoreChangeRequest == null) {
            throw new BusinessException("成绩修改申请不存在");
        }
        if (!"待审核".equals(scoreChangeRequest.get("requestStatus"))) {
            throw new BusinessException("该成绩修改申请已处理，请勿重复审核");
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String action = request.getAction();
        if ("APPROVE".equalsIgnoreCase(action)) {
            Long scoreId = ((Number) scoreChangeRequest.get("scoreId")).longValue();
            adminMapper.rollbackSubmittedScore(scoreId);
            adminMapper.updateScoreChangeRequestStatus(scoreChangeRequestId, "已通过", request.getReviewComment(), adminUserId);
            return;
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            adminMapper.updateScoreChangeRequestStatus(scoreChangeRequestId, "已驳回", request.getReviewComment(), adminUserId);
            return;
        }

        throw new BusinessException("不支持的审核动作");
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewScoreReviewRequest(
        Long requestId,
        ScoreReviewRequestReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> scoreReviewRequest = adminMapper.selectScoreReviewRequestById(requestId);
        if (scoreReviewRequest == null) {
            throw new BusinessException("成绩复核申请不存在");
        }
        if (!"待审核".equals(scoreReviewRequest.get("requestStatus"))) {
            throw new BusinessException("该成绩复核申请已处理，请勿重复审核");
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String reviewStatus = resolveReviewStatus(request.getAction());
        adminMapper.updateScoreReviewRequestStatus(
            requestId,
            reviewStatus,
            request.getReviewComment(),
            adminUserId
        );
        adminMapper.insertOperationLog(
            adminUserId,
            "成绩复核审核",
            "管理员处理成绩复核申请，申请编号：" + requestId + "，结果：" + reviewStatus
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewScheduleAdjustRequest(
        Long adjustRequestId,
        CourseSelectionRequestReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> adjustRequest = adminMapper.selectScheduleAdjustRequestById(adjustRequestId);
        if (adjustRequest == null) {
            throw new BusinessException("调课申请不存在");
        }
        if (!"待审核".equals(adjustRequest.get("requestStatus"))) {
            throw new BusinessException("该调课申请已处理，请勿重复审核");
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String action = request.getAction();
        if ("APPROVE".equalsIgnoreCase(action)) {
            Integer teachingClassId = ((Number) adjustRequest.get("teachingClassId")).intValue();
            Integer weekDay = ((Number) adjustRequest.get("requestedWeekDay")).intValue();
            String sectionDesc = (String) adjustRequest.get("requestedSectionDesc");
            String weekRange = (String) adjustRequest.get("requestedWeekRange");
            String classroomId = (String) adjustRequest.get("requestedClassroomId");
            if (adminMapper.countTeachingClassRoomConflict(teachingClassId, weekDay, sectionDesc, classroomId) > 0) {
                throw new BusinessException("审核失败：目标教室在该时间段已被占用");
            }
            adminMapper.applyScheduleAdjustRequest(teachingClassId, weekDay, sectionDesc, weekRange, classroomId);
            adminMapper.updateScheduleAdjustRequestStatus(adjustRequestId, "已通过", request.getReviewComment(), adminUserId);
            return;
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            adminMapper.updateScheduleAdjustRequestStatus(adjustRequestId, "已驳回", request.getReviewComment(), adminUserId);
            return;
        }

        throw new BusinessException("不支持的审核动作");
    }

    @Transactional
    public void reviewCourseSelectionRequest(
        Long requestId,
        CourseSelectionRequestReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> selectionRequest = adminMapper.selectCourseSelectionRequestById(requestId);
        if (selectionRequest == null) {
            throw new BusinessException("补选申请不存在");
        }
        String requestStatus = (String) selectionRequest.get("requestStatus");
        if (!"待审核".equals(requestStatus)) {
            throw new BusinessException("该补选申请已处理，请勿重复审核");
        }

        Integer reviewedBy = adminMapper.selectUserIdByUsername(adminUsername);
        if (reviewedBy == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String action = request.getAction();
        if ("APPROVE".equalsIgnoreCase(action)) {
            String studentId = (String) selectionRequest.get("studentId");
            Integer teachingClassId = ((Number) selectionRequest.get("teachingClassId")).intValue();
            if (adminMapper.countSelectedCourse(studentId, teachingClassId) > 0) {
                throw new BusinessException("该学生已选上此课程");
            }
            if (adminMapper.countStudentTimeConflict(studentId, teachingClassId) > 0) {
                throw new BusinessException("审核失败：该课程与学生当前课表存在时间冲突");
            }
            if (adminMapper.countDroppedCourse(studentId, teachingClassId) > 0) {
                adminMapper.deleteScore(studentId, teachingClassId);
                adminMapper.restoreDroppedCourseByAdmin(studentId, teachingClassId);
            } else {
                adminMapper.insertCourseSelectionByAdmin(studentId, teachingClassId);
            }
            adminMapper.increaseSelectedCount(teachingClassId);
            adminMapper.updateCourseSelectionRequestStatus(requestId, "已通过", request.getReviewComment(), reviewedBy);
            return;
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            adminMapper.updateCourseSelectionRequestStatus(requestId, "已驳回", request.getReviewComment(), reviewedBy);
            return;
        }

        throw new BusinessException("不支持的审核动作");
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewCourseDropRequest(
        Long dropRequestId,
        CourseDropReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> dropRequest = adminMapper.selectCourseDropRequestById(dropRequestId);
        if (dropRequest == null) {
            throw new BusinessException("退课申请不存在");
        }
        if (!"待审核".equals(dropRequest.get("requestStatus"))) {
            throw new BusinessException("该退课申请已处理，请勿重复审核");
        }
        if (!"待审核".equals(dropRequest.get("adminStatus"))) {
            throw new BusinessException("你已审核过该退课申请");
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String adminStatus = resolveReviewStatus(request.getAction());
        adminMapper.updateAdminDropReview(
            dropRequestId,
            adminStatus,
            request.getReviewComment(),
            adminUserId
        );
        dropCourseIfFullyApproved(dropRequestId, dropRequest);
    }

    private void dropCourseIfFullyApproved(Long dropRequestId, Map<String, Object> dropRequest) {
        String requestStatus = adminMapper.selectDropRequestStatus(dropRequestId);
        if (!"已通过".equals(requestStatus)) {
            return;
        }
        String studentId = (String) dropRequest.get("studentId");
        Integer teachingClassId = ((Number) dropRequest.get("teachingClassId")).intValue();
        try {
            adminMapper.dropSelectedCourse(studentId, teachingClassId);
            adminMapper.deleteScore(studentId, teachingClassId);
        } catch (RuntimeException e) {
            throw new BusinessException(resolveDropCourseMessage(e.getMessage()));
        }
    }

    private void createUserIfNecessary(String username, String realName, String userType, String roleCode) {
        Integer roleId = adminMapper.selectRoleIdByCode(roleCode);
        if (roleId == null) {
            throw new BusinessException("角色不存在: " + roleCode);
        }
        adminMapper.insertSysUser(username, passwordEncoder.encode("123456"), realName, userType, username);
        Integer userId = adminMapper.selectUserIdByUsername(username);
        if (userId == null) {
            throw new BusinessException("创建系统用户失败");
        }
        adminMapper.insertSysUserRole(userId, roleId);
    }
}
