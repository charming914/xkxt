package com.school.admin.controller;

import com.school.admin.dto.AdminCourseRequest;
import com.school.admin.dto.AdminStudentRequest;
import com.school.admin.dto.AdminTeacherRequest;
import com.school.admin.dto.CourseSelectionSettingRequest;
import com.school.admin.service.AdminService;
import com.school.common.api.ApiResponse;
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

    @GetMapping("/course-selection-setting")
    public ApiResponse<Map<String, Object>> currentSemesterSetting() {
        return ApiResponse.success(adminService.currentSemesterSetting());
    }

    @PutMapping("/course-selection-setting")
    public ApiResponse<Void> updateCourseSelectionSetting(@Valid @RequestBody CourseSelectionSettingRequest request) {
        adminService.updateCourseSelectionSetting(request);
        return ApiResponse.success("选课设置更新成功", null);
    }
}
