package com.school.teacher.controller;

import com.school.common.api.ApiResponse;
import com.school.common.dto.CourseDropReviewRequest;
import com.school.teacher.dto.ExamChangeRequestCreateRequest;
import com.school.teacher.dto.MakeupExamReviewRequest;
import com.school.teacher.dto.ScheduleAdjustRequestCreateRequest;
import com.school.teacher.dto.ScoreChangeRequestCreateRequest;
import com.school.teacher.dto.ScoreBatchSaveRequest;
import com.school.teacher.dto.ScoreInputRequest;
import com.school.teacher.dto.TeacherProfileUpdateRequest;
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
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.profile(teacherId));
    }

    @GetMapping("/todos")
    public ApiResponse<Map<String, Object>> todos(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.todoSummary(teacherId));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(
        @Valid @RequestBody TeacherProfileUpdateRequest request,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.updateProfile(teacherId, request);
        return ApiResponse.success("个人信息保存成功", null);
    }

    @GetMapping("/teaching-classes")
    public ApiResponse<List<Map<String, Object>>> teachingClasses(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId,
        @RequestParam(value = "includeAllSemesters", defaultValue = "false") boolean includeAllSemesters
    ) {
        return ApiResponse.success(teacherService.teachingClasses(teacherId, includeAllSemesters));
    }

    @GetMapping("/scores")
    public ApiResponse<List<Map<String, Object>>> scores(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.scores(teacherId));
    }

    @GetMapping("/scores/submit-window/{teachingClassId}")
    public ApiResponse<Map<String, Object>> scoreSubmitWindow(
        @PathVariable Integer teachingClassId,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.scoreSubmitWindow(teacherId, teachingClassId));
    }

    @PostMapping("/scores")
    public ApiResponse<Void> saveScore(
        @Valid @RequestBody ScoreInputRequest request,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.saveScore(request, teacherId);
        return ApiResponse.success("成绩保存成功", null);
    }

    @PostMapping("/scores/batch")
    public ApiResponse<Void> saveScores(
        @Valid @RequestBody ScoreBatchSaveRequest request,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.saveScores(request.getScores(), teacherId);
        return ApiResponse.success("成绩批量保存成功", null);
    }

    @GetMapping("/teaching-classes/{teachingClassId}")
    public ApiResponse<Map<String, Object>> teachingClassDetail(
        @PathVariable Integer teachingClassId,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.teachingClassDetail(teacherId, teachingClassId));
    }

    @GetMapping("/teaching-classes/{teachingClassId}/students")
    public ApiResponse<List<Map<String, Object>>> teachingClassStudents(
        @PathVariable Integer teachingClassId,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.teachingClassStudents(teacherId, teachingClassId));
    }

    @GetMapping("/course-drop-requests")
    public ApiResponse<List<Map<String, Object>>> courseDropRequests(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.courseDropRequests(teacherId));
    }

    @GetMapping("/makeup-requests")
    public ApiResponse<List<Map<String, Object>>> makeupExamRequests(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.makeupExamRequests(teacherId));
    }

    @GetMapping("/classroom-options")
    public ApiResponse<List<Map<String, Object>>> classroomOptions() {
        return ApiResponse.success(teacherService.classroomOptions());
    }

    @GetMapping("/current-semester")
    public ApiResponse<Map<String, Object>> currentSemester() {
        return ApiResponse.success(teacherService.currentSemester());
    }

    @GetMapping("/available-classroom-options")
    public ApiResponse<List<Map<String, Object>>> availableClassroomOptions(
        @RequestParam Integer teachingClassId,
        @RequestParam Integer weekDay,
        @RequestParam String sectionDesc,
        @RequestParam(required = false, defaultValue = "全周") String weekParity,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.availableClassroomOptions(teacherId, teachingClassId, weekDay, sectionDesc, weekParity));
    }

    @GetMapping("/teaching-class-sessions")
    public ApiResponse<List<Map<String, Object>>> teachingClassSessions(
        @RequestParam Integer teachingClassId,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.teachingClassSessions(teacherId, teachingClassId));
    }

    @GetMapping("/exam-available-classroom-options")
    public ApiResponse<List<Map<String, Object>>> examAvailableClassroomOptions(
        @RequestParam Long examId,
        @RequestParam String examDate,
        @RequestParam String startTime,
        @RequestParam String endTime,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.examAvailableClassroomOptions(
            teacherId,
            examId,
            examDate,
            startTime,
            endTime
        ));
    }

    @GetMapping("/schedule-adjust-requests")
    public ApiResponse<List<Map<String, Object>>> scheduleAdjustRequests(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.scheduleAdjustRequests(teacherId));
    }

    @PostMapping("/schedule-adjust-requests")
    public ApiResponse<Void> createScheduleAdjustRequest(
        @Valid @RequestBody ScheduleAdjustRequestCreateRequest request,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.createScheduleAdjustRequest(teacherId, request);
        return ApiResponse.success("调课申请提交成功", null);
    }

    @PutMapping("/schedule-adjust-requests/{adjustRequestId}/cancel")
    public ApiResponse<Void> cancelScheduleAdjustRequest(
        @PathVariable Long adjustRequestId,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.cancelScheduleAdjustRequest(teacherId, adjustRequestId);
        return ApiResponse.success("调课申请已撤销", null);
    }

    @GetMapping("/score-change-requests")
    public ApiResponse<List<Map<String, Object>>> scoreChangeRequests(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.scoreChangeRequests(teacherId));
    }

    @PostMapping("/score-change-requests")
    public ApiResponse<Void> createScoreChangeRequest(
        @Valid @RequestBody ScoreChangeRequestCreateRequest request,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.createScoreChangeRequest(teacherId, request);
        return ApiResponse.success("成绩修改申请提交成功", null);
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

    @PutMapping("/makeup-requests/{requestId}/review")
    public ApiResponse<Void> reviewMakeupExamRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody MakeupExamReviewRequest request,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.reviewMakeupExamRequest(requestId, request, teacherId);
        return ApiResponse.success("补考申请确认成功", null);
    }

    @GetMapping("/exams")
    public ApiResponse<List<Map<String, Object>>> exams(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.exams(teacherId));
    }

    @GetMapping("/exam-change-requests")
    public ApiResponse<List<Map<String, Object>>> examChangeRequests(
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        return ApiResponse.success(teacherService.examChangeRequests(teacherId));
    }

    @PostMapping("/exam-change-requests")
    public ApiResponse<Void> createExamChangeRequest(
        @Valid @RequestBody ExamChangeRequestCreateRequest request,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.createExamChangeRequest(teacherId, request);
        return ApiResponse.success("考试修改申请提交成功", null);
    }

    @PutMapping("/exam-change-requests/{examChangeRequestId}/cancel")
    public ApiResponse<Void> cancelExamChangeRequest(
        @PathVariable Long examChangeRequestId,
        @RequestHeader(value = "X-Teacher-Id", defaultValue = "T0101") String teacherId
    ) {
        teacherService.cancelExamChangeRequest(teacherId, examChangeRequestId);
        return ApiResponse.success("考试修改申请已撤销", null);
    }
}
