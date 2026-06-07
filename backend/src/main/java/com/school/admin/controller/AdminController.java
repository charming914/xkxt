package com.school.admin.controller;

import com.school.admin.dto.AdminCourseRequest;
import com.school.admin.dto.AdminStudentRequest;
import com.school.admin.dto.AdminTeacherRequest;
import com.school.admin.dto.AdminTeachingClassRequest;
import com.school.admin.dto.CourseSelectionSettingRequest;
import com.school.admin.dto.CourseSelectionRequestReviewRequest;
import com.school.admin.dto.ScoreSubmitSettingRequest;
import com.school.admin.service.AdminService;
import com.school.common.api.ApiResponse;
import com.school.common.dto.CourseDropReviewRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/students")
    public ApiResponse<List<Map<String, Object>>> students() {
        return ApiResponse.success(adminService.students());
    }

    @PostMapping("/students")
    public ApiResponse<Void> createStudent(@Valid @RequestBody AdminStudentRequest request) {
        adminService.createStudent(request);
        return ApiResponse.success("学生新增成功，默认登录密码为123456", null);
    }

    @PutMapping("/students/{studentId}")
    public ApiResponse<Void> updateStudent(@PathVariable String studentId, @Valid @RequestBody AdminStudentRequest request) {
        adminService.updateStudent(studentId, request);
        return ApiResponse.success("学生信息修改成功", null);
    }

    @DeleteMapping("/students/{studentId}")
    public ApiResponse<Void> deleteStudent(@PathVariable String studentId) {
        adminService.deleteStudent(studentId);
        return ApiResponse.success("学生删除成功", null);
    }

    @GetMapping("/teachers")
    public ApiResponse<List<Map<String, Object>>> teachers() {
        return ApiResponse.success(adminService.teachers());
    }

    @PostMapping("/teachers")
    public ApiResponse<Void> createTeacher(@Valid @RequestBody AdminTeacherRequest request) {
        adminService.createTeacher(request);
        return ApiResponse.success("教师新增成功，默认登录密码为123456", null);
    }

    @PutMapping("/teachers/{teacherId}")
    public ApiResponse<Void> updateTeacher(@PathVariable String teacherId, @Valid @RequestBody AdminTeacherRequest request) {
        adminService.updateTeacher(teacherId, request);
        return ApiResponse.success("教师信息修改成功", null);
    }

    @DeleteMapping("/teachers/{teacherId}")
    public ApiResponse<Void> deleteTeacher(@PathVariable String teacherId) {
        adminService.deleteTeacher(teacherId);
        return ApiResponse.success("教师删除成功", null);
    }

    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> courses() {
        return ApiResponse.success(adminService.courses());
    }

    @GetMapping("/teaching-classes")
    public ApiResponse<List<Map<String, Object>>> teachingClasses() {
        return ApiResponse.success(adminService.teachingClasses());
    }

    @GetMapping("/teaching-classes/{teachingClassId}/students")
    public ApiResponse<List<Map<String, Object>>> teachingClassStudents(@PathVariable Integer teachingClassId) {
        return ApiResponse.success(adminService.teachingClassStudents(teachingClassId));
    }

    @PostMapping("/courses")
    public ApiResponse<Void> createCourse(@Valid @RequestBody AdminCourseRequest request) {
        adminService.createCourse(request);
        return ApiResponse.success("课程新增成功", null);
    }

    @PutMapping("/courses/{courseId}")
    public ApiResponse<Void> updateCourse(@PathVariable String courseId, @Valid @RequestBody AdminCourseRequest request) {
        adminService.updateCourse(courseId, request);
        return ApiResponse.success("课程修改成功", null);
    }

    @DeleteMapping("/courses/{courseId}")
    public ApiResponse<Void> deleteCourse(@PathVariable String courseId) {
        adminService.deleteCourse(courseId);
        return ApiResponse.success("课程删除成功", null);
    }

    @PostMapping("/teaching-classes")
    public ApiResponse<Void> createTeachingClass(@Valid @RequestBody AdminTeachingClassRequest request) {
        adminService.createTeachingClass(request);
        return ApiResponse.success("教学班新增成功", null);
    }

    @PutMapping("/teaching-classes/{teachingClassId}")
    public ApiResponse<Void> updateTeachingClass(
        @PathVariable Integer teachingClassId,
        @Valid @RequestBody AdminTeachingClassRequest request
    ) {
        adminService.updateTeachingClass(teachingClassId, request);
        return ApiResponse.success("教学班修改成功", null);
    }

    @DeleteMapping("/teaching-classes/{teachingClassId}")
    public ApiResponse<Void> deleteTeachingClass(@PathVariable Integer teachingClassId) {
        adminService.deleteTeachingClass(teachingClassId);
        return ApiResponse.success("教学班删除成功", null);
    }

    @GetMapping("/statistics/course-selection")
    public ApiResponse<List<Map<String, Object>>> courseSelectionStatistics() {
        return ApiResponse.success(adminService.courseSelectionStatistics());
    }

    @GetMapping("/departments")
    public ApiResponse<List<Map<String, Object>>> departments() {
        return ApiResponse.success(adminService.departments());
    }

    @GetMapping("/majors")
    public ApiResponse<List<Map<String, Object>>> majors() {
        return ApiResponse.success(adminService.majors());
    }

    @GetMapping("/classes")
    public ApiResponse<List<Map<String, Object>>> classes() {
        return ApiResponse.success(adminService.classes());
    }

    @GetMapping("/semesters")
    public ApiResponse<List<Map<String, Object>>> semesters() {
        return ApiResponse.success(adminService.semesters());
    }

    @GetMapping("/teacher-options")
    public ApiResponse<List<Map<String, Object>>> teacherOptions() {
        return ApiResponse.success(adminService.teacherOptions());
    }

    @GetMapping("/classrooms")
    public ApiResponse<List<Map<String, Object>>> classrooms() {
        return ApiResponse.success(adminService.classroomOptions());
    }

    @GetMapping("/course-options")
    public ApiResponse<List<Map<String, Object>>> courseOptions() {
        return ApiResponse.success(adminService.courseOptions());
    }

    @GetMapping("/course-selection-setting")
    public ApiResponse<Map<String, Object>> currentSemesterSetting() {
        return ApiResponse.success(adminService.currentSemesterSetting());
    }

    @PutMapping("/course-selection-setting")
    public ApiResponse<Void> updateCourseSelectionSetting(@Valid @RequestBody CourseSelectionSettingRequest request) {
        adminService.updateCourseSelectionSetting(request);
        return ApiResponse.success("选课设置更新成功", null);
    }

    @GetMapping("/score-submit-setting")
    public ApiResponse<Map<String, Object>> currentScoreSubmitSetting() {
        return ApiResponse.success(adminService.currentScoreSubmitSetting());
    }

    @PutMapping("/score-submit-setting")
    public ApiResponse<Void> updateScoreSubmitSetting(@Valid @RequestBody ScoreSubmitSettingRequest request) {
        adminService.updateScoreSubmitSetting(request);
        return ApiResponse.success("成绩提交时间更新成功", null);
    }

    @GetMapping("/scores/submitted")
    public ApiResponse<List<Map<String, Object>>> submittedScores() {
        return ApiResponse.success(adminService.submittedScores());
    }

    @GetMapping("/score-change-requests")
    public ApiResponse<List<Map<String, Object>>> scoreChangeRequests() {
        return ApiResponse.success(adminService.scoreChangeRequests());
    }

    @PutMapping("/scores/{scoreId}/rollback")
    public ApiResponse<Void> rollbackSubmittedScore(@PathVariable Long scoreId) {
        adminService.rollbackSubmittedScore(scoreId);
        return ApiResponse.success("成绩已退回，可由教师重新修改提交", null);
    }

    @GetMapping("/course-selection-requests")
    public ApiResponse<List<Map<String, Object>>> courseSelectionRequests() {
        return ApiResponse.success(adminService.courseSelectionRequests());
    }

    @GetMapping("/course-drop-requests")
    public ApiResponse<List<Map<String, Object>>> courseDropRequests() {
        return ApiResponse.success(adminService.courseDropRequests());
    }

    @GetMapping("/schedule-adjust-requests")
    public ApiResponse<List<Map<String, Object>>> scheduleAdjustRequests() {
        return ApiResponse.success(adminService.scheduleAdjustRequests());
    }

    @PutMapping("/course-selection-requests/{requestId}/review")
    public ApiResponse<Void> reviewCourseSelectionRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody CourseSelectionRequestReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewCourseSelectionRequest(requestId, request, username);
        return ApiResponse.success("补选申请处理成功", null);
    }

    @PutMapping("/course-drop-requests/{dropRequestId}/review")
    public ApiResponse<Void> reviewCourseDropRequest(
        @PathVariable Long dropRequestId,
        @Valid @RequestBody CourseDropReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewCourseDropRequest(dropRequestId, request, username);
        return ApiResponse.success("退课申请处理成功", null);
    }

    @PutMapping("/schedule-adjust-requests/{adjustRequestId}/review")
    public ApiResponse<Void> reviewScheduleAdjustRequest(
        @PathVariable Long adjustRequestId,
        @Valid @RequestBody CourseSelectionRequestReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewScheduleAdjustRequest(adjustRequestId, request, username);
        return ApiResponse.success("调课申请处理成功", null);
    }

    @PutMapping("/score-change-requests/{scoreChangeRequestId}/review")
    public ApiResponse<Void> reviewScoreChangeRequest(
        @PathVariable Long scoreChangeRequestId,
        @Valid @RequestBody CourseSelectionRequestReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewScoreChangeRequest(scoreChangeRequestId, request, username);
        return ApiResponse.success("成绩修改申请处理成功", null);
    }
}
