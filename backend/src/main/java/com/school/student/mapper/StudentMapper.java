package com.school.student.mapper;

import java.util.List;
import java.util.Map;
import com.school.student.dto.StudentCourseQueryRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface StudentMapper {

    @Select("""
        SELECT ref_id
        FROM sys_user
        WHERE username = #{username}
          AND user_type = '学生'
          AND is_enabled = 1
        LIMIT 1
        """)
    String selectStudentIdByUsername(@Param("username") String username);

    @Select("""
        <script>
        SELECT
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            c.total_hours AS totalHours,
            t.teacher_name AS teacherName,
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
            tc.week_range AS weekRange,
            cl.building_name AS buildingName,
            cl.room_name AS roomName,
            tc.max_students AS maxStudents,
            tc.selected_count AS selectedCount,
            CASE
                WHEN NOW() BETWEEN sem.course_select_start AND sem.course_select_end
                 AND tc.selected_count &lt; tc.max_students
                 AND NOT EXISTS (
                    SELECT 1 FROM course_selection selected
                    WHERE selected.student_id = #{studentId}
                      AND selected.teaching_class_id = tc.teaching_class_id
                      AND selected.selection_status = '已选'
                 )
                THEN 1 ELSE 0
            END AS canSelectDirectly,
            CASE
                WHEN (
                    NOW() NOT BETWEEN sem.course_select_start AND sem.course_select_end
                    OR tc.selected_count >= tc.max_students
                )
                 AND NOT EXISTS (
                    SELECT 1 FROM course_selection selected
                    WHERE selected.student_id = #{studentId}
                      AND selected.teaching_class_id = tc.teaching_class_id
                      AND selected.selection_status = '已选'
                 )
                 AND NOT EXISTS (
                    SELECT 1 FROM course_selection_request pending
                    WHERE pending.student_id = #{studentId}
                      AND pending.teaching_class_id = tc.teaching_class_id
                      AND pending.request_status = '待审核'
                 )
                THEN 1 ELSE 0
            END AS canRequestSelection,
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM course_selection selected
                    WHERE selected.student_id = #{studentId}
                      AND selected.teaching_class_id = tc.teaching_class_id
                      AND selected.selection_status = '已选'
                )
                THEN 1 ELSE 0
            END AS alreadySelected,
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM course_selection_request pending
                    WHERE pending.student_id = #{studentId}
                      AND pending.teaching_class_id = tc.teaching_class_id
                      AND pending.request_status = '待审核'
                )
                THEN 1 ELSE 0
            END AS hasPendingSelectionRequest
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        WHERE sem.is_current = 1
          AND tc.status = '选课中'
          <if test="query.teacherName != null and query.teacherName != ''">
            AND t.teacher_name LIKE CONCAT('%', #{query.teacherName}, '%')
          </if>
          <if test="query.courseName != null and query.courseName != ''">
            AND c.course_name LIKE CONCAT('%', #{query.courseName}, '%')
          </if>
          <if test="query.courseId != null and query.courseId != ''">
            AND c.course_id LIKE CONCAT('%', #{query.courseId}, '%')
          </if>
          <if test="query.courseType != null and query.courseType != ''">
            AND c.course_type = #{query.courseType}
          </if>
          <if test="query.credit != null">
            AND c.credit = #{query.credit}
          </if>
          <if test="query.weekDay != null">
            AND tc.week_day = #{query.weekDay}
          </if>
          <if test="query.sectionDesc != null and query.sectionDesc != ''">
            AND (
                tc.section_desc = #{query.sectionDesc}
                OR (#{query.sectionDesc} IN ('1-2节', '3-4节') AND tc.section_desc = '1-4节')
                OR (#{query.sectionDesc} IN ('5-6节', '7-8节') AND tc.section_desc = '5-8节')
                OR (#{query.sectionDesc} IN ('9-10节', '11-12节') AND tc.section_desc = '9-12节')
            )
          </if>
        ORDER BY tc.week_day, tc.section_desc
        </script>
        """)
    List<Map<String, Object>> selectAvailableCourses(
        @Param("query") StudentCourseQueryRequest query,
        @Param("studentId") String studentId
    );

    @Select("""
        SELECT
            cs.selection_id AS selectionId,
            cs.selection_time AS selectionTime,
            cs.selection_status AS selectionStatus,
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            c.total_hours AS totalHours,
            t.teacher_name AS teacherName,
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
            tc.week_range AS weekRange,
            cl.building_name AS buildingName,
            cl.room_name AS roomName
        FROM course_selection cs
        JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        WHERE cs.student_id = #{studentId}
          AND cs.selection_status = '已选'
        ORDER BY sem.semester_name, tc.week_day, tc.section_desc
        """)
    List<Map<String, Object>> selectSelectedCourses(@Param("studentId") String studentId);

    @Select("""
        SELECT
            cs.selection_id AS selectionId,
            cs.selection_time AS selectionTime,
            tc.teaching_class_id AS teachingClassId,
            tc.course_start_date AS courseStartDate,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            c.total_hours AS totalHours,
            t.teacher_name AS teacherName,
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
            tc.week_range AS weekRange,
            cl.building_name AS buildingName,
            cl.room_name AS roomName,
            CASE
                WHEN tc.course_start_date IS NULL THEN 'FORBIDDEN'
                WHEN CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 7 DAY) THEN 'DIRECT'
                WHEN CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 21 DAY) THEN 'REQUEST'
                ELSE 'FORBIDDEN'
            END AS dropMode,
            CASE
                WHEN tc.course_start_date IS NULL THEN '课程开课日期未设置，无法退课'
                WHEN CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 7 DAY) THEN '可直接退课'
                WHEN CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 21 DAY) THEN '需提交退课申请'
                ELSE '已超过开课三周，不能退课'
            END AS dropModeLabel,
            CASE
                WHEN tc.course_start_date IS NOT NULL
                 AND CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 7 DAY)
                THEN 1 ELSE 0
            END AS canDropDirectly,
            CASE
                WHEN tc.course_start_date IS NOT NULL
                 AND CURDATE() > DATE_ADD(tc.course_start_date, INTERVAL 7 DAY)
                 AND CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 21 DAY)
                THEN 1 ELSE 0
            END AS canRequestDrop,
            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM course_drop_request cdr
                    WHERE cdr.student_id = cs.student_id
                      AND cdr.teaching_class_id = cs.teaching_class_id
                      AND cdr.request_status = '待审核'
                )
                THEN 1 ELSE 0
            END AS hasPendingDropRequest
        FROM course_selection cs
        JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        WHERE cs.student_id = #{studentId}
          AND cs.selection_status = '已选'
        ORDER BY sem.semester_name, tc.week_day, tc.section_desc
        """)
    List<Map<String, Object>> selectDropCourses(@Param("studentId") String studentId);

    @Select("""
        SELECT
            student_id AS studentId,
            student_name AS studentName,
            semester_name AS semesterName,
            course_name AS courseName,
            teacher_name AS teacherName,
            week_day AS weekDay,
            section_desc AS sectionDesc,
            week_range AS weekRange,
            building_name AS buildingName,
            room_name AS roomName
        FROM v_student_timetable
        WHERE student_id = #{studentId}
        ORDER BY semester_name, week_day, section_desc
        """)
    List<Map<String, Object>> selectTimetable(@Param("studentId") String studentId);

    @Select("""
        SELECT
            sc.score_id AS scoreId,
            sc.student_id AS studentId,
            s.student_name AS studentName,
            sem.semester_id AS semesterId,
            sem.semester_name AS semesterName,
            tc.teaching_class_id AS teachingClassId,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            t.teacher_name AS teacherName,
            sc.usual_score AS usualScore,
            sc.exam_score AS examScore,
            sc.final_score AS finalScore,
            sc.gpa_point AS gpaPoint,
            CASE
                WHEN sc.final_score IS NULL THEN NULL
                WHEN sc.final_score >= 90 THEN 'A'
                WHEN sc.final_score >= 85 THEN 'A-'
                WHEN sc.final_score >= 82 THEN 'B+'
                WHEN sc.final_score >= 78 THEN 'B'
                WHEN sc.final_score >= 75 THEN 'B-'
                WHEN sc.final_score >= 72 THEN 'C+'
                WHEN sc.final_score >= 68 THEN 'C'
                WHEN sc.final_score >= 66 THEN 'C-'
                WHEN sc.final_score >= 64 THEN 'D'
                WHEN sc.final_score >= 60 THEN 'D-'
                ELSE 'F'
            END AS gradeLevel,
            sc.is_submitted AS isSubmitted,
            sc.entered_at AS enteredAt,
            latest_review.request_id AS scoreReviewRequestId,
            latest_review.request_status AS scoreReviewStatus,
            latest_review.request_reason AS scoreReviewReason,
            latest_review.review_comment AS scoreReviewComment,
            latest_review.created_at AS scoreReviewCreatedAt,
            latest_review.reviewed_at AS scoreReviewReviewedAt,
            CASE
                WHEN latest_review.request_status = '待审核' THEN 1
                ELSE 0
            END AS hasPendingScoreReview
        FROM score sc
        JOIN student s ON sc.student_id = s.student_id
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN score_review_request latest_review
          ON latest_review.request_id = (
            SELECT srr.request_id
            FROM score_review_request srr
            WHERE srr.score_id = sc.score_id
            ORDER BY srr.created_at DESC, srr.request_id DESC
            LIMIT 1
          )
        WHERE sc.student_id = #{studentId}
          AND sc.is_submitted = 1
        ORDER BY sem.semester_name DESC, c.course_name
        """)
    List<Map<String, Object>> selectScores(@Param("studentId") String studentId);

    @Select("""
        SELECT
            sc.score_id AS scoreId,
            sc.student_id AS studentId,
            s.student_name AS studentName,
            sem.semester_id AS semesterId,
            sem.semester_name AS semesterName,
            tc.teaching_class_id AS teachingClassId,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            t.teacher_name AS teacherName,
            sc.usual_score AS usualScore,
            sc.exam_score AS examScore,
            sc.final_score AS finalScore,
            sc.gpa_point AS gpaPoint,
            CASE
                WHEN sc.final_score IS NULL THEN NULL
                WHEN sc.final_score >= 90 THEN 'A'
                WHEN sc.final_score >= 85 THEN 'A-'
                WHEN sc.final_score >= 82 THEN 'B+'
                WHEN sc.final_score >= 78 THEN 'B'
                WHEN sc.final_score >= 75 THEN 'B-'
                WHEN sc.final_score >= 72 THEN 'C+'
                WHEN sc.final_score >= 68 THEN 'C'
                WHEN sc.final_score >= 66 THEN 'C-'
                WHEN sc.final_score >= 64 THEN 'D'
                WHEN sc.final_score >= 60 THEN 'D-'
                ELSE 'F'
            END AS gradeLevel,
            sc.is_submitted AS isSubmitted,
            sc.entered_at AS enteredAt,
            NULL AS scoreReviewRequestId,
            NULL AS scoreReviewStatus,
            NULL AS scoreReviewReason,
            NULL AS scoreReviewComment,
            NULL AS scoreReviewCreatedAt,
            NULL AS scoreReviewReviewedAt,
            0 AS hasPendingScoreReview
        FROM score sc
        JOIN student s ON sc.student_id = s.student_id
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        WHERE sc.student_id = #{studentId}
          AND sc.is_submitted = 1
        ORDER BY sem.semester_name DESC, c.course_name
        """)
    List<Map<String, Object>> selectScoresWithoutReview(@Param("studentId") String studentId);

    @Insert("""
        INSERT INTO course_selection_request (student_id, teaching_class_id, request_reason)
        VALUES (#{studentId}, #{teachingClassId}, #{requestReason})
        """)
    int insertCourseSelectionRequest(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId,
        @Param("requestReason") String requestReason
    );

    @Select("""
        SELECT COUNT(*)
        FROM course_selection_request
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND request_status = '待审核'
        """)
    int countPendingRequest(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT COUNT(*)
        FROM course_selection
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND selection_status = '已选'
        """)
    int countSelectedCourse(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT COUNT(*)
        FROM course_selection
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND selection_status = '已退'
        """)
    int countDroppedCourse(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT COUNT(*)
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        WHERE tc.teaching_class_id = #{teachingClassId}
          AND sem.is_current = 1
          AND tc.status = '选课中'
          AND NOW() BETWEEN sem.course_select_start AND sem.course_select_end
        """)
    int countSelectableTeachingClass(@Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT COUNT(*)
        FROM teaching_class
        WHERE teaching_class_id = #{teachingClassId}
          AND selected_count < max_students
        """)
    int countAvailableCapacity(@Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT COUNT(*)
        FROM course_selection cs
        JOIN teaching_class t1 ON cs.teaching_class_id = t1.teaching_class_id
        JOIN teaching_class t2 ON t2.teaching_class_id = #{teachingClassId}
        WHERE cs.student_id = #{studentId}
          AND cs.selection_status = '已选'
          AND t1.semester_id = t2.semester_id
          AND t1.week_day = t2.week_day
          AND t1.section_desc = t2.section_desc
          AND cs.teaching_class_id <> #{teachingClassId}
        """)
    int countStudentTimeConflict(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Insert("""
        INSERT INTO course_selection (student_id, teaching_class_id, selection_status)
        VALUES (#{studentId}, #{teachingClassId}, '已选')
        """)
    int insertSelectedCourse(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Update("""
        UPDATE course_selection
        SET selection_status = '已选',
            selection_time = NOW()
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND selection_status = '已退'
        """)
    int restoreDroppedCourse(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Update("""
        UPDATE teaching_class
        SET selected_count = selected_count + 1
        WHERE teaching_class_id = #{teachingClassId}
        """)
    int increaseSelectedCount(@Param("teachingClassId") Integer teachingClassId);

    @Delete("""
        DELETE FROM score
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
        """)
    int deleteScore(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Update("""
        UPDATE course_selection
        SET selection_status = '已退'
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND selection_status = '已选'
        """)
    int dropCourse(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Insert("""
        INSERT INTO course_drop_request (student_id, teaching_class_id, request_reason)
        VALUES (#{studentId}, #{teachingClassId}, #{requestReason})
        """)
    int insertCourseDropRequest(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId,
        @Param("requestReason") String requestReason
    );

    @Select("""
        SELECT COUNT(*)
        FROM course_drop_request
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
          AND request_status = '待审核'
        """)
    int countPendingDropRequest(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Select("""
        SELECT
            cdr.drop_request_id AS dropRequestId,
            cdr.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            t.teacher_name AS teacherName,
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
        JOIN teaching_class tc ON cdr.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        WHERE cdr.student_id = #{studentId}
        ORDER BY cdr.created_at DESC
        """)
    List<Map<String, Object>> selectCourseDropRequests(@Param("studentId") String studentId);

    @Insert("""
        INSERT INTO operation_log (user_id, operation_type, operation_content)
        SELECT user_id, #{operationType}, #{operationContent}
        FROM sys_user
        WHERE ref_id = #{studentId}
        LIMIT 1
        """)
    int insertOperationLog(
        @Param("studentId") String studentId,
        @Param("operationType") String operationType,
        @Param("operationContent") String operationContent
    );

    @Select("""
        SELECT COUNT(*)
        FROM score
        WHERE score_id = #{scoreId}
          AND student_id = #{studentId}
          AND is_submitted = 1
        """)
    int countReviewableScore(@Param("studentId") String studentId, @Param("scoreId") Long scoreId);

    @Select("""
        SELECT COUNT(*)
        FROM score_review_request
        WHERE student_id = #{studentId}
          AND score_id = #{scoreId}
          AND request_status = '待审核'
        """)
    int countPendingScoreReviewRequest(@Param("studentId") String studentId, @Param("scoreId") Long scoreId);

    @Insert("""
        INSERT INTO score_review_request (student_id, score_id, request_reason)
        VALUES (#{studentId}, #{scoreId}, #{requestReason})
        """)
    int insertScoreReviewRequest(
        @Param("studentId") String studentId,
        @Param("scoreId") Long scoreId,
        @Param("requestReason") String requestReason
    );

    @Select("""
        SELECT
            srr.request_id AS requestId,
            srr.score_id AS scoreId,
            srr.request_reason AS requestReason,
            srr.request_status AS requestStatus,
            srr.review_comment AS reviewComment,
            srr.created_at AS createdAt,
            srr.reviewed_at AS reviewedAt,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            t.teacher_name AS teacherName,
            sc.usual_score AS usualScore,
            sc.exam_score AS examScore,
            sc.final_score AS finalScore,
            sc.gpa_point AS gpaPoint
        FROM score_review_request srr
        JOIN score sc ON srr.score_id = sc.score_id
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        WHERE srr.student_id = #{studentId}
        ORDER BY srr.created_at DESC, srr.request_id DESC
        """)
    List<Map<String, Object>> selectScoreReviewRequests(@Param("studentId") String studentId);

    @Select("""
        SELECT
            csr.request_id AS requestId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            t.teacher_name AS teacherName,
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
            tc.week_range AS weekRange,
            csr.request_reason AS requestReason,
            csr.request_status AS requestStatus,
            csr.review_comment AS reviewComment,
            csr.created_at AS createdAt,
            csr.reviewed_at AS reviewedAt
        FROM course_selection_request csr
        JOIN teaching_class tc ON csr.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        WHERE csr.student_id = #{studentId}
        ORDER BY csr.created_at DESC
        """)
    List<Map<String, Object>> selectCourseSelectionRequests(@Param("studentId") String studentId);
}
