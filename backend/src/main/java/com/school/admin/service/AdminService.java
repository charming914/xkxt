package com.school.admin.service;

import com.school.admin.dto.AdminCourseRequest;
import com.school.admin.dto.AdminStudentRequest;
import com.school.admin.dto.AdminTeacherRequest;
import com.school.admin.dto.CourseSelectionSettingRequest;
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
