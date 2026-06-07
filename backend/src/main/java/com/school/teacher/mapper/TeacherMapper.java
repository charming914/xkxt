package com.school.teacher.mapper;

import com.school.teacher.dto.ExamChangeRequestCreateRequest;
import com.school.teacher.dto.ScheduleAdjustRequestCreateRequest;
import com.school.teacher.dto.ScoreChangeRequestCreateRequest;
import com.school.teacher.dto.ScoreInputRequest;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface TeacherMapper {

    @Select("""
        SELECT
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            c.course_id AS courseId,
            c.course_type AS courseType,
            c.credit AS credit,
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
            tc.week_range AS weekRange,
            tc.max_students AS maxStudents,
            tc.selected_count AS selectedCount,
            tc.status AS status,
            cl.building_name AS buildingName,
            cl.room_name AS roomName
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        WHERE tc.teacher_id = #{teacherId}
        ORDER BY sem.semester_name, tc.week_day, tc.section_desc
        """)
    List<Map<String, Object>> selectTeachingClasses(@Param("teacherId") String teacherId);

    @Select("""
        SELECT
            t.teacher_id AS teacherId,
            t.teacher_name AS teacherName,
            t.gender AS gender,
            t.birth_date AS birthDate,
            t.title_name AS titleName,
            t.education AS education,
            t.phone AS phone,
            t.email AS email,
            t.status AS status,
            d.department_id AS departmentId,
            d.department_name AS departmentName
        FROM teacher t
        LEFT JOIN department d ON t.department_id = d.department_id
        WHERE t.teacher_id = #{teacherId}
        LIMIT 1
        """)
    Map<String, Object> selectTeacherProfile(@Param("teacherId") String teacherId);

    @Update("""
        UPDATE teacher
        SET phone = #{phone},
            email = #{email}
        WHERE teacher_id = #{teacherId}
        """)
    int updateTeacherProfile(
        @Param("teacherId") String teacherId,
        @Param("phone") String phone,
        @Param("email") String email
    );

    @Select("""
        SELECT
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            c.total_hours AS totalHours,
            c.assessment_method AS assessmentMethod,
            d.department_name AS departmentName,
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
            tc.week_range AS weekRange,
            tc.max_students AS maxStudents,
            tc.selected_count AS selectedCount,
            tc.status AS status,
            cl.building_name AS buildingName,
            cl.room_name AS roomName,
            cl.capacity AS classroomCapacity
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        LEFT JOIN department d ON c.department_id = d.department_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        WHERE tc.teacher_id = #{teacherId}
          AND tc.teaching_class_id = #{teachingClassId}
        LIMIT 1
        """)
    Map<String, Object> selectTeachingClassDetail(
        @Param("teacherId") String teacherId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Select("""
        SELECT COUNT(1)
        FROM teaching_class
        WHERE teacher_id = #{teacherId}
          AND teaching_class_id = #{teachingClassId}
        """)
    int countTeachingClassOwner(
        @Param("teacherId") String teacherId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Select("""
        SELECT
            cs.selection_id AS selectionId,
            cs.student_id AS studentId,
            s.student_name AS studentName,
            s.gender AS gender,
            s.phone AS phone,
            s.email AS email,
            d.department_name AS departmentName,
            m.major_name AS majorName,
            ci.class_name AS className,
            cs.selection_time AS selectionTime,
            sc.usual_score AS usualScore,
            sc.exam_score AS examScore,
            sc.final_score AS finalScore,
            sc.is_submitted AS isSubmitted
        FROM course_selection cs
        JOIN student s ON cs.student_id = s.student_id
        LEFT JOIN department d ON s.department_id = d.department_id
        LEFT JOIN major m ON s.major_id = m.major_id
        LEFT JOIN class_info ci ON s.class_id = ci.class_id
        LEFT JOIN score sc ON sc.student_id = cs.student_id AND sc.teaching_class_id = cs.teaching_class_id
        WHERE cs.teaching_class_id = #{teachingClassId}
          AND cs.selection_status = '已选'
        ORDER BY ci.class_name, cs.student_id
        """)
    List<Map<String, Object>> selectTeachingClassStudents(@Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT
            sc.score_id AS scoreId,
            cs.student_id AS studentId,
            s.student_name AS studentName,
            tc.teaching_class_id AS teachingClassId,
            c.course_name AS courseName,
            sc.usual_score AS usualScore,
            sc.exam_score AS examScore,
            sc.final_score AS finalScore,
            sc.gpa_point AS gpaPoint,
            sc.is_submitted AS isSubmitted
        FROM course_selection cs
        JOIN student s ON cs.student_id = s.student_id
        JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
        JOIN course c ON tc.course_id = c.course_id
        LEFT JOIN score sc ON sc.student_id = cs.student_id AND sc.teaching_class_id = tc.teaching_class_id
        WHERE tc.teacher_id = #{teacherId}
          AND cs.selection_status = '已选'
        ORDER BY tc.teaching_class_id, cs.student_id
        """)
    List<Map<String, Object>> selectScores(@Param("teacherId") String teacherId);

    @Select("""
        SELECT
            (SELECT COUNT(*)
             FROM course_drop_request cdr
             JOIN teaching_class tc ON cdr.teaching_class_id = tc.teaching_class_id
             WHERE tc.teacher_id = #{teacherId}
               AND cdr.request_status = '待审核'
               AND cdr.teacher_status = '待审核') AS pendingDropRequests,
            (SELECT COUNT(*)
             FROM course_selection cs
             JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
             JOIN semester sem ON tc.semester_id = sem.semester_id
             LEFT JOIN score sc ON sc.student_id = cs.student_id AND sc.teaching_class_id = cs.teaching_class_id
             WHERE tc.teacher_id = #{teacherId}
               AND cs.selection_status = '已选'
               AND sc.score_id IS NULL
               AND sem.score_submit_start IS NOT NULL
               AND sem.score_submit_end IS NOT NULL
               AND NOW() BETWEEN sem.score_submit_start AND sem.score_submit_end) AS missingScores,
            (SELECT COUNT(*)
             FROM course_selection cs
             JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
             JOIN semester sem ON tc.semester_id = sem.semester_id
             JOIN score sc ON sc.student_id = cs.student_id AND sc.teaching_class_id = cs.teaching_class_id
             WHERE tc.teacher_id = #{teacherId}
               AND cs.selection_status = '已选'
               AND sc.is_submitted = 0
               AND sem.score_submit_start IS NOT NULL
               AND sem.score_submit_end IS NOT NULL
               AND NOW() BETWEEN sem.score_submit_start AND sem.score_submit_end) AS unsubmittedScores,
            (SELECT COUNT(*)
             FROM schedule_adjust_request sar
             WHERE sar.teacher_id = #{teacherId}
               AND sar.request_status = '待审核') AS pendingScheduleAdjustRequests,
            (SELECT COUNT(*)
             FROM teaching_class tc
             JOIN semester sem ON tc.semester_id = sem.semester_id
             WHERE tc.teacher_id = #{teacherId}
               AND sem.score_submit_start IS NOT NULL
               AND sem.score_submit_end IS NOT NULL
               AND NOW() BETWEEN sem.score_submit_start AND sem.score_submit_end) AS scoreSubmitWindowOpen
        """)
    Map<String, Object> selectTodoSummary(@Param("teacherId") String teacherId);

    @Select("""
        SELECT
            sem.score_submit_start AS scoreSubmitStart,
            sem.score_submit_end AS scoreSubmitEnd,
            NOW() AS currentTime
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        WHERE tc.teacher_id = #{teacherId}
          AND tc.teaching_class_id = #{teachingClassId}
        LIMIT 1
        """)
    Map<String, Object> selectScoreSubmitWindow(
        @Param("teacherId") String teacherId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Select("""
        SELECT COUNT(1)
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        WHERE tc.teacher_id = #{teacherId}
          AND tc.teaching_class_id = #{teachingClassId}
          AND sem.score_submit_start IS NOT NULL
          AND sem.score_submit_end IS NOT NULL
          AND NOW() BETWEEN sem.score_submit_start AND sem.score_submit_end
        """)
    int countOpenScoreSubmitWindow(
        @Param("teacherId") String teacherId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Select("""
        SELECT classroom_id AS value,
               CONCAT(building_name, '-', room_name) AS label
        FROM classroom
        ORDER BY classroom_id
        """)
    List<Map<String, Object>> selectClassroomOptions();

    @Select("""
        SELECT
            cl.classroom_id AS value,
            CONCAT(cl.building_name, '-', cl.room_name, '（容量', cl.capacity, '）') AS label
        FROM classroom cl
        JOIN teaching_class target ON target.teaching_class_id = #{teachingClassId}
        WHERE cl.classroom_id <> COALESCE(target.classroom_id, '')
          AND NOT EXISTS (
              SELECT 1
              FROM teaching_class occupied
              WHERE occupied.semester_id = target.semester_id
                AND occupied.teaching_class_id <> target.teaching_class_id
                AND occupied.classroom_id = cl.classroom_id
                AND occupied.week_day = #{weekDay}
                AND NOT (
                    CAST(REPLACE(SUBSTRING_INDEX(occupied.section_desc, '-', -1), '节', '') AS UNSIGNED)
                        < CAST(SUBSTRING_INDEX(#{sectionDesc}, '-', 1) AS UNSIGNED)
                    OR CAST(SUBSTRING_INDEX(occupied.section_desc, '-', 1) AS UNSIGNED)
                        > CAST(REPLACE(SUBSTRING_INDEX(#{sectionDesc}, '-', -1), '节', '') AS UNSIGNED)
                )
          )
        ORDER BY cl.classroom_id
        """)
    List<Map<String, Object>> selectAvailableClassroomOptions(
        @Param("teachingClassId") Integer teachingClassId,
        @Param("weekDay") Integer weekDay,
        @Param("sectionDesc") String sectionDesc
    );

    @Select("""
        SELECT
            sar.adjust_request_id AS adjustRequestId,
            sar.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            sar.original_week_day AS originalWeekDay,
            sar.original_section_desc AS originalSectionDesc,
            sar.original_week_range AS originalWeekRange,
            sar.original_classroom_id AS originalClassroomId,
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
        LEFT JOIN classroom old_cl ON sar.original_classroom_id = old_cl.classroom_id
        LEFT JOIN classroom new_cl ON sar.requested_classroom_id = new_cl.classroom_id
        WHERE sar.teacher_id = #{teacherId}
        ORDER BY CASE sar.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END, sar.created_at DESC
        """)
    List<Map<String, Object>> selectScheduleAdjustRequests(@Param("teacherId") String teacherId);

    @Select("""
        SELECT COUNT(*)
        FROM schedule_adjust_request
        WHERE teacher_id = #{teacherId}
          AND teaching_class_id = #{teachingClassId}
          AND request_status = '待审核'
        """)
    int countPendingScheduleAdjustRequest(
        @Param("teacherId") String teacherId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Insert("""
        INSERT INTO schedule_adjust_request (
            teaching_class_id, teacher_id,
            original_week_day, original_section_desc, original_week_range, original_classroom_id,
            requested_week_day, requested_section_desc, requested_week_range, requested_classroom_id,
            request_reason, request_status, created_at, updated_at
        )
        SELECT
            tc.teaching_class_id, tc.teacher_id,
            tc.week_day, tc.section_desc, tc.week_range, tc.classroom_id,
            #{request.requestedWeekDay}, #{request.requestedSectionDesc}, #{request.requestedWeekRange},
            NULLIF(#{request.requestedClassroomId}, ''),
            #{request.requestReason}, '待审核', NOW(), NOW()
        FROM teaching_class tc
        WHERE tc.teacher_id = #{teacherId}
          AND tc.teaching_class_id = #{request.teachingClassId}
        """)
    int insertScheduleAdjustRequest(
        @Param("teacherId") String teacherId,
        @Param("request") ScheduleAdjustRequestCreateRequest request
    );

    @Insert("""
        INSERT INTO score (
            student_id, teaching_class_id, usual_score, exam_score, is_submitted, entered_at
        ) VALUES (
            #{request.studentId}, #{request.teachingClassId}, #{request.usualScore},
            #{request.examScore}, #{submitted}, NOW()
        )
        ON DUPLICATE KEY UPDATE
            usual_score = #{request.usualScore},
            exam_score = #{request.examScore},
            is_submitted = #{submitted},
            entered_at = NOW()
        """)
    int saveOrUpdateScore(@Param("request") ScoreInputRequest request, @Param("submitted") Integer submitted);

    @Select("""
        SELECT COUNT(*)
        FROM score sc
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        WHERE sc.score_id = #{scoreId}
          AND tc.teacher_id = #{teacherId}
          AND sc.is_submitted = 1
        """)
    int countSubmittedScoreOwner(
        @Param("teacherId") String teacherId,
        @Param("scoreId") Long scoreId
    );

    @Select("""
        SELECT COUNT(*)
        FROM score_change_request
        WHERE score_id = #{scoreId}
          AND request_status = '待审核'
        """)
    int countPendingScoreChangeRequest(@Param("scoreId") Long scoreId);

    @Insert("""
        INSERT INTO score_change_request (
            score_id, teacher_id, student_id, teaching_class_id,
            original_usual_score, original_exam_score, original_final_score, original_gpa_point,
            request_reason, request_status, created_at, updated_at
        )
        SELECT
            sc.score_id, tc.teacher_id, sc.student_id, sc.teaching_class_id,
            sc.usual_score, sc.exam_score, sc.final_score, sc.gpa_point,
            #{request.requestReason}, '待审核', NOW(), NOW()
        FROM score sc
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        WHERE sc.score_id = #{request.scoreId}
          AND tc.teacher_id = #{teacherId}
          AND sc.is_submitted = 1
        """)
    int insertScoreChangeRequest(
        @Param("teacherId") String teacherId,
        @Param("request") ScoreChangeRequestCreateRequest request
    );

    @Select("""
        SELECT
            scr.score_change_request_id AS scoreChangeRequestId,
            scr.score_id AS scoreId,
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
        JOIN student s ON scr.student_id = s.student_id
        JOIN teaching_class tc ON scr.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        WHERE scr.teacher_id = #{teacherId}
        ORDER BY CASE scr.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END, scr.created_at DESC
        """)
    List<Map<String, Object>> selectScoreChangeRequests(@Param("teacherId") String teacherId);

    @Select("""
        SELECT
            cdr.drop_request_id AS dropRequestId,
            cdr.student_id AS studentId,
            s.student_name AS studentName,
            cdr.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
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
        WHERE tc.teacher_id = #{teacherId}
        ORDER BY CASE cdr.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END, cdr.created_at DESC
        """)
    List<Map<String, Object>> selectCourseDropRequests(@Param("teacherId") String teacherId);

    @Select("""
        SELECT
            cdr.drop_request_id AS dropRequestId,
            cdr.student_id AS studentId,
            cdr.teaching_class_id AS teachingClassId,
            cdr.request_status AS requestStatus,
            cdr.teacher_status AS teacherStatus,
            cdr.admin_status AS adminStatus,
            tc.teacher_id AS teacherId
        FROM course_drop_request cdr
        JOIN teaching_class tc ON cdr.teaching_class_id = tc.teaching_class_id
        WHERE cdr.drop_request_id = #{dropRequestId}
        LIMIT 1
        """)
    Map<String, Object> selectCourseDropRequestById(@Param("dropRequestId") Long dropRequestId);

    @Update("""
        UPDATE course_drop_request
        SET teacher_status = #{teacherStatus},
            teacher_comment = #{teacherComment},
            teacher_reviewed_by = #{teacherId},
            teacher_reviewed_at = NOW(),
            request_status = CASE
                WHEN #{teacherStatus} = '已驳回' THEN '已驳回'
                WHEN admin_status = '已驳回' THEN '已驳回'
                WHEN #{teacherStatus} = '已通过' AND admin_status = '已通过' THEN '已通过'
                ELSE '待审核'
            END,
            updated_at = NOW()
        WHERE drop_request_id = #{dropRequestId}
        """)
    int updateTeacherDropReview(
        @Param("dropRequestId") Long dropRequestId,
        @Param("teacherStatus") String teacherStatus,
        @Param("teacherComment") String teacherComment,
        @Param("teacherId") String teacherId
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

    @Delete("""
        DELETE FROM score
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
        """)
    int deleteScore(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT
            ea.exam_id AS examId,
            ea.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            ea.exam_date AS examDate,
            ea.start_time AS startTime,
            ea.end_time AS endTime,
            ea.classroom_id AS classroomId,
            cl.building_name AS buildingName,
            cl.room_name AS roomName,
            ea.exam_status AS examStatus
        FROM exam_arrangement ea
        JOIN teaching_class tc ON ea.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        LEFT JOIN classroom cl ON ea.classroom_id = cl.classroom_id
        WHERE tc.teacher_id = #{teacherId}
          AND sem.is_current = 1
        ORDER BY ea.exam_date, ea.start_time, c.course_name
        """)
    List<Map<String, Object>> selectExams(@Param("teacherId") String teacherId);

    @Select("""
        SELECT
            ecr.exam_change_request_id AS examChangeRequestId,
            ecr.exam_id AS examId,
            ecr.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            ecr.original_exam_date AS originalExamDate,
            ecr.original_start_time AS originalStartTime,
            ecr.original_end_time AS originalEndTime,
            ecr.original_classroom_id AS originalClassroomId,
            old_cl.building_name AS originalBuildingName,
            old_cl.room_name AS originalRoomName,
            ecr.requested_exam_date AS requestedExamDate,
            ecr.requested_start_time AS requestedStartTime,
            ecr.requested_end_time AS requestedEndTime,
            ecr.requested_classroom_id AS requestedClassroomId,
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
        LEFT JOIN classroom old_cl ON ecr.original_classroom_id = old_cl.classroom_id
        LEFT JOIN classroom new_cl ON ecr.requested_classroom_id = new_cl.classroom_id
        WHERE ecr.teacher_id = #{teacherId}
        ORDER BY CASE ecr.request_status WHEN '待审核' THEN 1 WHEN '已通过' THEN 2 ELSE 3 END, ecr.created_at DESC
        """)
    List<Map<String, Object>> selectExamChangeRequests(@Param("teacherId") String teacherId);

    @Select("""
        SELECT COUNT(*)
        FROM exam_change_request
        WHERE exam_id = #{examId}
          AND request_status = '待审核'
        """)
    int countPendingExamChangeRequest(@Param("examId") Long examId);

    @Select("""
        SELECT
            ea.exam_id AS examId,
            ea.teaching_class_id AS teachingClassId,
            tc.teacher_id AS teacherId
        FROM exam_arrangement ea
        JOIN teaching_class tc ON ea.teaching_class_id = tc.teaching_class_id
        WHERE ea.exam_id = #{examId}
        LIMIT 1
        """)
    Map<String, Object> selectExamOwner(@Param("examId") Long examId);

    @Insert("""
        INSERT INTO exam_change_request (
            exam_id, teaching_class_id, teacher_id,
            original_exam_date, original_start_time, original_end_time, original_classroom_id,
            requested_exam_date, requested_start_time, requested_end_time, requested_classroom_id,
            request_reason, request_status, created_at, updated_at
        )
        SELECT
            ea.exam_id, ea.teaching_class_id, tc.teacher_id,
            ea.exam_date, ea.start_time, ea.end_time, ea.classroom_id,
            #{request.requestedExamDate}, #{request.requestedStartTime}, #{request.requestedEndTime},
            NULLIF(#{request.requestedClassroomId}, ''),
            #{request.requestReason}, '待审核', NOW(), NOW()
        FROM exam_arrangement ea
        JOIN teaching_class tc ON ea.teaching_class_id = tc.teaching_class_id
        WHERE ea.exam_id = #{request.examId}
          AND tc.teacher_id = #{teacherId}
        """)
    int insertExamChangeRequest(
        @Param("teacherId") String teacherId,
        @Param("request") ExamChangeRequestCreateRequest request
    );
}
