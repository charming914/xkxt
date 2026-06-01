package com.school.teacher.controller;

import com.school.common.api.ApiResponse;
import com.school.common.dto.CourseDropReviewRequest;
import com.school.teacher.dto.ScoreInputRequest;
import com.school.teacher.service.TeacherService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/teaching-classes")
    public ApiResponse<List<Map<String, Object>>> teachingClasses(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.teachingClasses(teacherId));
    }

    @GetMapping("/scores")
    public ApiResponse<List<Map<String, Object>>> scores(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.scores(teacherId));
    }

    @PostMapping("/scores")
    public ApiResponse<Void> saveScore(@Valid @RequestBody ScoreInputRequest request) {
        teacherService.saveScore(request);
        return ApiResponse.success("成绩保存成功", null);
    }

    @GetMapping("/course-drop-requests")
    public ApiResponse<List<Map<String, Object>>> courseDropRequests(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.courseDropRequests(teacherId));
    }

    @PutMapping("/course-drop-requests/{dropRequestId}/review")
    public ApiResponse<Void> reviewCourseDropRequest(
        @PathVariable Long dropRequestId,
        @Valid @RequestBody CourseDropReviewRequest request,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.reviewCourseDropRequest(dropRequestId, request, teacherId);
        return ApiResponse.success("退课申请审核成功", null);
    }
}
