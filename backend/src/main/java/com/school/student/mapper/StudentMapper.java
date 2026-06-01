package com.school.student.mapper;

import java.util.List;
import java.util.Map;
import com.school.student.dto.StudentCourseQueryRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudentMapper {

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
            tc.selected_count AS selectedCount
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
    List<Map<String, Object>> selectAvailableCourses(@Param("query") StudentCourseQueryRequest query);

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
            student_id AS studentId,
            student_name AS studentName,
            semester_name AS semesterName,
            course_name AS courseName,
            usual_score AS usualScore,
            exam_score AS examScore,
            final_score AS finalScore,
            gpa_point AS gpaPoint
        FROM v_student_score
        WHERE student_id = #{studentId}
        ORDER BY semester_name, course_name
        """)
    List<Map<String, Object>> selectScores(@Param("studentId") String studentId);

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
