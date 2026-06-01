package com.school.student.controller;

import com.school.common.api.ApiResponse;
import com.school.student.dto.CourseDropRequestCreateRequest;
import com.school.student.dto.CourseSelectionRequestCreateRequest;
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
    public ApiResponse<List<Map<String, Object>>> availableCourses(StudentCourseQueryRequest query) {
        return ApiResponse.success(studentService.availableCourses(query));
    }

    @GetMapping("/selected-courses")
    public ApiResponse<List<Map<String, Object>>> selectedCourses(
        @RequestHeader(value = "X-Student-Id", defaultValue = "20240001") String studentId
    ) {
        return ApiResponse.success(studentService.selectedCourses(studentId));
    }

    @PostMapping("/course-selections")
    public ApiResponse<Void> selectCourse(@Valid @RequestBody SelectCourseRequest request) {
        studentService.selectCourse(request);
        return ApiResponse.success("选课成功", null);
    }

    @DeleteMapping("/course-selections/{teachingClassId}")
    public ApiResponse<Void> dropCourse(
        @PathVariable Integer teachingClassId,
        @RequestHeader(value = "X-Student-Id", defaultValue = "20240001") String studentId
    ) {
        studentService.dropCourse(studentId, teachingClassId);
        return ApiResponse.success("退课成功", null);
    }

    @GetMapping("/timetable")
    public ApiResponse<List<Map<String, Object>>> timetable(
        @RequestHeader(value = "X-Student-Id", defaultValue = "20240001") String studentId
    ) {
        return ApiResponse.success(studentService.timetable(studentId));
    }

    @GetMapping("/scores")
    public ApiResponse<List<Map<String, Object>>> scores(
        @RequestHeader(value = "X-Student-Id", defaultValue = "20240001") String studentId
    ) {
        return ApiResponse.success(studentService.scores(studentId));
    }

    @PostMapping("/course-selection-requests")
    public ApiResponse<Void> createCourseSelectionRequest(@Valid @RequestBody CourseSelectionRequestCreateRequest request) {
        studentService.createCourseSelectionRequest(request);
        return ApiResponse.success("补选申请已提交", null);
    }

    @GetMapping("/course-selection-requests")
    public ApiResponse<List<Map<String, Object>>> courseSelectionRequests(
        @RequestHeader(value = "X-Student-Id", defaultValue = "20240001") String studentId
    ) {
        return ApiResponse.success(studentService.courseSelectionRequests(studentId));
    }

    @GetMapping("/drop-courses")
    public ApiResponse<List<Map<String, Object>>> dropCourses(
        @RequestHeader(value = "X-Student-Id", defaultValue = "20240001") String studentId
    ) {
        return ApiResponse.success(studentService.dropCourses(studentId));
    }

    @PostMapping("/drop-requests")
    public ApiResponse<Void> createCourseDropRequest(
        @RequestHeader(value = "X-Student-Id", defaultValue = "20240001") String studentId,
        @Valid @RequestBody CourseDropRequestCreateRequest request
    ) {
        studentService.createCourseDropRequest(studentId, request);
        return ApiResponse.success("退课申请已提交", null);
    }

    @GetMapping("/drop-requests")
    public ApiResponse<List<Map<String, Object>>> courseDropRequests(
        @RequestHeader(value = "X-Student-Id", defaultValue = "20240001") String studentId
    ) {
        return ApiResponse.success(studentService.courseDropRequests(studentId));
    }
}
