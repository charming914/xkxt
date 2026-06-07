package com.school.admin.service;

import com.school.admin.dto.AdminCourseRequest;
import com.school.admin.dto.AdminStudentRequest;
import com.school.admin.dto.AdminTeacherRequest;
import com.school.admin.dto.AdminTeachingClassRequest;
import com.school.admin.dto.CourseSelectionSettingRequest;
import com.school.admin.dto.CourseDropRequestAdminReviewRequest;
import com.school.admin.dto.CourseSelectionRequestReviewRequest;
import com.school.admin.dto.ScoreReviewRequestReviewRequest;
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
        adminMapper.deleteTeachingClass(teachingClassId);
    }

    public Map<String, Object> currentSemesterSetting() {
        Map<String, Object> setting = adminMapper.selectCurrentSemesterSetting();
        if (setting == null) {
            throw new BusinessException("当前学期不存在");
        }
        Integer semesterId = ((Number) setting.get("semesterId")).intValue();
        setting.put("teachingClassStatus", adminMapper.selectTeachingClassStatusBySemesterId(semesterId));
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
        adminMapper.updateTeachingClassStatusBySemesterId(semesterId, request.getTeachingClassStatus());
    }

    public List<Map<String, Object>> courseSelectionRequests() {
        return adminMapper.selectCourseSelectionRequests();
    }

    public List<Map<String, Object>> courseDropRequests() {
        return adminMapper.selectCourseDropRequests();
    }

    public List<Map<String, Object>> scoreReviewRequests() {
        return adminMapper.selectScoreReviewRequests();
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

        Integer reviewedBy = resolveAdminUserId(adminUsername);
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
            adminMapper.insertCourseSelectionByAdmin(studentId, teachingClassId);
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
        CourseDropRequestAdminReviewRequest request,
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

        Integer adminUserId = resolveAdminUserId(adminUsername);

        String adminStatus = resolveReviewStatus(request.getAction());
        adminMapper.updateAdminDropReview(
            dropRequestId,
            adminStatus,
            request.getReviewComment(),
            adminUserId
        );
        dropCourseIfFullyApproved(dropRequestId, dropRequest);
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

        Integer adminUserId = resolveAdminUserId(adminUsername);
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
        String requestStatus = adminMapper.selectDropRequestStatus(dropRequestId);
        if (!"已通过".equals(requestStatus)) {
            return;
        }
        String studentId = (String) dropRequest.get("studentId");
        Integer teachingClassId = ((Number) dropRequest.get("teachingClassId")).intValue();
        try {
            adminMapper.dropSelectedCourse(studentId, teachingClassId);
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

    private Integer resolveAdminUserId(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("未识别登录身份，请重新登录");
        }
        Integer adminUserId = adminMapper.selectAdminUserIdByUsername(username);
        if (adminUserId == null) {
            throw new BusinessException("当前账号不是管理员身份");
        }
        return adminUserId;
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
