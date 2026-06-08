package com.school.admin.mapper;

import com.school.admin.dto.AdminCourseRequest;
import com.school.admin.dto.AdminStudentRequest;
import com.school.admin.dto.AdminTeacherRequest;
import com.school.admin.dto.AdminTeachingClassRequest;
import com.school.admin.dto.CourseSelectionRequestReviewRequest;
import com.school.admin.dto.CourseSelectionSettingRequest;
import com.school.admin.dto.ScoreSubmitSettingRequest;
import com.school.admin.dto.SemesterRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminMapper {

    @Select("""
        SELECT
            s.student_id AS studentId,
            s.student_name AS studentName,
            s.gender AS gender,
            s.birth_date AS birthDate,
            s.hometown AS hometown,
            d.department_name AS departmentName,
            m.major_name AS majorName,
            c.class_name AS className,
            s.phone AS phone,
            s.email AS email,
            s.enrollment_year AS enrollmentYear,
            s.department_id AS departmentId,
            s.major_id AS majorId,
            s.class_id AS classId,
            s.status AS status
        FROM student s
        JOIN department d ON s.department_id = d.department_id
        JOIN major m ON s.major_id = m.major_id
        JOIN class_info c ON s.class_id = c.class_id
        ORDER BY s.student_id
        """)
    List<Map<String, Object>> selectStudents();

    @Select("""
        SELECT
            t.teacher_id AS teacherId,
            t.teacher_name AS teacherName,
            t.gender AS gender,
            t.birth_date AS birthDate,
            t.title_name AS titleName,
            t.education AS education,
            d.department_name AS departmentName,
            t.phone AS phone,
            t.email AS email,
            t.department_id AS departmentId,
            t.status AS status
        FROM teacher t
        JOIN department d ON t.department_id = d.department_id
        ORDER BY t.teacher_id
        """)
    List<Map<String, Object>> selectTeachers();

    @Select("""
        SELECT
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            c.total_hours AS totalHours,
            d.department_name AS departmentName,
            c.assessment_method AS assessmentMethod,
            c.usual_ratio AS usualRatio,
            c.exam_ratio AS examRatio,
            c.department_id AS departmentId,
            c.is_required AS isRequired,
            c.description AS description
        FROM course c
        JOIN department d ON c.department_id = d.department_id
        ORDER BY c.course_id
        """)
    List<Map<String, Object>> selectCourses();

    @Select("""
        SELECT
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            tc.semester_id AS semesterId,
            c.course_name AS courseName,
            tc.course_id AS courseId,
            t.teacher_name AS teacherName,
            tc.teacher_id AS teacherId,
            cl.building_name AS buildingName,
            cl.room_name AS roomName,
            tc.classroom_id AS classroomId,
            tc.course_start_date AS courseStartDate,
            (SELECT GROUP_CONCAT(
                        CONCAT('周', s.week_day, ' ', s.section_desc,
                               IF(s.week_parity = '全周', '', CONCAT('(', s.week_parity, ')')))
                        ORDER BY s.week_day, s.section_desc SEPARATOR '；')
             FROM teaching_class_session s
             WHERE s.teaching_class_id = tc.teaching_class_id) AS scheduleText,
            tc.week_range AS weekRange,
            COALESCE(tc.is_retake_class, 0) AS isRetakeClass,
            tc.max_students AS maxStudents,
            tc.selected_count AS selectedCount,
            CASE
                WHEN sem.course_select_start IS NULL OR sem.course_select_end IS NULL THEN '未开始'
                WHEN NOW() < sem.course_select_start THEN '未开始'
                WHEN NOW() > sem.course_select_end THEN '已结束'
                ELSE '选课中'
            END AS status
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        ORDER BY sem.semester_name DESC, tc.teaching_class_id
        """)
    List<Map<String, Object>> selectTeachingClasses();

    @Select("""
        SELECT
            s.student_id AS studentId,
            s.student_name AS studentName,
            ci.class_name AS className,
            s.phone AS phone,
            cs.selection_time AS selectionTime,
            cs.selection_status AS selectionStatus
        FROM course_selection cs
        JOIN student s ON cs.student_id = s.student_id
        JOIN class_info ci ON s.class_id = ci.class_id
        WHERE cs.teaching_class_id = #{teachingClassId}
          AND cs.selection_status = '已选'
        ORDER BY s.student_id
        """)
    List<Map<String, Object>> selectTeachingClassStudents(@Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            tc.teaching_class_id AS teachingClassId,
            t.teacher_name AS teacherName,
            tc.max_students AS maxStudents,
            tc.selected_count AS selectedCount
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        ORDER BY sem.semester_name, c.course_name
        """)
    List<Map<String, Object>> selectCourseSelectionStatistics();

    @Select("""
        SELECT department_id AS value, department_name AS label
        FROM department
        ORDER BY department_id
        """)
    List<Map<String, Object>> selectDepartmentOptions();

    @Select("""
        SELECT major_id AS value, major_name AS label, department_id AS departmentId
        FROM major
        ORDER BY major_id
        """)
    List<Map<String, Object>> selectMajorOptions();

    @Select("""
        SELECT class_id AS value, class_name AS label, major_id AS majorId
        FROM class_info
        ORDER BY class_id
        """)
    List<Map<String, Object>> selectClassOptions();

    @Select("""
        SELECT semester_id AS value, semester_name AS label
        FROM semester
        ORDER BY semester_id DESC
        """)
    List<Map<String, Object>> selectSemesterOptions();

    @Select("""
        SELECT
            semester_id AS semesterId,
            semester_name AS semesterName,
            school_year AS schoolYear,
            term_code AS termCode,
            start_date AS startDate,
            end_date AS endDate,
            course_select_start AS courseSelectStart,
            course_select_end AS courseSelectEnd,
            is_current AS isCurrent
        FROM semester
        ORDER BY semester_id DESC
        """)
    List<Map<String, Object>> selectAllSemesters();

    @Insert("""
        INSERT INTO semester (
            semester_name, school_year, term_code, start_date, end_date,
            course_select_start, course_select_end, is_current
        ) VALUES (
            #{request.semesterName}, #{request.schoolYear}, #{request.termCode},
            #{request.startDate}, #{request.endDate},
            #{request.courseSelectStart}, #{request.courseSelectEnd}, 0
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "semesterId", keyColumn = "semester_id")
    int insertSemester(@Param("request") SemesterRequest request);

    @Update("""
        UPDATE semester SET
            semester_name = #{request.semesterName},
            school_year = #{request.schoolYear},
            term_code = #{request.termCode},
            start_date = #{request.startDate},
            end_date = #{request.endDate},
            course_select_start = #{request.courseSelectStart},
            course_select_end = #{request.courseSelectEnd}
        WHERE semester_id = #{request.semesterId}
        """)
    int updateSemester(@Param("request") SemesterRequest request);

    @Delete("DELETE FROM semester WHERE semester_id = #{semesterId}")
    int deleteSemester(@Param("semesterId") Integer semesterId);

    @Select("""
        SELECT COUNT(*) FROM semester
        WHERE semester_name = #{semesterName}
          AND (#{excludeId} IS NULL OR semester_id <> #{excludeId})
        """)
    int countSemesterByName(@Param("semesterName") String semesterName, @Param("excludeId") Integer excludeId);

    @Select("SELECT COUNT(*) FROM semester WHERE semester_id = #{semesterId} AND is_current = 1")
    int isCurrentSemester(@Param("semesterId") Integer semesterId);

    @Select("SELECT COUNT(*) FROM teaching_class WHERE semester_id = #{semesterId}")
    int countTeachingClassesBySemester(@Param("semesterId") Integer semesterId);

    @Select("""
        SELECT end_date FROM semester
        WHERE start_date < #{startDate}
          AND (#{excludeId} IS NULL OR semester_id <> #{excludeId})
        ORDER BY start_date DESC
        LIMIT 1
        """)
    String selectPreviousSemesterEndDate(@Param("startDate") String startDate, @Param("excludeId") Integer excludeId);

    @Update("UPDATE semester SET is_current = 0")
    int clearCurrentSemester();

    @Update("UPDATE semester SET is_current = 1 WHERE semester_id = #{semesterId}")
    int markCurrentSemester(@Param("semesterId") Integer semesterId);

    @Select("SELECT COUNT(*) FROM semester WHERE semester_id = #{semesterId}")
    int countSemesterById(@Param("semesterId") Integer semesterId);

    @Insert("""
        INSERT INTO teaching_class (
            semester_id, course_id, teacher_id, classroom_id,
            course_start_date, week_range, is_retake_class, max_students, selected_count
        )
        SELECT
            #{targetSemesterId}, course_id, teacher_id, classroom_id,
            NULL, week_range, COALESCE(is_retake_class, 0), max_students, 0
        FROM teaching_class
        WHERE semester_id = #{sourceSemesterId}
        """)
    int carryOverTeachingClasses(
        @Param("sourceSemesterId") Integer sourceSemesterId,
        @Param("targetSemesterId") Integer targetSemesterId
    );

    /** 将源学期教学班的上课时间复制到目标学期对应教学班（按 course_id+teacher_id 匹配）。 */
    @Insert("""
        INSERT INTO teaching_class_session (teaching_class_id, week_day, section_desc, week_parity, sort_order)
        SELECT new_tc.teaching_class_id, s.week_day, s.section_desc, s.week_parity, s.sort_order
        FROM teaching_class_session s
        JOIN teaching_class old_tc ON s.teaching_class_id = old_tc.teaching_class_id
            AND old_tc.semester_id = #{sourceSemesterId}
        JOIN teaching_class new_tc ON new_tc.semester_id = #{targetSemesterId}
            AND new_tc.course_id = old_tc.course_id
            AND new_tc.teacher_id = old_tc.teacher_id
        WHERE NOT EXISTS (
            SELECT 1 FROM teaching_class_session ns WHERE ns.teaching_class_id = new_tc.teaching_class_id
        )
        """)
    int carryOverSessions(
        @Param("sourceSemesterId") Integer sourceSemesterId,
        @Param("targetSemesterId") Integer targetSemesterId
    );

    @Select("""
        SELECT teacher_id AS value, teacher_name AS label
        FROM teacher
        ORDER BY teacher_id
        """)
    List<Map<String, Object>> selectTeacherOptions();

    @Select("""
        SELECT classroom_id AS value,
               CONCAT(building_name, '-', room_name) AS label
        FROM classroom
        ORDER BY classroom_id
        """)
    List<Map<String, Object>> selectClassroomOptions();

    @Select("""
        SELECT course_id AS value, course_name AS label
        FROM course
        ORDER BY course_id
        """)
    List<Map<String, Object>> selectCourseOptions();

    @Insert("""
        INSERT INTO student (
            student_id, student_name, gender, birth_date, hometown, phone, email,
            enrollment_year, department_id, major_id, class_id, status
        ) VALUES (
            #{request.studentId}, #{request.studentName}, #{request.gender},
            NULLIF(#{request.birthDate}, ''), #{request.hometown}, #{request.phone}, #{request.email},
            #{request.enrollmentYear}, #{request.departmentId}, #{request.majorId}, #{request.classId}, #{request.status}
        )
        """)
    int insertStudent(@Param("request") AdminStudentRequest request);

    @Update("""
        UPDATE student
        SET student_name = #{request.studentName},
            gender = #{request.gender},
            birth_date = NULLIF(#{request.birthDate}, ''),
            hometown = #{request.hometown},
            phone = #{request.phone},
            email = #{request.email},
            enrollment_year = #{request.enrollmentYear},
            department_id = #{request.departmentId},
            major_id = #{request.majorId},
            class_id = #{request.classId},
            status = #{request.status}
        WHERE student_id = #{studentId}
        """)
    int updateStudent(
        @Param("studentId") String studentId,
        @Param("request") AdminStudentRequest request
    );

    @Delete("DELETE FROM student WHERE student_id = #{studentId}")
    int deleteStudent(@Param("studentId") String studentId);

    @Insert("""
        INSERT INTO teacher (
            teacher_id, teacher_name, gender, birth_date, title_name, education, phone, email, department_id, status
        ) VALUES (
            #{request.teacherId}, #{request.teacherName}, #{request.gender},
            NULLIF(#{request.birthDate}, ''), #{request.titleName}, #{request.education},
            #{request.phone}, #{request.email}, #{request.departmentId}, #{request.status}
        )
        """)
    int insertTeacher(@Param("request") AdminTeacherRequest request);

    @Update("""
        UPDATE teacher
        SET teacher_name = #{request.teacherName},
            gender = #{request.gender},
            birth_date = NULLIF(#{request.birthDate}, ''),
            title_name = #{request.titleName},
            education = #{request.education},
            phone = #{request.phone},
            email = #{request.email},
            department_id = #{request.departmentId},
            status = #{request.status}
        WHERE teacher_id = #{teacherId}
        """)
    int updateTeacher(
        @Param("teacherId") String teacherId,
        @Param("request") AdminTeacherRequest request
    );

    @Delete("DELETE FROM teacher WHERE teacher_id = #{teacherId}")
    int deleteTeacher(@Param("teacherId") String teacherId);

    @Insert("""
        INSERT INTO course (
            course_id, course_name, course_type, credit, total_hours,
            assessment_method, usual_ratio, exam_ratio, department_id, is_required, description
        ) VALUES (
            #{request.courseId}, #{request.courseName}, #{request.courseType}, #{request.credit}, #{request.totalHours},
            #{request.assessmentMethod}, #{request.usualRatio}, #{request.examRatio}, #{request.departmentId}, #{request.isRequired}, #{request.description}
        )
        """)
    int insertCourse(@Param("request") AdminCourseRequest request);

    @Update("""
        UPDATE course
        SET course_name = #{request.courseName},
            course_type = #{request.courseType},
            credit = #{request.credit},
            total_hours = #{request.totalHours},
            assessment_method = #{request.assessmentMethod},
            usual_ratio = #{request.usualRatio},
            exam_ratio = #{request.examRatio},
            department_id = #{request.departmentId},
            is_required = #{request.isRequired},
            description = #{request.description}
        WHERE course_id = #{courseId}
        """)
    int updateCourse(
        @Param("courseId") String courseId,
        @Param("request") AdminCourseRequest request
    );

    @Delete("DELETE FROM course WHERE course_id = #{courseId}")
    int deleteCourse(@Param("courseId") String courseId);

    @Insert("""
        INSERT INTO teaching_class (
            semester_id, course_id, teacher_id, classroom_id,
            course_start_date, week_range, is_retake_class, max_students, selected_count
        ) VALUES (
            #{request.semesterId}, #{request.courseId}, #{request.teacherId}, NULLIF(#{request.classroomId}, ''),
            #{request.courseStartDate}, #{request.weekRange},
            COALESCE(#{request.isRetakeClass}, 0), #{request.maxStudents}, 0
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "request.generatedId")
    int insertTeachingClass(@Param("request") AdminTeachingClassRequest request);

    @Update("""
        UPDATE teaching_class
        SET semester_id = #{request.semesterId},
            course_id = #{request.courseId},
            teacher_id = #{request.teacherId},
            classroom_id = NULLIF(#{request.classroomId}, ''),
            course_start_date = #{request.courseStartDate},
            week_range = #{request.weekRange},
            is_retake_class = COALESCE(#{request.isRetakeClass}, 0),
            max_students = #{request.maxStudents}
        WHERE teaching_class_id = #{teachingClassId}
        """)
    int updateTeachingClass(
        @Param("teachingClassId") Integer teachingClassId,
        @Param("request") AdminTeachingClassRequest request
    );

    @Delete("DELETE FROM teaching_class WHERE teaching_class_id = #{teachingClassId}")
    int deleteTeachingClass(@Param("teachingClassId") Integer teachingClassId);

    @Delete("DELETE FROM teaching_class_session WHERE teaching_class_id = #{teachingClassId}")
    int deleteSessions(@Param("teachingClassId") Integer teachingClassId);

    @Insert("""
        INSERT INTO teaching_class_session (teaching_class_id, week_day, section_desc, week_parity, sort_order)
        VALUES (#{teachingClassId}, #{slot.weekDay}, #{slot.sectionDesc}, #{slot.weekParity}, #{slot.sortOrder})
        """)
    int insertSession(
        @Param("teachingClassId") Integer teachingClassId,
        @Param("slot") com.school.common.schedule.SessionSlot slot
    );

    @Select("SELECT credit FROM course WHERE course_id = #{courseId}")
    java.math.BigDecimal selectCourseCredit(@Param("courseId") String courseId);

    /** 同学期同教师或同教室已占用的上课时段，用于自动排课避让。 */
    @Select("""
        SELECT s.week_day AS weekDay, s.section_desc AS sectionDesc, s.week_parity AS weekParity
        FROM teaching_class_session s
        JOIN teaching_class tc ON s.teaching_class_id = tc.teaching_class_id
        WHERE tc.semester_id = #{semesterId}
          AND tc.teaching_class_id <> COALESCE(#{excludeTeachingClassId}, -1)
          AND (tc.teacher_id = #{teacherId}
               OR (NULLIF(#{classroomId}, '') IS NOT NULL AND tc.classroom_id = #{classroomId}))
        """)
    List<Map<String, Object>> selectOccupiedSlots(
        @Param("semesterId") Integer semesterId,
        @Param("teacherId") String teacherId,
        @Param("classroomId") String classroomId,
        @Param("excludeTeachingClassId") Integer excludeTeachingClassId
    );

    @Select("""
        SELECT COUNT(*)
        FROM course_selection
        WHERE teaching_class_id = #{teachingClassId}
          AND selection_status = '已选'
        """)
    int countSelectedByTeachingClassId(@Param("teachingClassId") Integer teachingClassId);

    @Select("SELECT role_id FROM sys_role WHERE role_code = #{roleCode} LIMIT 1")
    Integer selectRoleIdByCode(@Param("roleCode") String roleCode);

    @Insert("""
        INSERT INTO sys_user (username, password, real_name, user_type, ref_id, is_enabled)
        VALUES (#{username}, #{password}, #{realName}, #{userType}, #{refId}, 1)
        """)
    int insertSysUser(
        @Param("username") String username,
        @Param("password") String password,
        @Param("realName") String realName,
        @Param("userType") String userType,
        @Param("refId") String refId
    );

    @Select("SELECT user_id FROM sys_user WHERE username = #{username} LIMIT 1")
    Integer selectUserIdByUsername(@Param("username") String username);

    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertSysUserRole(@Param("userId") Integer userId, @Param("roleId") Integer roleId);

    @Update("""
        UPDATE sys_user
        SET real_name = #{realName}
        WHERE ref_id = #{refId}
        """)
    int updateSysUserRealName(
        @Param("refId") String refId,
        @Param("realName") String realName
    );

    @Delete("""
        DELETE ur FROM sys_user_role ur
        JOIN sys_user u ON ur.user_id = u.user_id
        WHERE u.ref_id = #{refId}
        """)
    int deleteUserRoleByRefId(@Param("refId") String refId);

    @Delete("DELETE FROM sys_user WHERE ref_id = #{refId}")
    int deleteSysUserByRefId(@Param("refId") String refId);

    @Select("""
        SELECT
            semester_id AS semesterId,
            semester_name AS semesterName,
            course_select_start AS courseSelectStart,
            course_select_end AS courseSelectEnd,
            makeup_request_start AS makeupRequestStart,
            makeup_request_end AS makeupRequestEnd,
            is_current AS isCurrent
        FROM semester
        WHERE is_current = 1
        LIMIT 1
        """)
    Map<String, Object> selectCurrentSemesterSetting();

    @Update("""
        UPDATE semester
        SET course_select_start = #{request.courseSelectStart},
            course_select_end = #{request.courseSelectEnd},
            makeup_request_start = NULLIF(#{request.makeupRequestStart}, ''),
            makeup_request_end = NULLIF(#{request.makeupRequestEnd}, '')
        WHERE semester_id = #{semesterId}
        """)
    int updateCourseSelectionTime(
        @Param("semesterId") Integer semesterId,
        @Param("request") CourseSelectionSettingRequest request
    );

    @Select("""
        SELECT
            semester_id AS semesterId,
            semester_name AS semesterName,
            score_submit_start AS scoreSubmitStart,
            score_submit_end AS scoreSubmitEnd
        FROM semester
        WHERE is_current = 1
        LIMIT 1
        """)
    Map<String, Object> selectCurrentScoreSubmitSetting();

    @Update("""
        UPDATE semester
        SET score_submit_start = #{request.scoreSubmitStart},
            score_submit_end = #{request.scoreSubmitEnd}
        WHERE semester_id = #{semesterId}
        """)
    int updateScoreSubmitTime(
        @Param("semesterId") Integer semesterId,
        @Param("request") ScoreSubmitSettingRequest request
    );

    @Select("""
        SELECT
            sc.score_id AS scoreId,
            sc.student_id AS studentId,
            s.student_name AS studentName,
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            t.teacher_name AS teacherName,
            sc.usual_score AS usualScore,
            sc.exam_score AS examScore,
            sc.final_score AS finalScore,
            sc.gpa_point AS gpaPoint,
            sc.entered_at AS enteredAt
        FROM score sc
        JOIN student s ON sc.student_id = s.student_id
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        WHERE sc.is_submitted = 1
        ORDER BY sc.entered_at DESC, sc.score_id DESC
        """)
    List<Map<String, Object>> selectSubmittedScores();

    @Update("""
        UPDATE score
        SET is_submitted = 0
        WHERE score_id = #{scoreId}
          AND is_submitted = 1
        """)
    int rollbackSubmittedScore(@Param("scoreId") Long scoreId);

    @Select("""
        SELECT
            scr.score_change_request_id AS scoreChangeRequestId,
            scr.score_id AS scoreId,
            scr.teacher_id AS teacherId,
            t.teacher_name AS teacherName,
            scr.student_id AS studentId,
            s.student_name AS studentName,
            scr.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            scr.original_usual_score AS originalUsualScore,
            scr.original_exam_score AS originalExamScore,
            scr.original_final_score AS originalFinalScore,
            scr.original_gpa_point AS originalGpaPoint,
            scr.request_reason AS requestReason,
            scr.request_status AS requestStatus,
            scr.review_comment AS reviewComment,
            scr.created_at AS createdAt,
            scr.reviewed_at AS reviewedAt
        FROM score_change_request scr
        JOIN teacher t ON scr.teacher_id = t.teacher_id
        JOIN student s ON scr.student_id = s.student_id
        JOIN teaching_class tc ON scr.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        ORDER BY CASE scr.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END, scr.created_at DESC
        """)
    List<Map<String, Object>> selectScoreChangeRequests();

    @Select("""
        SELECT
            score_change_request_id AS scoreChangeRequestId,
            score_id AS scoreId,
            request_status AS requestStatus
        FROM score_change_request
        WHERE score_change_request_id = #{scoreChangeRequestId}
        LIMIT 1
        """)
    Map<String, Object> selectScoreChangeRequestById(@Param("scoreChangeRequestId") Long scoreChangeRequestId);

    @Update("""
        UPDATE score_change_request
        SET request_status = #{status},
            review_comment = #{reviewComment},
            reviewed_by = #{reviewedBy},
            reviewed_at = NOW(),
            updated_at = NOW()
        WHERE score_change_request_id = #{scoreChangeRequestId}
        """)
    int updateScoreChangeRequestStatus(
        @Param("scoreChangeRequestId") Long scoreChangeRequestId,
        @Param("status") String status,
        @Param("reviewComment") String reviewComment,
        @Param("reviewedBy") Integer reviewedBy
    );

    @Select("""
        SELECT
            sar.adjust_request_id AS adjustRequestId,
            sar.teacher_id AS teacherId,
            t.teacher_name AS teacherName,
            sar.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            sar.original_week_day AS originalWeekDay,
            sar.original_section_desc AS originalSectionDesc,
            sar.original_week_range AS originalWeekRange,
            old_cl.building_name AS originalBuildingName,
            old_cl.room_name AS originalRoomName,
            sar.requested_week_day AS requestedWeekDay,
            sar.requested_section_desc AS requestedSectionDesc,
            sar.requested_week_range AS requestedWeekRange,
            sar.requested_classroom_id AS requestedClassroomId,
            new_cl.building_name AS requestedBuildingName,
            new_cl.room_name AS requestedRoomName,
            sar.request_reason AS requestReason,
            sar.request_status AS requestStatus,
            sar.review_comment AS reviewComment,
            sar.created_at AS createdAt,
            sar.reviewed_at AS reviewedAt
        FROM schedule_adjust_request sar
        JOIN teaching_class tc ON sar.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON sar.teacher_id = t.teacher_id
        LEFT JOIN classroom old_cl ON sar.original_classroom_id = old_cl.classroom_id
        LEFT JOIN classroom new_cl ON sar.requested_classroom_id = new_cl.classroom_id
        ORDER BY CASE sar.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END, sar.created_at DESC
        """)
    List<Map<String, Object>> selectScheduleAdjustRequests();

    @Select("""
        SELECT
            sar.adjust_request_id AS adjustRequestId,
            sar.teaching_class_id AS teachingClassId,
            sar.session_id AS sessionId,
            sar.requested_week_day AS requestedWeekDay,
            sar.requested_section_desc AS requestedSectionDesc,
            sar.requested_week_range AS requestedWeekRange,
            sar.requested_classroom_id AS requestedClassroomId,
            sar.request_status AS requestStatus
        FROM schedule_adjust_request sar
        WHERE sar.adjust_request_id = #{adjustRequestId}
        LIMIT 1
        """)
    Map<String, Object> selectScheduleAdjustRequestById(@Param("adjustRequestId") Long adjustRequestId);

    @Select("""
        SELECT COUNT(*)
        FROM teaching_class target
        JOIN teaching_class other_tc
          ON other_tc.semester_id = target.semester_id
         AND other_tc.classroom_id = NULLIF(#{classroomId}, '')
        JOIN teaching_class_session other_s
          ON other_s.teaching_class_id = other_tc.teaching_class_id
         AND other_s.week_day = #{weekDay}
         AND other_s.session_id <> COALESCE(#{sessionId}, -1)
        WHERE target.teaching_class_id = #{teachingClassId}
          AND NULLIF(#{classroomId}, '') IS NOT NULL
          AND NOT (
              CAST(REPLACE(SUBSTRING_INDEX(other_s.section_desc, '-', -1), '节', '') AS UNSIGNED)
                  < CAST(SUBSTRING_INDEX(#{sectionDesc}, '-', 1) AS UNSIGNED)
              OR CAST(SUBSTRING_INDEX(other_s.section_desc, '-', 1) AS UNSIGNED)
                  > CAST(REPLACE(SUBSTRING_INDEX(#{sectionDesc}, '-', -1), '节', '') AS UNSIGNED)
          )
          AND (other_s.week_parity = '全周' OR #{weekParity} = '全周' OR other_s.week_parity = #{weekParity})
        """)
    int countTeachingClassRoomConflict(
        @Param("teachingClassId") Integer teachingClassId,
        @Param("sessionId") Integer sessionId,
        @Param("weekDay") Integer weekDay,
        @Param("sectionDesc") String sectionDesc,
        @Param("weekParity") String weekParity,
        @Param("classroomId") String classroomId
    );

    @Select("""
        SELECT COUNT(*)
        FROM teaching_class target
        JOIN teaching_class other_tc
          ON other_tc.semester_id = target.semester_id
         AND other_tc.teacher_id = target.teacher_id
        JOIN teaching_class_session other_s
          ON other_s.teaching_class_id = other_tc.teaching_class_id
         AND other_s.week_day = #{weekDay}
         AND other_s.session_id <> COALESCE(#{sessionId}, -1)
        WHERE target.teaching_class_id = #{teachingClassId}
          AND NOT (
              CAST(REPLACE(SUBSTRING_INDEX(other_s.section_desc, '-', -1), '节', '') AS UNSIGNED)
                  < CAST(SUBSTRING_INDEX(#{sectionDesc}, '-', 1) AS UNSIGNED)
              OR CAST(SUBSTRING_INDEX(other_s.section_desc, '-', 1) AS UNSIGNED)
                  > CAST(REPLACE(SUBSTRING_INDEX(#{sectionDesc}, '-', -1), '节', '') AS UNSIGNED)
          )
          AND (other_s.week_parity = '全周' OR #{weekParity} = '全周' OR other_s.week_parity = #{weekParity})
        """)
    int countTeachingClassTeacherConflict(
        @Param("teachingClassId") Integer teachingClassId,
        @Param("sessionId") Integer sessionId,
        @Param("weekDay") Integer weekDay,
        @Param("sectionDesc") String sectionDesc,
        @Param("weekParity") String weekParity
    );

    @Select("SELECT week_parity FROM teaching_class_session WHERE session_id = #{sessionId}")
    String selectSessionParity(@Param("sessionId") Integer sessionId);

    @Update("""
        UPDATE teaching_class_session
        SET week_day = #{weekDay},
            section_desc = #{sectionDesc}
        WHERE session_id = #{sessionId}
        """)
    int applyScheduleAdjustSession(
        @Param("sessionId") Integer sessionId,
        @Param("weekDay") Integer weekDay,
        @Param("sectionDesc") String sectionDesc
    );

    @Update("""
        UPDATE teaching_class
        SET week_range = #{weekRange},
            classroom_id = NULLIF(#{classroomId}, '')
        WHERE teaching_class_id = #{teachingClassId}
        """)
    int applyScheduleAdjustClass(
        @Param("teachingClassId") Integer teachingClassId,
        @Param("weekRange") String weekRange,
        @Param("classroomId") String classroomId
    );

    @Update("""
        UPDATE schedule_adjust_request
        SET request_status = #{status},
            review_comment = #{reviewComment},
            reviewed_by = #{reviewedBy},
            reviewed_at = NOW(),
            updated_at = NOW()
        WHERE adjust_request_id = #{adjustRequestId}
        """)
    int updateScheduleAdjustRequestStatus(
        @Param("adjustRequestId") Long adjustRequestId,
        @Param("status") String status,
        @Param("reviewComment") String reviewComment,
        @Param("reviewedBy") Integer reviewedBy
    );

    @Select("""
        SELECT
            csr.request_id AS requestId,
            csr.student_id AS studentId,
            s.student_name AS studentName,
            csr.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            t.teacher_name AS teacherName,
            (SELECT GROUP_CONCAT(
                        CONCAT('周', s.week_day, ' ', s.section_desc,
                               IF(s.week_parity = '全周', '', CONCAT('(', s.week_parity, ')')))
                        ORDER BY s.week_day, s.section_desc SEPARATOR '；')
             FROM teaching_class_session s
             WHERE s.teaching_class_id = tc.teaching_class_id) AS scheduleText,
            tc.week_range AS weekRange,
            csr.request_reason AS requestReason,
            csr.request_status AS requestStatus,
            csr.review_comment AS reviewComment,
            csr.created_at AS createdAt,
            csr.reviewed_at AS reviewedAt
        FROM course_selection_request csr
        JOIN student s ON csr.student_id = s.student_id
        JOIN teaching_class tc ON csr.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        ORDER BY CASE csr.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END, csr.created_at DESC
        """)
    List<Map<String, Object>> selectCourseSelectionRequests();

    @Select("""
        SELECT request_id AS requestId, student_id AS studentId, teaching_class_id AS teachingClassId, request_status AS requestStatus
        FROM course_selection_request
        WHERE request_id = #{requestId}
        LIMIT 1
        """)
    Map<String, Object> selectCourseSelectionRequestById(@Param("requestId") Long requestId);

    @Select("""
        SELECT COUNT(*)
        FROM course_selection cs
        JOIN teaching_class_session s1 ON cs.teaching_class_id = s1.teaching_class_id
        JOIN teaching_class t1 ON cs.teaching_class_id = t1.teaching_class_id
        JOIN teaching_class t2 ON t2.teaching_class_id = #{teachingClassId}
        JOIN teaching_class_session s2 ON s2.teaching_class_id = t2.teaching_class_id
        WHERE cs.student_id = #{studentId}
          AND cs.selection_status = '已选'
          AND t1.semester_id = t2.semester_id
          AND s1.week_day = s2.week_day
          AND cs.teaching_class_id <> #{teachingClassId}
          AND NOT (
              CAST(REPLACE(SUBSTRING_INDEX(s1.section_desc, '-', -1), '节', '') AS UNSIGNED)
                  < CAST(SUBSTRING_INDEX(s2.section_desc, '-', 1) AS UNSIGNED)
              OR CAST(SUBSTRING_INDEX(s1.section_desc, '-', 1) AS UNSIGNED)
                  > CAST(REPLACE(SUBSTRING_INDEX(s2.section_desc, '-', -1), '节', '') AS UNSIGNED)
          )
          AND (s1.week_parity = '全周' OR s2.week_parity = '全周' OR s1.week_parity = s2.week_parity)
        """)
    int countStudentTimeConflict(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Select("""
        SELECT COUNT(*)
        FROM course_selection
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND selection_status = '已选'
        """)
    int countSelectedCourse(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Select("""
        SELECT COUNT(*)
        FROM course_selection
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND selection_status = '已退'
        """)
    int countDroppedCourse(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Insert("""
        INSERT INTO course_selection (student_id, teaching_class_id, selection_status)
        VALUES (#{studentId}, #{teachingClassId}, '已选')
        """)
    int insertCourseSelectionByAdmin(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Update("""
        UPDATE course_selection
        SET selection_status = '已选',
            selection_time = NOW()
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND selection_status = '已退'
        """)
    int restoreDroppedCourseByAdmin(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Delete("""
        DELETE FROM score
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
        """)
    int deleteScore(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Update("""
        UPDATE teaching_class
        SET selected_count = selected_count + 1
        WHERE teaching_class_id = #{teachingClassId}
        """)
    int increaseSelectedCount(@Param("teachingClassId") Integer teachingClassId);

    @Update("""
        UPDATE course_selection_request
        SET request_status = #{status},
            review_comment = #{reviewComment},
            reviewed_by = #{reviewedBy},
            reviewed_at = NOW()
        WHERE request_id = #{requestId}
        """)
    int updateCourseSelectionRequestStatus(
        @Param("requestId") Long requestId,
        @Param("status") String status,
        @Param("reviewComment") String reviewComment,
        @Param("reviewedBy") Integer reviewedBy
    );

    @Select("""
        SELECT
            cdr.drop_request_id AS dropRequestId,
            cdr.student_id AS studentId,
            s.student_name AS studentName,
            cdr.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            t.teacher_name AS teacherName,
            (SELECT GROUP_CONCAT(
                        CONCAT('周', s.week_day, ' ', s.section_desc,
                               IF(s.week_parity = '全周', '', CONCAT('(', s.week_parity, ')')))
                        ORDER BY s.week_day, s.section_desc SEPARATOR '；')
             FROM teaching_class_session s
             WHERE s.teaching_class_id = tc.teaching_class_id) AS scheduleText,
            tc.week_range AS weekRange,
            cdr.request_reason AS requestReason,
            cdr.request_status AS requestStatus,
            cdr.teacher_status AS teacherStatus,
            cdr.teacher_comment AS teacherComment,
            cdr.admin_status AS adminStatus,
            cdr.admin_comment AS adminComment,
            cdr.created_at AS createdAt,
            cdr.updated_at AS updatedAt
        FROM course_drop_request cdr
        JOIN student s ON cdr.student_id = s.student_id
        JOIN teaching_class tc ON cdr.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        ORDER BY CASE cdr.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END, cdr.created_at DESC
        """)
    List<Map<String, Object>> selectCourseDropRequests();

    @Select("""
        SELECT
            drop_request_id AS dropRequestId,
            student_id AS studentId,
            teaching_class_id AS teachingClassId,
            request_status AS requestStatus,
            teacher_status AS teacherStatus,
            admin_status AS adminStatus
        FROM course_drop_request
        WHERE drop_request_id = #{dropRequestId}
        LIMIT 1
        """)
    Map<String, Object> selectCourseDropRequestById(@Param("dropRequestId") Long dropRequestId);

    @Update("""
        UPDATE course_drop_request
        SET admin_status = #{adminStatus},
            admin_comment = #{adminComment},
            admin_reviewed_by = #{adminUserId},
            admin_reviewed_at = NOW(),
            request_status = CASE
                WHEN #{adminStatus} = '已驳回' THEN '已驳回'
                WHEN teacher_status = '已驳回' THEN '已驳回'
                WHEN #{adminStatus} = '已通过' AND teacher_status = '已通过' THEN '已通过'
                ELSE '待审核'
            END,
            updated_at = NOW()
        WHERE drop_request_id = #{dropRequestId}
        """)
    int updateAdminDropReview(
        @Param("dropRequestId") Long dropRequestId,
        @Param("adminStatus") String adminStatus,
        @Param("adminComment") String adminComment,
        @Param("adminUserId") Integer adminUserId
    );

    @Select("""
        SELECT request_status AS requestStatus
        FROM course_drop_request
        WHERE drop_request_id = #{dropRequestId}
        LIMIT 1
        """)
    String selectDropRequestStatus(@Param("dropRequestId") Long dropRequestId);

    @Update("""
        UPDATE course_selection
        SET selection_status = '已退'
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND selection_status = '已选'
        """)
    int dropSelectedCourse(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Select("""
        SELECT
            srr.request_id AS requestId,
            srr.score_id AS scoreId,
            srr.student_id AS studentId,
            s.student_name AS studentName,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            t.teacher_name AS teacherName,
            sc.usual_score AS usualScore,
            sc.exam_score AS examScore,
            sc.final_score AS finalScore,
            sc.gpa_point AS gpaPoint,
            srr.request_reason AS requestReason,
            srr.request_status AS requestStatus,
            srr.review_comment AS reviewComment,
            reviewer.real_name AS reviewedByName,
            srr.created_at AS createdAt,
            srr.reviewed_at AS reviewedAt
        FROM score_review_request srr
        JOIN student s ON srr.student_id = s.student_id
        JOIN score sc ON srr.score_id = sc.score_id
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN sys_user reviewer ON srr.reviewed_by = reviewer.user_id
        ORDER BY CASE srr.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END,
                 srr.created_at DESC,
                 srr.request_id DESC
        """)
    List<Map<String, Object>> selectScoreReviewRequests();

    @Select("""
        SELECT
            request_id AS requestId,
            score_id AS scoreId,
            student_id AS studentId,
            request_status AS requestStatus
        FROM score_review_request
        WHERE request_id = #{requestId}
        LIMIT 1
        """)
    Map<String, Object> selectScoreReviewRequestById(@Param("requestId") Long requestId);

    @Update("""
        UPDATE score_review_request
        SET request_status = #{status},
            review_comment = #{reviewComment},
            reviewed_by = #{reviewedBy},
            reviewed_at = NOW()
        WHERE request_id = #{requestId}
        """)
    int updateScoreReviewRequestStatus(
        @Param("requestId") Long requestId,
        @Param("status") String status,
        @Param("reviewComment") String reviewComment,
        @Param("reviewedBy") Integer reviewedBy
    );

    @Insert("""
        INSERT INTO operation_log (user_id, operation_type, operation_content)
        VALUES (#{userId}, #{operationType}, #{operationContent})
        """)
    int insertOperationLog(
        @Param("userId") Integer userId,
        @Param("operationType") String operationType,
        @Param("operationContent") String operationContent
    );

    // ==================== 仪表盘概览 ====================

    @Select("""
        SELECT
            (SELECT COUNT(*) FROM student) AS studentCount,
            (SELECT COUNT(*) FROM teacher) AS teacherCount,
            (SELECT COUNT(*) FROM course) AS courseCount,
            (SELECT COUNT(*) FROM teaching_class tc
                JOIN semester sem ON tc.semester_id = sem.semester_id
                WHERE sem.is_current = 1) AS teachingClassCount,
            (SELECT COUNT(*) FROM course_selection_request
                WHERE request_status = '待审核') AS pendingSelectionRequests,
            (SELECT COUNT(*) FROM course_drop_request
                WHERE request_status = '待审核' AND admin_status = '待审核') AS pendingDropRequests,
            (SELECT COUNT(*) FROM schedule_adjust_request
                WHERE request_status = '待审核') AS pendingScheduleRequests,
            (SELECT COUNT(*) FROM score_change_request
                WHERE request_status = '待审核') AS pendingScoreChangeRequests,
            (SELECT COUNT(*) FROM makeup_exam_request
                WHERE request_status = '待管理员排教室'
                  AND admin_status = '待安排') AS pendingMakeupRoomRequests
        """)
    Map<String, Object> selectDashboardOverview();

    // ==================== 成绩图表分析 ====================

    @Select("""
        SELECT
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            t.teacher_name AS teacherName,
            COUNT(sc.score_id) AS scoredCount
        FROM score sc
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        WHERE sc.is_submitted = 1 AND sc.final_score IS NOT NULL
        GROUP BY tc.teaching_class_id, sem.semester_name, c.course_name, t.teacher_name
        ORDER BY sem.semester_name DESC, c.course_name
        """)
    List<Map<String, Object>> selectScoredTeachingClasses();

    @Select("""
        SELECT
            COUNT(*) AS totalCount,
            ROUND(AVG(final_score), 2) AS avgScore,
            MAX(final_score) AS maxScore,
            MIN(final_score) AS minScore,
            SUM(CASE WHEN final_score >= 60 THEN 1 ELSE 0 END) AS passCount,
            SUM(CASE WHEN final_score < 60 THEN 1 ELSE 0 END) AS rangeFail,
            SUM(CASE WHEN final_score >= 60 AND final_score < 70 THEN 1 ELSE 0 END) AS range60,
            SUM(CASE WHEN final_score >= 70 AND final_score < 80 THEN 1 ELSE 0 END) AS range70,
            SUM(CASE WHEN final_score >= 80 AND final_score < 90 THEN 1 ELSE 0 END) AS range80,
            SUM(CASE WHEN final_score >= 90 THEN 1 ELSE 0 END) AS range90
        FROM score
        WHERE is_submitted = 1
          AND final_score IS NOT NULL
          AND teaching_class_id = #{teachingClassId}
        """)
    Map<String, Object> selectScoreDistribution(@Param("teachingClassId") Integer teachingClassId);

    // ==================== 学业扩展 ====================

    @Select("""
        SELECT
            req.requirement_id AS requirementId,
            req.enrollment_year AS enrollmentYear,
            req.major_id AS majorId,
            m.major_name AS majorName,
            req.course_type AS courseType,
            req.required_credit AS requiredCredit,
            req.remark AS remark,
            req.updated_at AS updatedAt
        FROM cultivation_plan_requirement req
        LEFT JOIN major m ON req.major_id = m.major_id
        ORDER BY req.enrollment_year DESC, m.major_name, req.course_type
        """)
    List<Map<String, Object>> selectCultivationPlanRequirements();

    @Insert("""
        INSERT INTO cultivation_plan_requirement (
            enrollment_year, major_id, course_type, required_credit, remark
        ) VALUES (
            #{enrollmentYear}, #{majorId}, #{courseType}, #{requiredCredit}, #{remark}
        )
        ON DUPLICATE KEY UPDATE
            required_credit = #{requiredCredit},
            remark = #{remark},
            updated_at = NOW()
        """)
    int upsertCultivationPlanRequirement(
        @Param("enrollmentYear") Integer enrollmentYear,
        @Param("majorId") String majorId,
        @Param("courseType") String courseType,
        @Param("requiredCredit") BigDecimal requiredCredit,
        @Param("remark") String remark
    );

    @Select("""
        SELECT
            mer.request_id AS requestId,
            mer.student_id AS studentId,
            s.student_name AS studentName,
            mer.score_id AS scoreId,
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            t.teacher_id AS teacherId,
            t.teacher_name AS teacherName,
            sc.final_score AS finalScore,
            mer.request_reason AS requestReason,
            mer.request_status AS requestStatus,
            mer.teacher_status AS teacherStatus,
            mer.teacher_comment AS teacherComment,
            mer.admin_status AS adminStatus,
            mer.admin_comment AS adminComment,
            mer.result_status AS resultStatus,
            mer.review_comment AS reviewComment,
            mer.requested_exam_time AS requestedExamTime,
            mer.confirmed_exam_time AS confirmedExamTime,
            mer.classroom_id AS classroomId,
            cl.building_name AS buildingName,
            cl.room_name AS roomName,
            mer.created_at AS createdAt,
            mer.reviewed_at AS reviewedAt,
            mer.teacher_reviewed_at AS teacherReviewedAt,
            mer.admin_reviewed_at AS adminReviewedAt
        FROM makeup_exam_request mer
        JOIN student s ON mer.student_id = s.student_id
        JOIN score sc ON mer.score_id = sc.score_id
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON mer.classroom_id = cl.classroom_id
        ORDER BY CASE mer.request_status
                    WHEN '待教师确认' THEN 1
                    WHEN '待管理员排教室' THEN 2
                    WHEN '已安排' THEN 3
                    WHEN '已通过' THEN 4
                    ELSE 5
                 END,
                 mer.created_at DESC,
                 mer.request_id DESC
        """)
    List<Map<String, Object>> selectMakeupExamRequests();

    @Select("""
        SELECT
            request_id AS requestId,
            student_id AS studentId,
            score_id AS scoreId,
            requested_exam_time AS requestedExamTime,
            confirmed_exam_time AS confirmedExamTime,
            request_status AS requestStatus,
            teacher_status AS teacherStatus,
            admin_status AS adminStatus,
            result_status AS resultStatus
        FROM makeup_exam_request
        WHERE request_id = #{requestId}
        LIMIT 1
        """)
    Map<String, Object> selectMakeupExamRequestById(@Param("requestId") Long requestId);

    @Update("""
        UPDATE makeup_exam_request
        SET request_status = #{requestStatus},
            admin_status = #{adminStatus},
            admin_comment = #{adminComment},
            admin_reviewed_by = #{reviewedBy},
            admin_reviewed_at = NOW(),
            reviewed_by = #{reviewedBy},
            reviewed_at = NOW(),
            classroom_id = NULLIF(#{classroomId}, ''),
            result_status = #{resultStatus},
            updated_at = NOW()
        WHERE request_id = #{requestId}
        """)
    int updateMakeupExamArrangement(
        @Param("requestId") Long requestId,
        @Param("requestStatus") String requestStatus,
        @Param("adminStatus") String adminStatus,
        @Param("adminComment") String adminComment,
        @Param("classroomId") String classroomId,
        @Param("resultStatus") String resultStatus,
        @Param("reviewedBy") Integer reviewedBy
    );

    @Select("""
        SELECT COUNT(*)
        FROM makeup_exam_request mer
        WHERE NULLIF(#{classroomId}, '') IS NOT NULL
          AND mer.classroom_id = NULLIF(#{classroomId}, '')
          AND mer.request_id <> #{requestId}
          AND mer.request_status = '已安排'
          AND mer.confirmed_exam_time < #{endDateTime}
          AND #{startDateTime} < DATE_ADD(mer.confirmed_exam_time, INTERVAL 2 HOUR)
        """)
    int countMakeupExamRoomConflict(
        @Param("requestId") Long requestId,
        @Param("startDateTime") String startDateTime,
        @Param("endDateTime") String endDateTime,
        @Param("classroomId") String classroomId
    );

    @Update("""
        UPDATE makeup_exam_request
        SET result_status = #{resultStatus},
            updated_at = NOW()
        WHERE request_id = #{requestId}
          AND request_status IN ('已安排', '已通过')
        """)
    int updateMakeupExamResult(
        @Param("requestId") Long requestId,
        @Param("resultStatus") String resultStatus
    );

    @Select("""
        SELECT
            crr.request_id AS requestId,
            crr.student_id AS studentId,
            s.student_name AS studentName,
            crr.score_id AS scoreId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            sc.final_score AS finalScore,
            crr.request_reason AS requestReason,
            crr.request_status AS requestStatus,
            crr.review_comment AS reviewComment,
            crr.created_at AS createdAt,
            crr.reviewed_at AS reviewedAt
        FROM course_retake_request crr
        JOIN student s ON crr.student_id = s.student_id
        JOIN score sc ON crr.score_id = sc.score_id
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        ORDER BY CASE crr.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END,
                 crr.created_at DESC,
                 crr.request_id DESC
        """)
    List<Map<String, Object>> selectRetakeRequests();

    @Select("""
        SELECT
            request_id AS requestId,
            student_id AS studentId,
            score_id AS scoreId,
            request_status AS requestStatus
        FROM course_retake_request
        WHERE request_id = #{requestId}
        LIMIT 1
        """)
    Map<String, Object> selectRetakeRequestById(@Param("requestId") Long requestId);

    @Update("""
        UPDATE course_retake_request
        SET request_status = #{requestStatus},
            review_comment = #{reviewComment},
            reviewed_by = #{reviewedBy},
            reviewed_at = NOW(),
            updated_at = NOW()
        WHERE request_id = #{requestId}
        """)
    int updateRetakeReview(
        @Param("requestId") Long requestId,
        @Param("requestStatus") String requestStatus,
        @Param("reviewComment") String reviewComment,
        @Param("reviewedBy") Integer reviewedBy
    );

    // ==================== 考试管理 ====================

    @Select("""
        SELECT
            ea.exam_id AS examId,
            ea.teaching_class_id AS teachingClassId,
            ea.exam_date AS examDate,
            ea.start_time AS startTime,
            ea.end_time AS endTime,
            ea.classroom_id AS classroomId,
            ea.exam_status AS examStatus,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            t.teacher_id AS teacherId,
            t.teacher_name AS teacherName,
            cl.building_name AS buildingName,
            cl.room_name AS roomName
        FROM exam_arrangement ea
        JOIN teaching_class tc ON ea.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON ea.classroom_id = cl.classroom_id
        WHERE sem.is_current = 1
        ORDER BY ea.exam_date, ea.start_time, c.course_name
        """)
    List<Map<String, Object>> selectExams();

    @Select("""
        SELECT end_date AS endDate
        FROM semester
        WHERE is_current = 1
        LIMIT 1
        """)
    Map<String, Object> selectCurrentSemesterEndDate();

    @Select("""
        SELECT tc.teaching_class_id AS teachingClassId,
               tc.classroom_id AS classroomId
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        WHERE sem.is_current = 1
          AND NOT EXISTS (
              SELECT 1 FROM exam_arrangement ea
              WHERE ea.teaching_class_id = tc.teaching_class_id
          )
        ORDER BY tc.teaching_class_id
        """)
    List<Map<String, Object>> selectTeachingClassesWithoutExam();

    @Insert("""
        INSERT INTO exam_arrangement (teaching_class_id, exam_date, start_time, end_time, classroom_id, exam_status)
        VALUES (#{teachingClassId}, #{examDate}, #{startTime}, #{endTime}, NULLIF(#{classroomId}, ''), '已排定')
        """)
    int insertExam(
        @Param("teachingClassId") Integer teachingClassId,
        @Param("examDate") String examDate,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime,
        @Param("classroomId") String classroomId
    );

    @Select("""
        SELECT
            ea.exam_id AS examId,
            ea.teaching_class_id AS teachingClassId,
            ea.classroom_id AS classroomId
        FROM exam_arrangement ea
        WHERE ea.exam_id = #{examId}
        LIMIT 1
        """)
    Map<String, Object> selectExamById(@Param("examId") Long examId);

    @Select("""
        SELECT COUNT(*)
        FROM exam_arrangement ea
        WHERE NULLIF(#{classroomId}, '') IS NOT NULL
          AND ea.classroom_id = NULLIF(#{classroomId}, '')
          AND ea.exam_date = #{examDate}
          AND ea.exam_id <> #{examId}
          AND ea.start_time < #{endTime}
          AND #{startTime} < ea.end_time
        """)
    int countExamRoomConflict(
        @Param("examId") Long examId,
        @Param("examDate") String examDate,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime,
        @Param("classroomId") String classroomId
    );

    @Select("""
        SELECT COUNT(*)
        FROM exam_arrangement target
        JOIN teaching_class target_tc ON target.teaching_class_id = target_tc.teaching_class_id
        JOIN exam_arrangement ea ON ea.exam_id <> target.exam_id
        JOIN teaching_class tc ON ea.teaching_class_id = tc.teaching_class_id
        WHERE target.exam_id = #{examId}
          AND tc.teacher_id = target_tc.teacher_id
          AND ea.exam_date = #{examDate}
          AND ea.start_time < #{endTime}
          AND #{startTime} < ea.end_time
        """)
    int countExamTeacherConflict(
        @Param("examId") Long examId,
        @Param("examDate") String examDate,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );

    @Update("""
        UPDATE exam_arrangement
        SET exam_date = #{examDate},
            start_time = #{startTime},
            end_time = #{endTime},
            classroom_id = NULLIF(#{classroomId}, ''),
            updated_at = NOW()
        WHERE exam_id = #{examId}
        """)
    int updateExam(
        @Param("examId") Long examId,
        @Param("examDate") String examDate,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime,
        @Param("classroomId") String classroomId
    );

    @Delete("""
        DELETE FROM exam_change_request WHERE exam_id = #{examId}
        """)
    int deleteExamChangeRequestsByExam(@Param("examId") Long examId);

    @Delete("""
        DELETE FROM exam_arrangement WHERE exam_id = #{examId}
        """)
    int deleteExam(@Param("examId") Long examId);

    @Select("""
        SELECT
            ecr.exam_change_request_id AS examChangeRequestId,
            ecr.exam_id AS examId,
            ecr.teaching_class_id AS teachingClassId,
            ecr.teacher_id AS teacherId,
            t.teacher_name AS teacherName,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            ecr.original_exam_date AS originalExamDate,
            ecr.original_start_time AS originalStartTime,
            ecr.original_end_time AS originalEndTime,
            old_cl.building_name AS originalBuildingName,
            old_cl.room_name AS originalRoomName,
            ecr.requested_exam_date AS requestedExamDate,
            ecr.requested_start_time AS requestedStartTime,
            ecr.requested_end_time AS requestedEndTime,
            new_cl.building_name AS requestedBuildingName,
            new_cl.room_name AS requestedRoomName,
            ecr.request_reason AS requestReason,
            ecr.request_status AS requestStatus,
            ecr.review_comment AS reviewComment,
            ecr.created_at AS createdAt,
            ecr.reviewed_at AS reviewedAt
        FROM exam_change_request ecr
        JOIN teaching_class tc ON ecr.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON ecr.teacher_id = t.teacher_id
        LEFT JOIN classroom old_cl ON ecr.original_classroom_id = old_cl.classroom_id
        LEFT JOIN classroom new_cl ON ecr.requested_classroom_id = new_cl.classroom_id
        ORDER BY CASE ecr.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END, ecr.created_at DESC
        """)
    List<Map<String, Object>> selectExamChangeRequests();

    @Select("""
        SELECT
            ecr.exam_change_request_id AS examChangeRequestId,
            ecr.exam_id AS examId,
            ecr.requested_exam_date AS requestedExamDate,
            ecr.requested_start_time AS requestedStartTime,
            ecr.requested_end_time AS requestedEndTime,
            ecr.requested_classroom_id AS requestedClassroomId,
            ecr.request_status AS requestStatus
        FROM exam_change_request ecr
        WHERE ecr.exam_change_request_id = #{examChangeRequestId}
        LIMIT 1
        """)
    Map<String, Object> selectExamChangeRequestById(@Param("examChangeRequestId") Long examChangeRequestId);

    @Update("""
        UPDATE exam_change_request
        SET request_status = #{status},
            review_comment = #{reviewComment},
            reviewed_by = #{reviewedBy},
            reviewed_at = NOW(),
            updated_at = NOW()
        WHERE exam_change_request_id = #{examChangeRequestId}
        """)
    int updateExamChangeRequestStatus(
        @Param("examChangeRequestId") Long examChangeRequestId,
        @Param("status") String status,
        @Param("reviewComment") String reviewComment,
        @Param("reviewedBy") Integer reviewedBy
    );
}
