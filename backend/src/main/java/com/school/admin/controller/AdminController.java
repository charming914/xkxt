package com.school.admin.controller;

import com.school.admin.dto.AcademicRequestReviewRequest;
import com.school.admin.dto.AdminCourseRequest;
import com.school.admin.dto.AdminStudentRequest;
import com.school.admin.dto.AdminTeacherRequest;
import com.school.admin.dto.AdminTeachingClassRequest;
import com.school.admin.dto.CourseSelectionSettingRequest;
import com.school.admin.dto.CourseSelectionRequestReviewRequest;
import com.school.admin.dto.ExamUpdateRequest;
import com.school.admin.dto.MakeupExamArrangeRequest;
import com.school.admin.dto.ScoreReviewRequestReviewRequest;
import com.school.admin.dto.ScoreSubmitSettingRequest;
import com.school.admin.dto.SemesterRequest;
import com.school.admin.service.AdminService;
import com.school.common.api.ApiResponse;
import com.school.common.dto.CourseDropReviewRequest;
import com.school.log.OperationLog;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    @OperationLog(type = "学生管理", action = "新增学生")
    public ApiResponse<Void> createStudent(@Valid @RequestBody AdminStudentRequest request) {
        adminService.createStudent(request);
        return ApiResponse.success("学生新增成功，默认登录密码为123456", null);
    }

    @PutMapping("/students/{studentId}")
    @OperationLog(type = "学生管理", action = "修改学生")
    public ApiResponse<Void> updateStudent(@PathVariable String studentId, @Valid @RequestBody AdminStudentRequest request) {
        adminService.updateStudent(studentId, request);
        return ApiResponse.success("学生信息修改成功", null);
    }

    @DeleteMapping("/students/{studentId}")
    @OperationLog(type = "学生管理", action = "删除学生")
    public ApiResponse<Void> deleteStudent(@PathVariable String studentId) {
        adminService.deleteStudent(studentId);
        return ApiResponse.success("学生删除成功", null);
    }

    @GetMapping("/teachers")
    public ApiResponse<List<Map<String, Object>>> teachers() {
        return ApiResponse.success(adminService.teachers());
    }

    @PostMapping("/teachers")
    @OperationLog(type = "教师管理", action = "新增教师")
    public ApiResponse<Void> createTeacher(@Valid @RequestBody AdminTeacherRequest request) {
        adminService.createTeacher(request);
        return ApiResponse.success("教师新增成功，默认登录密码为123456", null);
    }

    @PutMapping("/teachers/{teacherId}")
    @OperationLog(type = "教师管理", action = "修改教师")
    public ApiResponse<Void> updateTeacher(@PathVariable String teacherId, @Valid @RequestBody AdminTeacherRequest request) {
        adminService.updateTeacher(teacherId, request);
        return ApiResponse.success("教师信息修改成功", null);
    }

    @DeleteMapping("/teachers/{teacherId}")
    @OperationLog(type = "教师管理", action = "删除教师")
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
    @OperationLog(type = "课程管理", action = "新增课程")
    public ApiResponse<Void> createCourse(@Valid @RequestBody AdminCourseRequest request) {
        adminService.createCourse(request);
        return ApiResponse.success("课程新增成功", null);
    }

    @PutMapping("/courses/{courseId}")
    @OperationLog(type = "课程管理", action = "修改课程")
    public ApiResponse<Void> updateCourse(@PathVariable String courseId, @Valid @RequestBody AdminCourseRequest request) {
        adminService.updateCourse(courseId, request);
        return ApiResponse.success("课程修改成功", null);
    }

    @DeleteMapping("/courses/{courseId}")
    @OperationLog(type = "课程管理", action = "删除课程")
    public ApiResponse<Void> deleteCourse(@PathVariable String courseId) {
        adminService.deleteCourse(courseId);
        return ApiResponse.success("课程删除成功", null);
    }

    @PostMapping("/teaching-classes")
    @OperationLog(type = "教学班管理", action = "新增教学班")
    public ApiResponse<Void> createTeachingClass(@Valid @RequestBody AdminTeachingClassRequest request) {
        adminService.createTeachingClass(request);
        return ApiResponse.success("教学班新增成功", null);
    }

    @PutMapping("/teaching-classes/{teachingClassId}")
    @OperationLog(type = "教学班管理", action = "修改教学班")
    public ApiResponse<Void> updateTeachingClass(
        @PathVariable Integer teachingClassId,
        @Valid @RequestBody AdminTeachingClassRequest request
    ) {
        adminService.updateTeachingClass(teachingClassId, request);
        return ApiResponse.success("教学班修改成功", null);
    }

    @DeleteMapping("/teaching-classes/{teachingClassId}")
    @OperationLog(type = "教学班管理", action = "删除教学班")
    public ApiResponse<Void> deleteTeachingClass(@PathVariable Integer teachingClassId) {
        adminService.deleteTeachingClass(teachingClassId);
        return ApiResponse.success("教学班删除成功", null);
    }

    @GetMapping("/statistics/course-selection")
    public ApiResponse<List<Map<String, Object>>> courseSelectionStatistics() {
        return ApiResponse.success(adminService.courseSelectionStatistics());
    }

    @GetMapping("/dashboard/overview")
    public ApiResponse<Map<String, Object>> dashboardOverview() {
        return ApiResponse.success(adminService.dashboardOverview());
    }

    @GetMapping("/statistics/scored-classes")
    public ApiResponse<List<Map<String, Object>>> scoredTeachingClasses() {
        return ApiResponse.success(adminService.scoredTeachingClasses());
    }

    @GetMapping("/statistics/score-distribution/{teachingClassId}")
    public ApiResponse<Map<String, Object>> scoreDistribution(@PathVariable Integer teachingClassId) {
        return ApiResponse.success(adminService.scoreDistribution(teachingClassId));
    }

    @GetMapping("/cultivation-plan")
    public ApiResponse<List<Map<String, Object>>> cultivationPlanRequirements() {
        return ApiResponse.success(adminService.cultivationPlanRequirements());
    }

    @PostMapping("/cultivation-plan/import")
    @OperationLog(type = "培养方案", action = "导入培养方案")
    public ApiResponse<Integer> importCultivationPlan(@RequestParam("file") MultipartFile file) {
        int imported = adminService.importCultivationPlan(file);
        return ApiResponse.success("培养方案导入成功", imported);
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

    @GetMapping("/semester-management")
    public ApiResponse<List<Map<String, Object>>> allSemesters() {
        return ApiResponse.success(adminService.allSemesters());
    }

    @PostMapping("/semesters")
    @OperationLog(type = "学期管理", action = "新建学期")
    public ApiResponse<Void> createSemester(@Valid @RequestBody SemesterRequest request) {
        adminService.createSemester(request);
        return ApiResponse.success("学期创建成功", null);
    }

    @PutMapping("/semesters/{semesterId}")
    @OperationLog(type = "学期管理", action = "编辑学期")
    public ApiResponse<Void> updateSemester(@PathVariable Integer semesterId,
                                            @Valid @RequestBody SemesterRequest request) {
        adminService.updateSemester(semesterId, request);
        return ApiResponse.success("学期更新成功", null);
    }

    @DeleteMapping("/semesters/{semesterId}")
    @OperationLog(type = "学期管理", action = "删除学期")
    public ApiResponse<Void> deleteSemester(@PathVariable Integer semesterId) {
        adminService.deleteSemester(semesterId);
        return ApiResponse.success("学期删除成功", null);
    }

    @PutMapping("/semesters/{semesterId}/current")
    @OperationLog(type = "学期管理", action = "切换当前学期")
    public ApiResponse<Void> setCurrentSemester(@PathVariable Integer semesterId) {
        adminService.setCurrentSemester(semesterId);
        return ApiResponse.success("当前学期切换成功", null);
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
    @OperationLog(type = "选课设置", action = "更新选课设置")
    public ApiResponse<Void> updateCourseSelectionSetting(@Valid @RequestBody CourseSelectionSettingRequest request) {
        adminService.updateCourseSelectionSetting(request);
        return ApiResponse.success("选课设置更新成功", null);
    }

    @GetMapping("/score-submit-setting")
    public ApiResponse<Map<String, Object>> currentScoreSubmitSetting() {
        return ApiResponse.success(adminService.currentScoreSubmitSetting());
    }

    @PutMapping("/score-submit-setting")
    @OperationLog(type = "成绩设置", action = "更新成绩提交时间")
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

    @GetMapping("/score-review-requests")
    public ApiResponse<List<Map<String, Object>>> scoreReviewRequests() {
        return ApiResponse.success(adminService.scoreReviewRequests());
    }

    @GetMapping("/makeup-requests")
    public ApiResponse<List<Map<String, Object>>> makeupExamRequests() {
        return ApiResponse.success(adminService.makeupExamRequests());
    }

    @PutMapping("/makeup-requests/{requestId}/review")
    @OperationLog(type = "补考教室安排", action = "安排补考教室")
    public ApiResponse<Void> reviewMakeupExamRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody MakeupExamArrangeRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewMakeupExamRequest(requestId, request, username);
        return ApiResponse.success("补考教室安排处理成功", null);
    }

    @PutMapping("/makeup-requests/{requestId}/result")
    @OperationLog(type = "补考管理", action = "登记补考结果")
    public ApiResponse<Void> updateMakeupExamResult(
        @PathVariable Long requestId,
        @Valid @RequestBody AcademicRequestReviewRequest request
    ) {
        adminService.updateMakeupExamResult(requestId, request);
        return ApiResponse.success("补考结果登记成功", null);
    }

    @GetMapping("/retake-requests")
    public ApiResponse<List<Map<String, Object>>> retakeRequests() {
        return ApiResponse.success(adminService.retakeRequests());
    }

    @PutMapping("/retake-requests/{requestId}/review")
    @OperationLog(type = "重修审核", action = "审核重修申请")
    public ApiResponse<Void> reviewRetakeRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody AcademicRequestReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewRetakeRequest(requestId, request, username);
        return ApiResponse.success("重修申请处理成功", null);
    }

    @PutMapping("/scores/{scoreId}/rollback")
    @OperationLog(type = "成绩管理", action = "退回成绩")
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
    @OperationLog(type = "补选审核", action = "审核补选申请")
    public ApiResponse<Void> reviewCourseSelectionRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody CourseSelectionRequestReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewCourseSelectionRequest(requestId, request, username);
        return ApiResponse.success("补选申请处理成功", null);
    }

    @PutMapping("/course-drop-requests/{dropRequestId}/review")
    @OperationLog(type = "退课审核", action = "审核退课申请")
    public ApiResponse<Void> reviewCourseDropRequest(
        @PathVariable Long dropRequestId,
        @Valid @RequestBody CourseDropReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewCourseDropRequest(dropRequestId, request, username);
        return ApiResponse.success("退课申请处理成功", null);
    }

    @PutMapping("/schedule-adjust-requests/{adjustRequestId}/review")
    @OperationLog(type = "调课审核", action = "审核调课申请")
    public ApiResponse<Void> reviewScheduleAdjustRequest(
        @PathVariable Long adjustRequestId,
        @Valid @RequestBody CourseSelectionRequestReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewScheduleAdjustRequest(adjustRequestId, request, username);
        return ApiResponse.success("调课申请处理成功", null);
    }

    @PutMapping("/score-change-requests/{scoreChangeRequestId}/review")
    @OperationLog(type = "成绩修改审核", action = "审核成绩修改申请")
    public ApiResponse<Void> reviewScoreChangeRequest(
        @PathVariable Long scoreChangeRequestId,
        @Valid @RequestBody CourseSelectionRequestReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewScoreChangeRequest(scoreChangeRequestId, request, username);
        return ApiResponse.success("成绩修改申请处理成功", null);
    }

    @PutMapping("/score-review-requests/{requestId}/review")
    public ApiResponse<Void> reviewScoreReviewRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody ScoreReviewRequestReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewScoreReviewRequest(requestId, request, username);
        return ApiResponse.success("成绩复核申请处理成功", null);
    }

    // ==================== 考试管理 ====================

    @GetMapping("/exams")
    public ApiResponse<List<Map<String, Object>>> exams() {
        return ApiResponse.success(adminService.exams());
    }

    @PostMapping("/exams/generate")
    @OperationLog(type = "考试管理", action = "生成考试安排")
    public ApiResponse<Integer> generateExams(
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        int created = adminService.generateExams(username);
        return ApiResponse.success("已生成 " + created + " 场考试安排", created);
    }

    @PutMapping("/exams/{examId}")
    @OperationLog(type = "考试管理", action = "修改考试安排")
    public ApiResponse<Void> updateExam(
        @PathVariable Long examId,
        @Valid @RequestBody ExamUpdateRequest request
    ) {
        adminService.updateExam(examId, request);
        return ApiResponse.success("考试安排修改成功", null);
    }

    @DeleteMapping("/exams/{examId}")
    @OperationLog(type = "考试管理", action = "删除考试安排")
    public ApiResponse<Void> deleteExam(@PathVariable Long examId) {
        adminService.deleteExam(examId);
        return ApiResponse.success("考试安排删除成功", null);
    }

    @GetMapping("/exam-change-requests")
    public ApiResponse<List<Map<String, Object>>> examChangeRequests() {
        return ApiResponse.success(adminService.examChangeRequests());
    }

    @PutMapping("/exam-change-requests/{examChangeRequestId}/review")
    @OperationLog(type = "考试修改审核", action = "审核考试修改申请")
    public ApiResponse<Void> reviewExamChangeRequest(
        @PathVariable Long examChangeRequestId,
        @Valid @RequestBody CourseSelectionRequestReviewRequest request,
        @RequestHeader(value = "X-Username", defaultValue = "admin") String username
    ) {
        adminService.reviewExamChangeRequest(examChangeRequestId, request, username);
        return ApiResponse.success("考试修改申请处理成功", null);
    }
}
