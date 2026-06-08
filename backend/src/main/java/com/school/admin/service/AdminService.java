package com.school.admin.service;

import com.school.admin.dto.AdminCourseRequest;
import com.school.admin.dto.AcademicRequestReviewRequest;
import com.school.admin.dto.AdminStudentRequest;
import com.school.admin.dto.AdminTeacherRequest;
import com.school.admin.dto.AdminTeachingClassRequest;
import com.school.admin.dto.CourseSelectionSettingRequest;
import com.school.admin.dto.ExamUpdateRequest;
import com.school.admin.dto.MakeupExamArrangeRequest;
import com.school.admin.dto.ScoreReviewRequestReviewRequest;
import com.school.admin.dto.ScoreSubmitSettingRequest;
import com.school.admin.dto.SemesterRequest;
import static com.school.common.support.CourseDropReviewSupport.resolveDropCourseMessage;
import static com.school.common.support.CourseDropReviewSupport.resolveReviewStatus;

import com.school.common.dto.CourseDropReviewRequest;
import com.school.admin.dto.CourseSelectionRequestReviewRequest;
import com.school.admin.mapper.AdminMapper;
import com.school.common.exception.BusinessException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    public List<Map<String, Object>> teachingClasses() {
        return adminMapper.selectTeachingClasses();
    }

    public List<Map<String, Object>> teachingClassStudents(Integer teachingClassId) {
        return adminMapper.selectTeachingClassStudents(teachingClassId);
    }

    public List<Map<String, Object>> courseSelectionStatistics() {
        return adminMapper.selectCourseSelectionStatistics();
    }

    public Map<String, Object> dashboardOverview() {
        return adminMapper.selectDashboardOverview();
    }

    public List<Map<String, Object>> scoredTeachingClasses() {
        return adminMapper.selectScoredTeachingClasses();
    }

    public Map<String, Object> scoreDistribution(Integer teachingClassId) {
        Map<String, Object> distribution = adminMapper.selectScoreDistribution(teachingClassId);
        if (distribution == null || distribution.get("totalCount") == null
            || ((Number) distribution.get("totalCount")).intValue() == 0) {
            throw new BusinessException("该教学班暂无已提交成绩");
        }
        return distribution;
    }

    public List<Map<String, Object>> cultivationPlanRequirements() {
        return adminMapper.selectCultivationPlanRequirements();
    }

    @Transactional(rollbackFor = Exception.class)
    public int importCultivationPlan(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择培养方案 CSV 文件");
        }
        int imported = 0;
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                String normalized = stripBom(line).trim();
                if (normalized.isBlank()) {
                    continue;
                }
                String[] parts = normalized.split(",", -1);
                if (firstLine && isCultivationPlanHeader(parts)) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;
                if (parts.length < 4) {
                    throw new BusinessException("培养方案 CSV 至少需要4列：入学年份,专业编号,课程类型,要求学分");
                }
                Integer enrollmentYear = Integer.valueOf(parts[0].trim());
                String majorId = parts[1].trim();
                String courseType = parts[2].trim();
                BigDecimal requiredCredit = new BigDecimal(parts[3].trim());
                String remark = parts.length > 4 ? parts[4].trim() : "";
                if (majorId.isBlank() || courseType.isBlank()) {
                    throw new BusinessException("专业编号和课程类型不能为空");
                }
                adminMapper.upsertCultivationPlanRequirement(
                    enrollmentYear,
                    majorId,
                    courseType,
                    requiredCredit,
                    remark
                );
                imported++;
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("培养方案 CSV 中入学年份或要求学分格式不正确");
        } catch (IOException e) {
            throw new BusinessException("培养方案 CSV 读取失败");
        }
        return imported;
    }

    public List<Map<String, Object>> makeupExamRequests() {
        return adminMapper.selectMakeupExamRequests();
    }

    public List<Map<String, Object>> retakeRequests() {
        return adminMapper.selectRetakeRequests();
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewMakeupExamRequest(
        Long requestId,
        MakeupExamArrangeRequest request,
        String adminUsername
    ) {
        Map<String, Object> makeupRequest = adminMapper.selectMakeupExamRequestById(requestId);
        if (makeupRequest == null) {
            throw new BusinessException("补考申请不存在");
        }
        if (!"待管理员排教室".equals(makeupRequest.get("requestStatus"))) {
            throw new BusinessException("该补考申请尚未进入管理员排教室环节");
        }
        if (!"已确认".equals(makeupRequest.get("teacherStatus"))) {
            throw new BusinessException("任课教师确认后才能安排补考教室");
        }
        Integer adminUserId = resolveAdminUserId(adminUsername);

        String action = request.getAction();
        if ("APPROVE".equalsIgnoreCase(action)) {
            if (request.getClassroomId() == null || request.getClassroomId().isBlank()) {
                throw new BusinessException("请选择补考教室");
            }
            LocalDateTime confirmedExamTime = parseDateTimeValue(
                makeupRequest.get("confirmedExamTime"),
                "教师确认补考时间"
            );
            LocalDateTime endTime = confirmedExamTime.plusHours(2);
            String examDate = confirmedExamTime.toLocalDate().toString();
            String startClock = formatTime(confirmedExamTime);
            String endClock = formatTime(endTime);
            String classroomId = request.getClassroomId();
            if (adminMapper.countExamRoomConflict(0L, examDate, startClock, endClock, classroomId) > 0
                || adminMapper.countMakeupExamRoomConflict(
                    requestId,
                    formatDateTime(confirmedExamTime),
                    formatDateTime(endTime),
                    classroomId
                ) > 0) {
                throw new BusinessException("安排失败：目标教室在该时间段已被占用");
            }
            adminMapper.updateMakeupExamArrangement(
                requestId,
                "已安排",
                "已安排",
                request.getReviewComment(),
                classroomId,
                "待考试",
                adminUserId
            );
            adminMapper.insertOperationLog(
                adminUserId,
                "补考教室安排",
                "管理员安排补考教室，申请编号：" + requestId + "，教室：" + classroomId
            );
            return;
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            if (request.getReviewComment() == null || request.getReviewComment().isBlank()) {
                throw new BusinessException("驳回补考教室申请需要填写理由");
            }
            adminMapper.updateMakeupExamArrangement(
                requestId,
                "已驳回",
                "已驳回",
                request.getReviewComment(),
                null,
                "未安排",
                adminUserId
            );
            adminMapper.insertOperationLog(
                adminUserId,
                "补考教室安排",
                "管理员驳回补考教室申请，申请编号：" + requestId
            );
            return;
        }

        throw new BusinessException("不支持的处理动作");
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMakeupExamResult(Long requestId, AcademicRequestReviewRequest request) {
        Map<String, Object> makeupRequest = adminMapper.selectMakeupExamRequestById(requestId);
        if (makeupRequest == null) {
            throw new BusinessException("补考申请不存在");
        }
        if (!"已安排".equals(makeupRequest.get("requestStatus"))
            && !"已通过".equals(makeupRequest.get("requestStatus"))) {
            throw new BusinessException("只有已安排的补考申请可以登记结果");
        }
        String resultStatus = normalizeMakeupResult(request.getResultStatus(), null);
        if (adminMapper.updateMakeupExamResult(requestId, resultStatus) == 0) {
            throw new BusinessException("补考结果登记失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewRetakeRequest(
        Long requestId,
        AcademicRequestReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> retakeRequest = adminMapper.selectRetakeRequestById(requestId);
        if (retakeRequest == null) {
            throw new BusinessException("重修申请不存在");
        }
        if (!"待审核".equals(retakeRequest.get("requestStatus"))) {
            throw new BusinessException("该重修申请已处理，请勿重复审核");
        }
        Integer adminUserId = resolveAdminUserId(adminUsername);
        String reviewStatus = resolveReviewStatus(request.getAction());
        adminMapper.updateRetakeReview(requestId, reviewStatus, request.getReviewComment(), adminUserId);
        adminMapper.insertOperationLog(
            adminUserId,
            "重修审核",
            "管理员处理重修申请，申请编号：" + requestId + "，结果：" + reviewStatus
        );
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

    public List<Map<String, Object>> semesters() {
        return adminMapper.selectSemesterOptions();
    }

    public List<Map<String, Object>> allSemesters() {
        return adminMapper.selectAllSemesters();
    }

    /** 教学周数：固定 16 周 */
    private static final int TEACHING_WEEKS = 16;
    /** 考试周数：固定 2 周，紧跟教学周之后 */
    private static final int EXAM_WEEKS = 2;

    /**
     * 由学期开始日期推导其余全部字段，写回 request。
     * 规则：16 教学周 + 2 考试周；考试周紧跟学期结束日之后，再之后为假期。
     * 学年/学期代码按开始月份判定：7 月及以后为第一学期（秋季），否则为第二学期（春季）。
     */
    private void deriveSemesterFields(SemesterRequest request) {
        LocalDate start;
        try {
            start = LocalDate.parse(request.getStartDate().substring(0, 10));
        } catch (RuntimeException ex) {
            throw new BusinessException("学期开始日期格式不正确");
        }

        // 教学结束日 = 开始日 + 16 周 - 1 天（第 16 个教学周的最后一天）
        LocalDate endDate = start.plusWeeks(TEACHING_WEEKS).minusDays(1);

        // 学年与学期代码：7 月及以后视为秋季学期（学期代码 1），否则为春季学期（学期代码 2）
        int year = start.getYear();
        String termCode;
        String schoolYear;
        if (start.getMonthValue() >= 7) {
            termCode = "1";
            schoolYear = year + "-" + (year + 1);
        } else {
            termCode = "2";
            schoolYear = (year - 1) + "-" + year;
        }
        String semesterName = schoolYear + "-" + termCode;

        // 选课时间：设在「上学期考试周」之前的两周（即上学期最后两个教学周内完成下学期选课）
        LocalDateTime selectStart;
        LocalDateTime selectEnd;
        String prevEndStr = adminMapper.selectPreviousSemesterEndDate(start.toString(), request.getSemesterId());
        if (prevEndStr != null) {
            LocalDate prevEnd = LocalDate.parse(prevEndStr.substring(0, 10));
            LocalDate prevExamStart = prevEnd.plusDays(1); // 上学期考试周第一天
            selectStart = prevExamStart.minusWeeks(EXAM_WEEKS).atTime(8, 0, 0);
            selectEnd = prevExamStart.minusDays(1).atTime(23, 59, 59);
        } else {
            // 没有上学期（首个学期）时退回：开学前 2 周开放，至第 2 教学周结束
            selectStart = start.minusWeeks(2).atTime(8, 0, 0);
            selectEnd = start.plusWeeks(2).minusDays(1).atTime(23, 59, 59);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        request.setStartDate(start.toString());
        request.setEndDate(endDate.toString());
        request.setSemesterName(semesterName);
        request.setSchoolYear(schoolYear);
        request.setTermCode(termCode);
        request.setCourseSelectStart(selectStart.format(dtf));
        request.setCourseSelectEnd(selectEnd.format(dtf));
    }

    @Transactional
    public void createSemester(SemesterRequest request) {
        deriveSemesterFields(request);
        if (adminMapper.countSemesterByName(request.getSemesterName(), null) > 0) {
            throw new BusinessException("已存在学期「" + request.getSemesterName() + "」，请检查开始日期");
        }
        Integer carryOverFrom = request.getCarryOverFromSemesterId();
        if (carryOverFrom != null && adminMapper.countSemesterById(carryOverFrom) == 0) {
            throw new BusinessException("结转来源学期不存在");
        }
        adminMapper.insertSemester(request);
        Integer newSemesterId = request.getSemesterId();
        if (carryOverFrom != null) {
            adminMapper.carryOverTeachingClasses(carryOverFrom, newSemesterId);
        }
        if (Boolean.TRUE.equals(request.getSetAsCurrent())) {
            adminMapper.clearCurrentSemester();
            adminMapper.markCurrentSemester(newSemesterId);
        }
    }

    @Transactional
    public void updateSemester(Integer semesterId, SemesterRequest request) {
        if (adminMapper.countSemesterById(semesterId) == 0) {
            throw new BusinessException("学期不存在");
        }
        request.setSemesterId(semesterId);
        deriveSemesterFields(request);
        if (adminMapper.countSemesterByName(request.getSemesterName(), semesterId) > 0) {
            throw new BusinessException("已存在学期「" + request.getSemesterName() + "」，请检查开始日期");
        }
        adminMapper.updateSemester(request);
    }

    @Transactional
    public void deleteSemester(Integer semesterId) {
        if (adminMapper.countSemesterById(semesterId) == 0) {
            throw new BusinessException("学期不存在");
        }
        if (adminMapper.isCurrentSemester(semesterId) > 0) {
            throw new BusinessException("当前学期不可删除，请先切换到其他学期");
        }
        if (adminMapper.countTeachingClassesBySemester(semesterId) > 0) {
            throw new BusinessException("该学期已存在教学班，不可删除");
        }
        adminMapper.deleteSemester(semesterId);
    }

    @Transactional
    public void setCurrentSemester(Integer semesterId) {
        if (adminMapper.countSemesterById(semesterId) == 0) {
            throw new BusinessException("学期不存在");
        }
        adminMapper.clearCurrentSemester();
        adminMapper.markCurrentSemester(semesterId);
    }

    public List<Map<String, Object>> teacherOptions() {
        return adminMapper.selectTeacherOptions();
    }

    public List<Map<String, Object>> classroomOptions() {
        return adminMapper.selectClassroomOptions();
    }

    public List<Map<String, Object>> courseOptions() {
        return adminMapper.selectCourseOptions();
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
        validateScoreRatio(request);
        adminMapper.insertCourse(request);
    }

    public void updateCourse(String courseId, AdminCourseRequest request) {
        validateScoreRatio(request);
        adminMapper.updateCourse(courseId, request);
    }

    private void validateScoreRatio(AdminCourseRequest request) {
        if (request.getUsualRatio() + request.getExamRatio() != 100) {
            throw new BusinessException("平时成绩占比与考试成绩占比之和必须为100");
        }
    }

    public void deleteCourse(String courseId) {
        adminMapper.deleteCourse(courseId);
    }

    public void createTeachingClass(AdminTeachingClassRequest request) {
        adminMapper.insertTeachingClass(request);
    }

    public void updateTeachingClass(Integer teachingClassId, AdminTeachingClassRequest request) {
        adminMapper.updateTeachingClass(teachingClassId, request);
    }

    public void deleteTeachingClass(Integer teachingClassId) {
        if (adminMapper.countSelectedByTeachingClassId(teachingClassId) > 0) {
            throw new BusinessException("该教学班已有学生选课，不能删除");
        }
        adminMapper.deleteTeachingClass(teachingClassId);
    }

    public Map<String, Object> currentSemesterSetting() {
        Map<String, Object> setting = adminMapper.selectCurrentSemesterSetting();
        if (setting == null) {
            throw new BusinessException("当前学期不存在");
        }
        return setting;
    }

    @Transactional
    public void updateCourseSelectionSetting(CourseSelectionSettingRequest request) {
        Map<String, Object> setting = adminMapper.selectCurrentSemesterSetting();
        if (setting == null) {
            throw new BusinessException("当前学期不存在");
        }
        validateMakeupRequestWindow(request);
        Integer semesterId = ((Number) setting.get("semesterId")).intValue();
        adminMapper.updateCourseSelectionTime(semesterId, request);
    }

    public Map<String, Object> currentScoreSubmitSetting() {
        Map<String, Object> setting = adminMapper.selectCurrentScoreSubmitSetting();
        if (setting == null) {
            throw new BusinessException("当前学期不存在");
        }
        return setting;
    }

    @Transactional
    public void updateScoreSubmitSetting(ScoreSubmitSettingRequest request) {
        Map<String, Object> setting = adminMapper.selectCurrentScoreSubmitSetting();
        if (setting == null) {
            throw new BusinessException("当前学期不存在");
        }
        Integer semesterId = ((Number) setting.get("semesterId")).intValue();
        adminMapper.updateScoreSubmitTime(semesterId, request);
    }

    public List<Map<String, Object>> submittedScores() {
        return adminMapper.selectSubmittedScores();
    }

    public List<Map<String, Object>> scoreChangeRequests() {
        return adminMapper.selectScoreChangeRequests();
    }

    public List<Map<String, Object>> scoreReviewRequests() {
        return adminMapper.selectScoreReviewRequests();
    }

    @Transactional
    public void rollbackSubmittedScore(Long scoreId) {
        if (adminMapper.rollbackSubmittedScore(scoreId) == 0) {
            throw new BusinessException("成绩不存在或已处于可修改状态");
        }
    }

    public List<Map<String, Object>> courseSelectionRequests() {
        return adminMapper.selectCourseSelectionRequests();
    }

    public List<Map<String, Object>> courseDropRequests() {
        return adminMapper.selectCourseDropRequests();
    }

    public List<Map<String, Object>> scheduleAdjustRequests() {
        return adminMapper.selectScheduleAdjustRequests();
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewScoreChangeRequest(
        Long scoreChangeRequestId,
        CourseSelectionRequestReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> scoreChangeRequest = adminMapper.selectScoreChangeRequestById(scoreChangeRequestId);
        if (scoreChangeRequest == null) {
            throw new BusinessException("成绩修改申请不存在");
        }
        if (!"待审核".equals(scoreChangeRequest.get("requestStatus"))) {
            throw new BusinessException("该成绩修改申请已处理，请勿重复审核");
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String action = request.getAction();
        if ("APPROVE".equalsIgnoreCase(action)) {
            Long scoreId = ((Number) scoreChangeRequest.get("scoreId")).longValue();
            adminMapper.rollbackSubmittedScore(scoreId);
            adminMapper.updateScoreChangeRequestStatus(scoreChangeRequestId, "已通过", request.getReviewComment(), adminUserId);
            return;
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            adminMapper.updateScoreChangeRequestStatus(scoreChangeRequestId, "已驳回", request.getReviewComment(), adminUserId);
            return;
        }

        throw new BusinessException("不支持的审核动作");
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewScoreReviewRequest(
        Long requestId,
        ScoreReviewRequestReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> scoreReviewRequest = adminMapper.selectScoreReviewRequestById(requestId);
        if (scoreReviewRequest == null) {
            throw new BusinessException("成绩复核申请不存在");
        }
        if (!"待审核".equals(scoreReviewRequest.get("requestStatus"))) {
            throw new BusinessException("该成绩复核申请已处理，请勿重复审核");
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String reviewStatus = resolveReviewStatus(request.getAction());
        adminMapper.updateScoreReviewRequestStatus(
            requestId,
            reviewStatus,
            request.getReviewComment(),
            adminUserId
        );
        adminMapper.insertOperationLog(
            adminUserId,
            "成绩复核审核",
            "管理员处理成绩复核申请，申请编号：" + requestId + "，结果：" + reviewStatus
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewScheduleAdjustRequest(
        Long adjustRequestId,
        CourseSelectionRequestReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> adjustRequest = adminMapper.selectScheduleAdjustRequestById(adjustRequestId);
        if (adjustRequest == null) {
            throw new BusinessException("调课申请不存在");
        }
        if (!"待审核".equals(adjustRequest.get("requestStatus"))) {
            throw new BusinessException("该调课申请已处理，请勿重复审核");
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String action = request.getAction();
        if ("APPROVE".equalsIgnoreCase(action)) {
            Integer teachingClassId = ((Number) adjustRequest.get("teachingClassId")).intValue();
            Integer weekDay = ((Number) adjustRequest.get("requestedWeekDay")).intValue();
            String sectionDesc = (String) adjustRequest.get("requestedSectionDesc");
            String weekRange = (String) adjustRequest.get("requestedWeekRange");
            String classroomId = (String) adjustRequest.get("requestedClassroomId");
            if (adminMapper.countTeachingClassRoomConflict(teachingClassId, weekDay, sectionDesc, classroomId) > 0) {
                throw new BusinessException("审核失败：目标教室在该时间段已被占用");
            }
            if (adminMapper.countTeachingClassTeacherConflict(teachingClassId, weekDay, sectionDesc) > 0) {
                throw new BusinessException("审核失败：申请时间冲突");
            }
            adminMapper.applyScheduleAdjustRequest(teachingClassId, weekDay, sectionDesc, weekRange, classroomId);
            adminMapper.updateScheduleAdjustRequestStatus(adjustRequestId, "已通过", request.getReviewComment(), adminUserId);
            return;
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            adminMapper.updateScheduleAdjustRequestStatus(adjustRequestId, "已驳回", request.getReviewComment(), adminUserId);
            return;
        }

        throw new BusinessException("不支持的审核动作");
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
            if (adminMapper.countDroppedCourse(studentId, teachingClassId) > 0) {
                adminMapper.deleteScore(studentId, teachingClassId);
                adminMapper.restoreDroppedCourseByAdmin(studentId, teachingClassId);
            } else {
                adminMapper.insertCourseSelectionByAdmin(studentId, teachingClassId);
            }
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

    @Transactional(rollbackFor = Exception.class)
    public void reviewCourseDropRequest(
        Long dropRequestId,
        CourseDropReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> dropRequest = adminMapper.selectCourseDropRequestById(dropRequestId);
        if (dropRequest == null) {
            throw new BusinessException("退课申请不存在");
        }
        if (!"待审核".equals(dropRequest.get("requestStatus"))) {
            throw new BusinessException("该退课申请已处理，请勿重复审核");
        }
        if (!"待审核".equals(dropRequest.get("adminStatus"))) {
            throw new BusinessException("你已审核过该退课申请");
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String adminStatus = resolveReviewStatus(request.getAction());
        adminMapper.updateAdminDropReview(
            dropRequestId,
            adminStatus,
            request.getReviewComment(),
            adminUserId
        );
        dropCourseIfFullyApproved(dropRequestId, dropRequest);
    }

    private void dropCourseIfFullyApproved(Long dropRequestId, Map<String, Object> dropRequest) {
        String requestStatus = adminMapper.selectDropRequestStatus(dropRequestId);
        if (!"已通过".equals(requestStatus)) {
            return;
        }
        String studentId = (String) dropRequest.get("studentId");
        Integer teachingClassId = ((Number) dropRequest.get("teachingClassId")).intValue();
        try {
            adminMapper.dropSelectedCourse(studentId, teachingClassId);
            adminMapper.deleteScore(studentId, teachingClassId);
        } catch (RuntimeException e) {
            throw new BusinessException(resolveDropCourseMessage(e.getMessage()));
        }
    }

    private Integer resolveAdminUserId(String adminUsername) {
        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }
        return adminUserId;
    }

    private String stripBom(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "");
    }

    private boolean isCultivationPlanHeader(String[] parts) {
        if (parts.length == 0) {
            return false;
        }
        String first = stripBom(parts[0]).toLowerCase();
        return first.contains("enrollment") || first.contains("入学");
    }

    private String normalizeMakeupResult(String resultStatus, String fallback) {
        String value = resultStatus == null || resultStatus.isBlank() ? fallback : resultStatus.trim();
        if (value == null || value.isBlank()) {
            throw new BusinessException("补考结果不能为空");
        }
        if (!List.of("待考试", "已通过", "未通过", "未安排").contains(value)) {
            throw new BusinessException("补考结果只能是待考试、已通过、未通过或未安排");
        }
        return value;
    }

    private void validateMakeupRequestWindow(CourseSelectionSettingRequest request) {
        boolean hasStart = request.getMakeupRequestStart() != null && !request.getMakeupRequestStart().isBlank();
        boolean hasEnd = request.getMakeupRequestEnd() != null && !request.getMakeupRequestEnd().isBlank();
        if (!hasStart && !hasEnd) {
            return;
        }
        if (!hasStart || !hasEnd) {
            throw new BusinessException("补考申请开始和结束时间必须同时填写");
        }
        LocalDateTime start = parseDateTime(request.getMakeupRequestStart(), "补考申请开始时间");
        LocalDateTime end = parseDateTime(request.getMakeupRequestEnd(), "补考申请结束时间");
        if (!end.isAfter(start)) {
            throw new BusinessException("补考申请结束时间必须晚于开始时间");
        }
        if (end.isAfter(start.plusDays(7))) {
            throw new BusinessException("补考申请开放时间不能超过一周");
        }
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        try {
            return LocalDateTime.parse(value.trim().replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            throw new BusinessException(fieldName + "格式不正确");
        }
    }

    private LocalDateTime parseDateTimeValue(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessException(fieldName + "未设置");
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return parseDateTime(value.toString(), fieldName);
    }

    private String formatDateTime(LocalDateTime value) {
        return value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatTime(LocalDateTime value) {
        return value.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
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

    // ==================== 考试管理 ====================

    public List<Map<String, Object>> exams() {
        return adminMapper.selectExams();
    }

    /**
     * 为当前学期所有尚未排考的教学班自动生成考试安排。
     * 考试周为学期结束日次日起的两周内工作日，每天两场（09:00-11:00、14:00-16:00），
     * 复用教学班原上课教室，并贪心避开同教室同场次冲突。
     */
    @Transactional(rollbackFor = Exception.class)
    public int generateExams(String adminUsername) {
        Map<String, Object> semester = adminMapper.selectCurrentSemesterEndDate();
        if (semester == null || semester.get("endDate") == null) {
            throw new BusinessException("未设置当前学期或学期结束日期，无法生成考试安排");
        }
        LocalDate endDate = LocalDate.parse(semester.get("endDate").toString().substring(0, 10));

        List<Map<String, Object>> pending = adminMapper.selectTeachingClassesWithoutExam();
        if (pending.isEmpty()) {
            throw new BusinessException("当前学期没有需要排考的教学班（可能均已排定）");
        }

        // 考试周：学期结束日次日起两周内的工作日
        List<LocalDate> examDays = new java.util.ArrayList<>();
        LocalDate cursor = endDate.plusDays(1);
        LocalDate limit = endDate.plusDays(14);
        while (!cursor.isAfter(limit)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                examDays.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        if (examDays.isEmpty()) {
            throw new BusinessException("考试周内没有可用工作日，无法生成考试安排");
        }

        String[][] sessions = {{"09:00:00", "11:00:00"}, {"14:00:00", "16:00:00"}};
        // 记录已占用的 (日期|场次|教室) 以避免同教室同场次冲突
        java.util.Set<String> occupied = new java.util.HashSet<>();
        int created = 0;
        int idx = 0;
        for (Map<String, Object> tc : pending) {
            Integer teachingClassId = ((Number) tc.get("teachingClassId")).intValue();
            String classroomId = (String) tc.get("classroomId");

            boolean placed = false;
            // 从当前轮转位置开始为该教学班寻找不冲突的时段
            for (int attempt = 0; attempt < examDays.size() * sessions.length && !placed; attempt++) {
                int slot = (idx + attempt);
                LocalDate day = examDays.get((slot / sessions.length) % examDays.size());
                String[] session = sessions[slot % sessions.length];
                String key = day + "|" + session[0] + "|" + (classroomId == null ? "" : classroomId);
                if (classroomId != null && occupied.contains(key)) {
                    continue;
                }
                adminMapper.insertExam(teachingClassId, day.toString(), session[0], session[1], classroomId);
                if (classroomId != null) {
                    occupied.add(key);
                }
                placed = true;
                created++;
                idx = slot + 1;
            }
            if (!placed) {
                // 时段全部占满则不限制教室冲突，顺延排入
                LocalDate day = examDays.get((idx / sessions.length) % examDays.size());
                String[] session = sessions[idx % sessions.length];
                adminMapper.insertExam(teachingClassId, day.toString(), session[0], session[1], classroomId);
                created++;
                idx++;
            }
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId != null) {
            adminMapper.insertOperationLog(adminUserId, "考试管理", "生成本学期考试安排，共 " + created + " 场");
        }
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateExam(Long examId, ExamUpdateRequest request) {
        Map<String, Object> exam = adminMapper.selectExamById(examId);
        if (exam == null) {
            throw new BusinessException("考试安排不存在");
        }
        if (request.getStartTime().compareTo(request.getEndTime()) >= 0) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (request.getClassroomId() != null && !request.getClassroomId().isBlank()
            && adminMapper.countExamRoomConflict(examId, request.getExamDate(),
                request.getStartTime(), request.getEndTime(), request.getClassroomId()) > 0) {
            throw new BusinessException("修改失败：目标教室在该时段已被占用");
        }
        adminMapper.updateExam(examId, request.getExamDate(), request.getStartTime(),
            request.getEndTime(), request.getClassroomId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteExam(Long examId) {
        Map<String, Object> exam = adminMapper.selectExamById(examId);
        if (exam == null) {
            throw new BusinessException("考试安排不存在");
        }
        adminMapper.deleteExamChangeRequestsByExam(examId);
        adminMapper.deleteExam(examId);
    }

    public List<Map<String, Object>> examChangeRequests() {
        return adminMapper.selectExamChangeRequests();
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewExamChangeRequest(
        Long examChangeRequestId,
        CourseSelectionRequestReviewRequest request,
        String adminUsername
    ) {
        Map<String, Object> changeRequest = adminMapper.selectExamChangeRequestById(examChangeRequestId);
        if (changeRequest == null) {
            throw new BusinessException("考试修改申请不存在");
        }
        if (!"待审核".equals(changeRequest.get("requestStatus"))) {
            throw new BusinessException("该考试修改申请已处理，请勿重复审核");
        }

        Integer adminUserId = adminMapper.selectUserIdByUsername(adminUsername);
        if (adminUserId == null) {
            throw new BusinessException("管理员账号不存在");
        }

        String action = request.getAction();
        if ("APPROVE".equalsIgnoreCase(action)) {
            Long examId = ((Number) changeRequest.get("examId")).longValue();
            String examDate = changeRequest.get("requestedExamDate").toString().substring(0, 10);
            String startTime = changeRequest.get("requestedStartTime").toString();
            String endTime = changeRequest.get("requestedEndTime").toString();
            String classroomId = (String) changeRequest.get("requestedClassroomId");
            if (classroomId != null && !classroomId.isBlank()
                && adminMapper.countExamRoomConflict(examId, examDate, startTime, endTime, classroomId) > 0) {
                throw new BusinessException("审核失败：目标教室在该时段已被占用");
            }
            if (adminMapper.countExamTeacherConflict(examId, examDate, startTime, endTime) > 0) {
                throw new BusinessException("审核失败：申请时间冲突");
            }
            adminMapper.updateExam(examId, examDate, startTime, endTime, classroomId);
            adminMapper.updateExamChangeRequestStatus(examChangeRequestId, "已通过", request.getReviewComment(), adminUserId);
            return;
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            adminMapper.updateExamChangeRequestStatus(examChangeRequestId, "已驳回", request.getReviewComment(), adminUserId);
            return;
        }

        throw new BusinessException("不支持的审核动作");
    }
}
