package com.school.student.controller;

import com.school.common.api.ApiResponse;
import com.school.student.dto.SelectCourseRequest;
import com.school.student.service.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ApiResponse<List<Map<String, Object>>> availableCourses() {
        return ApiResponse.success(studentService.availableCourses());
    }

    @PostMapping("/course-selections")
    public ApiResponse<Void> selectCourse(@Valid @RequestBody SelectCourseRequest request) {
        studentService.selectCourse(request);
        return ApiResponse.success("选课成功", null);
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
}
