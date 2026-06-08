package com.school.student.controller;

import com.school.common.api.ApiResponse;
import com.school.student.dto.AcademicRequestCreateRequest;
import com.school.student.dto.CourseDropRequestCreateRequest;
import com.school.student.dto.CourseSelectionRequestCreateRequest;
import com.school.student.dto.ScoreReviewRequestCreateRequest;
import com.school.student.dto.SelectCourseRequest;
import com.school.student.dto.StudentCourseQueryRequest;
import com.school.student.dto.StudentProfileUpdateRequest;
import com.school.student.service.StudentService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.profile(username, studentId));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId,
        @Valid @RequestBody StudentProfileUpdateRequest request
    ) {
        studentService.updateProfile(username, studentId, request);
        return ApiResponse.success("个人信息保存成功", null);
    }

    @GetMapping("/todos")
    public ApiResponse<Map<String, Object>> todos(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.todoSummary(username, studentId));
    }

    @GetMapping("/current-semester")
    public ApiResponse<Map<String, Object>> currentSemester() {
        return ApiResponse.success(studentService.currentSemester());
    }

    @GetMapping("/courses/available")
    public ApiResponse<List<Map<String, Object>>> availableCourses(
        StudentCourseQueryRequest query,
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.availableCourses(query, username, studentId));
    }

    @GetMapping("/course-plans")
    public ApiResponse<List<Map<String, Object>>> coursePlans(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.coursePlans(username, studentId));
    }

    @PostMapping("/course-plans")
    public ApiResponse<Void> createCoursePlan(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId,
        @Valid @RequestBody SelectCourseRequest request
    ) {
        studentService.createCoursePlan(username, studentId, request);
        return ApiResponse.success("已加入选课计划", null);
    }

    @DeleteMapping("/course-plans/{planId}")
    public ApiResponse<Void> deleteCoursePlan(
        @PathVariable Long planId,
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        studentService.deleteCoursePlan(username, studentId, planId);
        return ApiResponse.success("已移出选课计划", null);
    }

    @GetMapping("/selected-courses")
    public ApiResponse<List<Map<String, Object>>> selectedCourses(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.selectedCourses(username, studentId));
    }

    @PostMapping("/course-selections")
    public ApiResponse<Void> selectCourse(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId,
        @Valid @RequestBody SelectCourseRequest request
    ) {
        studentService.selectCourse(username, studentId, request);
        return ApiResponse.success("选课成功", null);
    }

    @DeleteMapping("/course-selections/{teachingClassId}")
    public ApiResponse<Void> dropCourse(
        @PathVariable Integer teachingClassId,
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        studentService.dropCourse(username, studentId, teachingClassId);
        return ApiResponse.success("退课成功", null);
    }

    @GetMapping("/timetable")
    public ApiResponse<List<Map<String, Object>>> timetable(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.timetable(username, studentId));
    }

    @GetMapping("/exams")
    public ApiResponse<List<Map<String, Object>>> exams(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.exams(username, studentId));
    }

    @GetMapping("/scores")
    public ApiResponse<List<Map<String, Object>>> scores(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.scores(username, studentId));
    }

    @PostMapping("/course-selection-requests")
    public ApiResponse<Void> createCourseSelectionRequest(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId,
        @Valid @RequestBody CourseSelectionRequestCreateRequest request
    ) {
        studentService.createCourseSelectionRequest(username, studentId, request);
        return ApiResponse.success("补选申请已提交", null);
    }

    @GetMapping("/course-selection-requests")
    public ApiResponse<List<Map<String, Object>>> courseSelectionRequests(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.courseSelectionRequests(username, studentId));
    }

    @GetMapping("/drop-courses")
    public ApiResponse<List<Map<String, Object>>> dropCourses(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.dropCourses(username, studentId));
    }

    @PostMapping("/drop-requests")
    public ApiResponse<Void> createCourseDropRequest(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId,
        @Valid @RequestBody CourseDropRequestCreateRequest request
    ) {
        studentService.createCourseDropRequest(username, studentId, request);
        return ApiResponse.success("退课申请已提交", null);
    }

    @GetMapping("/drop-requests")
    public ApiResponse<List<Map<String, Object>>> courseDropRequests(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.courseDropRequests(username, studentId));
    }

    @PostMapping("/score-review-requests")
    public ApiResponse<Void> createScoreReviewRequest(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId,
        @Valid @RequestBody ScoreReviewRequestCreateRequest request
    ) {
        studentService.createScoreReviewRequest(username, studentId, request);
        return ApiResponse.success("成绩复核申请已提交", null);
    }

    @GetMapping("/score-review-requests")
    public ApiResponse<List<Map<String, Object>>> scoreReviewRequests(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.scoreReviewRequests(username, studentId));
    }

    @GetMapping("/academic-remediation/scores")
    public ApiResponse<List<Map<String, Object>>> academicRemediationScores(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.academicRemediationScores(username, studentId));
    }

    @GetMapping("/makeup-requests")
    public ApiResponse<List<Map<String, Object>>> makeupExamRequests(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.makeupExamRequests(username, studentId));
    }

    @PostMapping("/makeup-requests")
    public ApiResponse<Void> createMakeupExamRequest(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId,
        @Valid @RequestBody AcademicRequestCreateRequest request
    ) {
        studentService.createMakeupExamRequest(username, studentId, request);
        return ApiResponse.success("补考申请已提交", null);
    }

    @GetMapping("/retake-requests")
    public ApiResponse<List<Map<String, Object>>> retakeRequests(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.retakeRequests(username, studentId));
    }

    @PostMapping("/retake-requests")
    public ApiResponse<Void> createRetakeRequest(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId,
        @Valid @RequestBody AcademicRequestCreateRequest request
    ) {
        studentService.createRetakeRequest(username, studentId, request);
        return ApiResponse.success("重修申请已提交", null);
    }

    @GetMapping("/academic-progress")
    public ApiResponse<List<Map<String, Object>>> academicProgress(
        @RequestHeader(value = "X-Username", required = false) String username,
        @RequestHeader(value = "X-Student-Id", required = false) String studentId
    ) {
        return ApiResponse.success(studentService.academicProgress(username, studentId));
    }
}
