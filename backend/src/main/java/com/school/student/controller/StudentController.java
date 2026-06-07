package com.school.student.controller;

import com.school.common.api.ApiResponse;
import com.school.student.dto.CourseDropRequestCreateRequest;
import com.school.student.dto.CourseSelectionRequestCreateRequest;
import com.school.student.dto.ScoreReviewRequestCreateRequest;
import com.school.student.dto.SelectCourseRequest;
import com.school.student.dto.StudentCourseQueryRequest;
import com.school.student.service.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/courses/available")
    public ApiResponse<List<Map<String, Object>>> availableCourses(
        @RequestHeader(value = "X-Username", required = false) String username,
        StudentCourseQueryRequest query
    ) {
        return ApiResponse.success(studentService.availableCourses(username, query));
    }

    @GetMapping("/course-selections")
    public ApiResponse<List<Map<String, Object>>> selectedCourses(
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        return ApiResponse.success(studentService.selectedCourses(username));
    }

    @GetMapping("/selected-courses")
    public ApiResponse<List<Map<String, Object>>> selectedCoursesAlias(
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        return ApiResponse.success(studentService.selectedCourses(username));
    }

    @PostMapping("/course-selections")
    public ApiResponse<Void> selectCourse(
        @RequestHeader(value = "X-Username", required = false) String username,
        @Valid @RequestBody SelectCourseRequest request
    ) {
        studentService.selectCourse(username, request);
        return ApiResponse.success("选课成功", null);
    }

    @DeleteMapping("/course-selections/{teachingClassId}")
    public ApiResponse<Void> dropCourse(
        @PathVariable Integer teachingClassId,
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        studentService.dropCourse(username, teachingClassId);
        return ApiResponse.success("退课成功", null);
    }

    @GetMapping("/timetable")
    public ApiResponse<List<Map<String, Object>>> timetable(
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        return ApiResponse.success(studentService.timetable(username));
    }

    @GetMapping("/scores")
    public ApiResponse<List<Map<String, Object>>> scores(
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        return ApiResponse.success(studentService.scores(username));
    }

    @PostMapping("/course-selection-requests")
    public ApiResponse<Void> createCourseSelectionRequest(
        @RequestHeader(value = "X-Username", required = false) String username,
        @Valid @RequestBody CourseSelectionRequestCreateRequest request
    ) {
        studentService.createCourseSelectionRequest(username, request);
        return ApiResponse.success("补选申请已提交", null);
    }

    @GetMapping("/course-selection-requests")
    public ApiResponse<List<Map<String, Object>>> courseSelectionRequests(
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        return ApiResponse.success(studentService.courseSelectionRequests(username));
    }

    @GetMapping("/drop-courses")
    public ApiResponse<List<Map<String, Object>>> dropCourses(
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        return ApiResponse.success(studentService.dropCourses(username));
    }

    @PostMapping("/course-drop-requests")
    public ApiResponse<Void> createCourseDropRequest(
        @RequestHeader(value = "X-Username", required = false) String username,
        @Valid @RequestBody CourseDropRequestCreateRequest request
    ) {
        studentService.createCourseDropRequest(username, request);
        return ApiResponse.success("退课申请已提交", null);
    }

    @PostMapping("/drop-requests")
    public ApiResponse<Void> createCourseDropRequestAlias(
        @RequestHeader(value = "X-Username", required = false) String username,
        @Valid @RequestBody CourseDropRequestCreateRequest request
    ) {
        studentService.createCourseDropRequest(username, request);
        return ApiResponse.success("退课申请已提交", null);
    }

    @GetMapping("/course-drop-requests")
    public ApiResponse<List<Map<String, Object>>> courseDropRequests(
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        return ApiResponse.success(studentService.courseDropRequests(username));
    }

    @GetMapping("/drop-requests")
    public ApiResponse<List<Map<String, Object>>> courseDropRequestsAlias(
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        return ApiResponse.success(studentService.courseDropRequests(username));
    }

    @PostMapping("/score-review-requests")
    public ApiResponse<Void> createScoreReviewRequest(
        @RequestHeader(value = "X-Username", required = false) String username,
        @Valid @RequestBody ScoreReviewRequestCreateRequest request
    ) {
        studentService.createScoreReviewRequest(username, request);
        return ApiResponse.success("成绩复核申请已提交", null);
    }

    @GetMapping("/score-review-requests")
    public ApiResponse<List<Map<String, Object>>> scoreReviewRequests(
        @RequestHeader(value = "X-Username", required = false) String username
    ) {
        return ApiResponse.success(studentService.scoreReviewRequests(username));
    }
}
