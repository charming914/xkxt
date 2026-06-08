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
        SELECT
            s.student_id AS studentId,
            s.student_name AS studentName,
            s.gender AS gender,
            s.birth_date AS birthDate,
            s.hometown AS hometown,
            s.phone AS phone,
            s.email AS email,
            s.enrollment_year AS enrollmentYear,
            s.status AS status,
            d.department_id AS departmentId,
            d.department_name AS departmentName,
            m.major_id AS majorId,
            m.major_name AS majorName,
            ci.class_id AS classId,
            ci.class_name AS className
        FROM student s
        LEFT JOIN department d ON s.department_id = d.department_id
        LEFT JOIN major m ON s.major_id = m.major_id
        LEFT JOIN class_info ci ON s.class_id = ci.class_id
        WHERE s.student_id = #{studentId}
        LIMIT 1
        """)
    Map<String, Object> selectStudentProfile(@Param("studentId") String studentId);

    @Update("""
        UPDATE student
        SET phone = #{phone},
            email = #{email}
        WHERE student_id = #{studentId}
        """)
    int updateStudentProfile(
        @Param("studentId") String studentId,
        @Param("phone") String phone,
        @Param("email") String email
    );

    @Select("""
        SELECT
            semester_id AS semesterId,
            semester_name AS semesterName,
            school_year AS schoolYear,
            term_code AS termCode,
            start_date AS startDate,
            end_date AS endDate
        FROM semester
        WHERE is_current = 1
        LIMIT 1
        """)
    Map<String, Object> selectCurrentSemester();

    @Select("""
        SELECT
            (SELECT COUNT(*)
             FROM course_selection cs
             WHERE cs.student_id = #{studentId}
               AND cs.selection_status = '已选') AS selectedCourseCount,
            (SELECT COUNT(*)
             FROM course_selection cs
             JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
             JOIN semester today_sem ON tc.semester_id = today_sem.semester_id
             WHERE cs.student_id = #{studentId}
               AND cs.selection_status = '已选'
               AND today_sem.is_current = 1
               AND tc.week_day = WEEKDAY(CURDATE()) + 1) AS todayCourseCount,
            (SELECT COUNT(*)
             FROM course_selection_request csr
             WHERE csr.student_id = #{studentId}
               AND csr.request_status = '待审核') AS pendingSelectionRequests,
            (SELECT COUNT(*)
             FROM course_drop_request cdr
             WHERE cdr.student_id = #{studentId}
               AND cdr.request_status = '待审核') AS pendingDropRequests,
            (SELECT COUNT(*)
             FROM score_review_request srr
             WHERE srr.student_id = #{studentId}
               AND srr.request_status = '待审核') AS pendingScoreReviewRequests,
            (SELECT COUNT(*)
             FROM score sc
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1) AS submittedScoreCount,
            (SELECT COUNT(*)
             FROM score sc
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1
               AND sc.final_score < 60) AS failedScoreCount,
            (SELECT COUNT(DISTINCT tc.teaching_class_id)
             FROM score sc
             JOIN teaching_class failed_tc ON sc.teaching_class_id = failed_tc.teaching_class_id
             JOIN teaching_class tc ON tc.course_id = failed_tc.course_id
             JOIN semester sem ON tc.semester_id = sem.semester_id
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1
               AND sc.final_score < 60
               AND COALESCE(tc.is_retake_class, 0) = 1
               AND NOW() BETWEEN sem.course_select_start AND sem.course_select_end
               AND NOT EXISTS (
                   SELECT 1
                   FROM makeup_exam_request passed_makeup
                   WHERE passed_makeup.student_id = sc.student_id
                     AND passed_makeup.score_id = sc.score_id
                     AND passed_makeup.result_status = '已通过'
               )
               AND NOT EXISTS (
                   SELECT 1
                   FROM course_selection selected
                   WHERE selected.student_id = #{studentId}
                     AND selected.teaching_class_id = tc.teaching_class_id
                     AND selected.selection_status = '已选'
               )) AS openRetakeClassCount,
            (SELECT ROUND(SUM(c.credit * sc.gpa_point) / NULLIF(SUM(c.credit), 0), 2)
             FROM score sc
             JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
             JOIN course c ON tc.course_id = c.course_id
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1
               AND sc.gpa_point IS NOT NULL) AS weightedGpa,
            (SELECT sem.semester_name
             FROM semester sem
             WHERE sem.is_current = 1
             LIMIT 1) AS currentSemesterName,
            (SELECT CONCAT(c.course_name, '（', t.teacher_name, '，', tc.section_desc, '）')
             FROM course_selection cs
             JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
             JOIN semester next_sem ON tc.semester_id = next_sem.semester_id
             JOIN course c ON tc.course_id = c.course_id
             JOIN teacher t ON tc.teacher_id = t.teacher_id
             WHERE cs.student_id = #{studentId}
               AND cs.selection_status = '已选'
               AND next_sem.is_current = 1
               AND tc.week_day = WEEKDAY(CURDATE()) + 1
             ORDER BY CAST(SUBSTRING_INDEX(tc.section_desc, '-', 1) AS UNSIGNED)
             LIMIT 1) AS nextCourseLabel
        """)
    Map<String, Object> selectTodoSummary(@Param("studentId") String studentId);

    @Select("""
        SELECT
            (SELECT COUNT(*)
             FROM course_selection cs
             WHERE cs.student_id = #{studentId}
               AND cs.selection_status = '已选') AS selectedCourseCount,
            (SELECT COUNT(*)
             FROM course_selection cs
             JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
             JOIN semester today_sem ON tc.semester_id = today_sem.semester_id
             WHERE cs.student_id = #{studentId}
               AND cs.selection_status = '已选'
               AND today_sem.is_current = 1
               AND tc.week_day = WEEKDAY(CURDATE()) + 1) AS todayCourseCount,
            (SELECT COUNT(*)
             FROM course_selection_request csr
             WHERE csr.student_id = #{studentId}
               AND csr.request_status = '待审核') AS pendingSelectionRequests,
            (SELECT COUNT(*)
             FROM course_drop_request cdr
             WHERE cdr.student_id = #{studentId}
               AND cdr.request_status = '待审核') AS pendingDropRequests,
            0 AS pendingScoreReviewRequests,
            (SELECT COUNT(*)
             FROM score sc
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1) AS submittedScoreCount,
            (SELECT COUNT(*)
             FROM score sc
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1
               AND sc.final_score < 60) AS failedScoreCount,
            (SELECT COUNT(DISTINCT tc.teaching_class_id)
             FROM score sc
             JOIN teaching_class failed_tc ON sc.teaching_class_id = failed_tc.teaching_class_id
             JOIN teaching_class tc ON tc.course_id = failed_tc.course_id
             JOIN semester sem ON tc.semester_id = sem.semester_id
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1
               AND sc.final_score < 60
               AND COALESCE(tc.is_retake_class, 0) = 1
               AND NOW() BETWEEN sem.course_select_start AND sem.course_select_end
               AND NOT EXISTS (
                   SELECT 1
                   FROM makeup_exam_request passed_makeup
                   WHERE passed_makeup.student_id = sc.student_id
                     AND passed_makeup.score_id = sc.score_id
                     AND passed_makeup.result_status = '已通过'
               )
               AND NOT EXISTS (
                   SELECT 1
                   FROM course_selection selected
                   WHERE selected.student_id = #{studentId}
                     AND selected.teaching_class_id = tc.teaching_class_id
                     AND selected.selection_status = '已选'
               )) AS openRetakeClassCount,
            (SELECT ROUND(SUM(c.credit * sc.gpa_point) / NULLIF(SUM(c.credit), 0), 2)
             FROM score sc
             JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
             JOIN course c ON tc.course_id = c.course_id
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1
               AND sc.gpa_point IS NOT NULL) AS weightedGpa,
            (SELECT sem.semester_name
             FROM semester sem
             WHERE sem.is_current = 1
             LIMIT 1) AS currentSemesterName,
            (SELECT CONCAT(c.course_name, '（', t.teacher_name, '，', tc.section_desc, '）')
             FROM course_selection cs
             JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
             JOIN semester next_sem ON tc.semester_id = next_sem.semester_id
             JOIN course c ON tc.course_id = c.course_id
             JOIN teacher t ON tc.teacher_id = t.teacher_id
             WHERE cs.student_id = #{studentId}
               AND cs.selection_status = '已选'
               AND next_sem.is_current = 1
               AND tc.week_day = WEEKDAY(CURDATE()) + 1
             ORDER BY CAST(SUBSTRING_INDEX(tc.section_desc, '-', 1) AS UNSIGNED)
             LIMIT 1) AS nextCourseLabel
        """)
    Map<String, Object> selectTodoSummaryWithoutReview(@Param("studentId") String studentId);

    @Select("""
        SELECT
            sem.semester_id AS semesterId,
            sem.semester_name AS semesterName,
            sem.school_year AS schoolYear,
            sem.term_code AS termCode,
            sem.start_date AS startDate,
            sem.end_date AS endDate,
            sem.is_current AS isCurrent,
            (SELECT COUNT(*)
             FROM course_selection cs
             JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
             WHERE cs.student_id = #{studentId}
               AND cs.selection_status = '已选'
               AND tc.semester_id = sem.semester_id) AS selectedCourseCount,
            (SELECT COUNT(*)
             FROM score sc
             JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1
               AND tc.semester_id = sem.semester_id) AS scoreCount,
            (SELECT COUNT(*)
             FROM exam_arrangement ea
             JOIN teaching_class tc ON ea.teaching_class_id = tc.teaching_class_id
             JOIN course_selection cs ON cs.teaching_class_id = tc.teaching_class_id
             WHERE cs.student_id = #{studentId}
               AND cs.selection_status = '已选'
               AND tc.semester_id = sem.semester_id) AS examCount
        FROM semester sem
        ORDER BY sem.is_current DESC, sem.start_date DESC, sem.semester_id DESC
        """)
    List<Map<String, Object>> selectStudentSemesters(@Param("studentId") String studentId);

    @Select("""
        SELECT
            sem.semester_id AS semesterId,
            sem.semester_name AS semesterName,
            sem.school_year AS schoolYear,
            sem.term_code AS termCode,
            sem.start_date AS startDate,
            sem.end_date AS endDate,
            sem.is_current AS isCurrent,
            (SELECT COUNT(*)
             FROM course_selection cs
             JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
             WHERE cs.student_id = #{studentId}
               AND cs.selection_status = '已选'
               AND tc.semester_id = sem.semester_id) AS selectedCourseCount,
            (SELECT COUNT(*)
             FROM score sc
             JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
             WHERE sc.student_id = #{studentId}
               AND sc.is_submitted = 1
               AND tc.semester_id = sem.semester_id) AS scoreCount,
            0 AS examCount
        FROM semester sem
        ORDER BY sem.is_current DESC, sem.start_date DESC, sem.semester_id DESC
        """)
    List<Map<String, Object>> selectStudentSemestersWithoutExams(@Param("studentId") String studentId);

    @Select("""
        <script>
        SELECT
            tc.teaching_class_id AS teachingClassId,
            sem.semester_id AS semesterId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            c.total_hours AS totalHours,
            t.teacher_name AS teacherName,
            sem.course_select_start AS courseSelectStart,
            sem.course_select_end AS courseSelectEnd,
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
                 AND NOT EXISTS (
                    SELECT 1
                    FROM course_selection selected
                    JOIN teaching_class selected_tc ON selected.teaching_class_id = selected_tc.teaching_class_id
                    WHERE selected.student_id = #{studentId}
                      AND selected.selection_status = '已选'
                      AND selected_tc.semester_id = tc.semester_id
                      AND selected_tc.week_day = tc.week_day
                      AND selected_tc.teaching_class_id != tc.teaching_class_id
                      AND NOT (
                          CAST(REPLACE(SUBSTRING_INDEX(selected_tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                              &lt; CAST(SUBSTRING_INDEX(tc.section_desc, '-', 1) AS UNSIGNED)
                          OR CAST(SUBSTRING_INDEX(selected_tc.section_desc, '-', 1) AS UNSIGNED)
                              > CAST(REPLACE(SUBSTRING_INDEX(tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                      )
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
            END AS hasPendingSelectionRequest,
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM student_course_plan plan
                    WHERE plan.student_id = #{studentId}
                      AND plan.teaching_class_id = tc.teaching_class_id
                )
                THEN 1 ELSE 0
            END AS hasCoursePlan,
            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM course_selection selected
                    JOIN teaching_class selected_tc ON selected.teaching_class_id = selected_tc.teaching_class_id
                    WHERE selected.student_id = #{studentId}
                      AND selected.selection_status = '已选'
                      AND selected_tc.semester_id = tc.semester_id
                      AND selected_tc.week_day = tc.week_day
                      AND selected_tc.teaching_class_id != tc.teaching_class_id
                      AND NOT (
                          CAST(REPLACE(SUBSTRING_INDEX(selected_tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                              &lt; CAST(SUBSTRING_INDEX(tc.section_desc, '-', 1) AS UNSIGNED)
                          OR CAST(SUBSTRING_INDEX(selected_tc.section_desc, '-', 1) AS UNSIGNED)
                              > CAST(REPLACE(SUBSTRING_INDEX(tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                      )
                )
                THEN 1 ELSE 0
            END AS hasSelectedTimeConflict,
            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM student_course_plan plan
                    JOIN teaching_class plan_tc ON plan.teaching_class_id = plan_tc.teaching_class_id
                    WHERE plan.student_id = #{studentId}
                      AND plan_tc.semester_id = tc.semester_id
                      AND plan_tc.week_day = tc.week_day
                      AND plan_tc.teaching_class_id != tc.teaching_class_id
                      AND NOT (
                          CAST(REPLACE(SUBSTRING_INDEX(plan_tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                              &lt; CAST(SUBSTRING_INDEX(tc.section_desc, '-', 1) AS UNSIGNED)
                          OR CAST(SUBSTRING_INDEX(plan_tc.section_desc, '-', 1) AS UNSIGNED)
                              > CAST(REPLACE(SUBSTRING_INDEX(tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                      )
                )
                THEN 1 ELSE 0
            END AS hasPlanTimeConflict,
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM course_selection selected
                    WHERE selected.student_id = #{studentId}
                      AND selected.teaching_class_id = tc.teaching_class_id
                      AND selected.selection_status = '已选'
                ) THEN '已选该教学班'
                WHEN EXISTS (
                    SELECT 1 FROM course_selection_request pending
                    WHERE pending.student_id = #{studentId}
                      AND pending.teaching_class_id = tc.teaching_class_id
                      AND pending.request_status = '待审核'
                ) THEN '补选申请待审核'
                WHEN NOW() &lt; sem.course_select_start THEN '选课时间未到'
                WHEN NOW() > sem.course_select_end THEN '选课时间已结束'
                WHEN tc.selected_count >= tc.max_students THEN '教学班人数已满，可提交补选申请'
                WHEN EXISTS (
                    SELECT 1
                    FROM course_selection selected
                    JOIN teaching_class selected_tc ON selected.teaching_class_id = selected_tc.teaching_class_id
                    WHERE selected.student_id = #{studentId}
                      AND selected.selection_status = '已选'
                      AND selected_tc.semester_id = tc.semester_id
                      AND selected_tc.week_day = tc.week_day
                      AND selected_tc.teaching_class_id != tc.teaching_class_id
                      AND NOT (
                          CAST(REPLACE(SUBSTRING_INDEX(selected_tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                              &lt; CAST(SUBSTRING_INDEX(tc.section_desc, '-', 1) AS UNSIGNED)
                          OR CAST(SUBSTRING_INDEX(selected_tc.section_desc, '-', 1) AS UNSIGNED)
                              > CAST(REPLACE(SUBSTRING_INDEX(tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                      )
                ) THEN '与已选课程时间冲突'
                ELSE '可直接选课'
            END AS selectionActionReason
        FROM teaching_class tc
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        WHERE 1 = 1
          <if test="query.semesterId != null">
            AND tc.semester_id = #{query.semesterId}
          </if>
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
        <script>
        SELECT
            cs.selection_id AS selectionId,
            cs.selection_time AS selectionTime,
            cs.selection_status AS selectionStatus,
            tc.teaching_class_id AS teachingClassId,
            sem.semester_id AS semesterId,
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
          <choose>
            <when test="semesterId != null">
              AND tc.semester_id = #{semesterId}
            </when>
            <otherwise>
              AND (sem.is_current = 1 OR sem.end_date >= CURDATE())
            </otherwise>
          </choose>
        ORDER BY sem.start_date DESC, tc.week_day, tc.section_desc
        </script>
        """)
    List<Map<String, Object>> selectSelectedCourses(
        @Param("studentId") String studentId,
        @Param("semesterId") Integer semesterId
    );

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
                WHEN tc.course_start_date IS NULL THEN 'DIRECT'
                WHEN CURDATE() < tc.course_start_date THEN 'DIRECT'
                WHEN CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 7 DAY) THEN 'DIRECT'
                WHEN CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 21 DAY) THEN 'REQUEST'
                ELSE 'FORBIDDEN'
            END AS dropMode,
            CASE
                WHEN tc.course_start_date IS NULL THEN '新学期未设置开课时间，可直接退课'
                WHEN CURDATE() < tc.course_start_date THEN '课程尚未开课，可直接退课'
                WHEN CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 7 DAY) THEN '可直接退课'
                WHEN CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 21 DAY) THEN '需提交退课申请'
                ELSE '已超过开课三周，不能退课'
            END AS dropModeLabel,
            CASE
                WHEN tc.course_start_date IS NULL
                  OR CURDATE() <= DATE_ADD(tc.course_start_date, INTERVAL 7 DAY)
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
        <script>
        SELECT
            cs.student_id AS studentId,
            s.student_name AS studentName,
            tc.semester_id AS semesterId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            t.teacher_name AS teacherName,
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
            tc.week_range AS weekRange,
            cl.building_name AS buildingName,
            cl.room_name AS roomName
        FROM course_selection cs
        JOIN student s ON cs.student_id = s.student_id
        JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        WHERE cs.student_id = #{studentId}
          AND cs.selection_status = '已选'
          <choose>
            <when test="semesterId != null">
              AND tc.semester_id = #{semesterId}
            </when>
            <otherwise>
              AND sem.is_current = 1
            </otherwise>
          </choose>
        ORDER BY tc.week_day, tc.section_desc
        </script>
        """)
    List<Map<String, Object>> selectTimetable(
        @Param("studentId") String studentId,
        @Param("semesterId") Integer semesterId
    );

    @Select("""
        <script>
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
          <if test="semesterId != null">
            AND tc.semester_id = #{semesterId}
          </if>
        ORDER BY sem.semester_name DESC, c.course_name
        </script>
        """)
    List<Map<String, Object>> selectScores(
        @Param("studentId") String studentId,
        @Param("semesterId") Integer semesterId
    );

    @Select("""
        <script>
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
          <if test="semesterId != null">
            AND tc.semester_id = #{semesterId}
          </if>
        ORDER BY sem.semester_name DESC, c.course_name
        </script>
        """)
    List<Map<String, Object>> selectScoresWithoutReview(
        @Param("studentId") String studentId,
        @Param("semesterId") Integer semesterId
    );

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
          AND cs.teaching_class_id <> #{teachingClassId}
          AND NOT (
              CAST(REPLACE(SUBSTRING_INDEX(t1.section_desc, '-', -1), '节', '') AS UNSIGNED)
                  < CAST(SUBSTRING_INDEX(t2.section_desc, '-', 1) AS UNSIGNED)
              OR CAST(SUBSTRING_INDEX(t1.section_desc, '-', 1) AS UNSIGNED)
                  > CAST(REPLACE(SUBSTRING_INDEX(t2.section_desc, '-', -1), '节', '') AS UNSIGNED)
          )
        """)
    int countStudentTimeConflict(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId
    );

    @Select("""
        SELECT COUNT(*)
        FROM student_course_plan plan
        JOIN teaching_class planned_tc ON plan.teaching_class_id = planned_tc.teaching_class_id
        JOIN teaching_class target_tc ON target_tc.teaching_class_id = #{teachingClassId}
        WHERE plan.student_id = #{studentId}
          AND planned_tc.semester_id = target_tc.semester_id
          AND planned_tc.week_day = target_tc.week_day
          AND planned_tc.teaching_class_id <> target_tc.teaching_class_id
          AND NOT (
              CAST(REPLACE(SUBSTRING_INDEX(planned_tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                  < CAST(SUBSTRING_INDEX(target_tc.section_desc, '-', 1) AS UNSIGNED)
              OR CAST(SUBSTRING_INDEX(planned_tc.section_desc, '-', 1) AS UNSIGNED)
                  > CAST(REPLACE(SUBSTRING_INDEX(target_tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
          )
        """)
    int countCoursePlanTimeConflict(
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

    @Select("""
        SELECT COUNT(*)
        FROM student_course_plan
        WHERE student_id = #{studentId}
          AND teaching_class_id = #{teachingClassId}
        """)
    int countCoursePlan(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Insert("""
        INSERT INTO student_course_plan (student_id, teaching_class_id)
        VALUES (#{studentId}, #{teachingClassId})
        """)
    int insertCoursePlan(@Param("studentId") String studentId, @Param("teachingClassId") Integer teachingClassId);

    @Delete("""
        DELETE FROM student_course_plan
        WHERE plan_id = #{planId}
          AND student_id = #{studentId}
        """)
    int deleteCoursePlan(@Param("studentId") String studentId, @Param("planId") Long planId);

    @Select("""
        SELECT
            plan.plan_id AS planId,
            plan.created_at AS createdAt,
            tc.teaching_class_id AS teachingClassId,
            sem.semester_id AS semesterId,
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
            tc.max_students AS maxStudents,
            tc.selected_count AS selectedCount,
            cl.building_name AS buildingName,
            cl.room_name AS roomName,
            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM course_selection cs
                    WHERE cs.student_id = plan.student_id
                      AND cs.teaching_class_id = plan.teaching_class_id
                      AND cs.selection_status = '已选'
                ) THEN 1 ELSE 0
            END AS alreadySelected,
            CASE
                WHEN tc.selected_count >= tc.max_students THEN 1 ELSE 0
            END AS isFull
        FROM student_course_plan plan
        JOIN teaching_class tc ON plan.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        WHERE plan.student_id = #{studentId}
        ORDER BY plan.created_at DESC, sem.semester_name DESC, tc.week_day, tc.section_desc
        """)
    List<Map<String, Object>> selectCoursePlans(@Param("studentId") String studentId);

    @Select("""
        SELECT COUNT(*)
        FROM score
        WHERE student_id = #{studentId}
          AND score_id = #{scoreId}
          AND is_submitted = 1
          AND final_score < 60
        """)
    int countFailedSubmittedScore(@Param("studentId") String studentId, @Param("scoreId") Long scoreId);

    @Select("""
        SELECT COUNT(*)
        FROM makeup_exam_request
        WHERE student_id = #{studentId}
          AND score_id = #{scoreId}
          AND request_status NOT IN ('已驳回', '教师已驳回', '管理员已驳回')
        """)
    int countActiveMakeupRequest(@Param("studentId") String studentId, @Param("scoreId") Long scoreId);

    @Select("""
        SELECT
            sem.makeup_request_start AS makeupRequestStart,
            sem.makeup_request_end AS makeupRequestEnd
        FROM score sc
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        WHERE sc.score_id = #{scoreId}
        LIMIT 1
        """)
    Map<String, Object> selectMakeupRequestWindow(@Param("scoreId") Long scoreId);

    @Insert("""
        INSERT INTO makeup_exam_request (
            student_id, score_id, request_reason, requested_exam_time,
            request_status, teacher_status, admin_status, result_status
        )
        VALUES (
            #{studentId}, #{scoreId}, #{requestReason}, #{requestedExamTime},
            '待教师确认', '待确认', '待安排', '未安排'
        )
        """)
    int insertMakeupExamRequest(
        @Param("studentId") String studentId,
        @Param("scoreId") Long scoreId,
        @Param("requestReason") String requestReason,
        @Param("requestedExamTime") String requestedExamTime
    );

    @Select("""
        SELECT COUNT(*)
        FROM makeup_exam_request
        WHERE student_id = #{studentId}
          AND score_id = #{scoreId}
          AND request_status IN ('已安排', '已通过')
          AND result_status = '未通过'
        """)
    int countFailedMakeupResult(@Param("studentId") String studentId, @Param("scoreId") Long scoreId);

    @Select("""
        SELECT COUNT(*)
        FROM course_retake_request
        WHERE student_id = #{studentId}
          AND score_id = #{scoreId}
          AND request_status IN ('待审核', '已通过')
        """)
    int countActiveRetakeRequest(@Param("studentId") String studentId, @Param("scoreId") Long scoreId);

    @Insert("""
        INSERT INTO course_retake_request (student_id, score_id, request_reason)
        VALUES (#{studentId}, #{scoreId}, #{requestReason})
        """)
    int insertRetakeRequest(
        @Param("studentId") String studentId,
        @Param("scoreId") Long scoreId,
        @Param("requestReason") String requestReason
    );

    @Select("""
        SELECT
            sc.score_id AS scoreId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            t.teacher_name AS teacherName,
            sc.final_score AS finalScore,
            makeup.request_status AS makeupStatus,
            makeup.teacher_status AS makeupTeacherStatus,
            makeup.admin_status AS makeupAdminStatus,
            makeup.result_status AS makeupResultStatus,
            makeup.requested_exam_time AS requestedExamTime,
            makeup.confirmed_exam_time AS confirmedExamTime,
            retake.request_status AS retakeStatus,
            CASE
                WHEN makeup.request_id IS NULL
                  OR makeup.request_status IN ('已驳回', '教师已驳回', '管理员已驳回')
                THEN 1
                ELSE 0
            END AS canApplyMakeup,
            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM makeup_exam_request mer
                    WHERE mer.student_id = sc.student_id
                      AND mer.score_id = sc.score_id
                      AND mer.request_status IN ('已安排', '已通过')
                      AND mer.result_status = '未通过'
                )
                 AND (retake.request_id IS NULL OR retake.request_status = '已驳回')
                THEN 1 ELSE 0
            END AS canApplyRetake
        FROM score sc
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN makeup_exam_request makeup
          ON makeup.request_id = (
            SELECT mer.request_id
            FROM makeup_exam_request mer
            WHERE mer.student_id = sc.student_id
              AND mer.score_id = sc.score_id
            ORDER BY mer.created_at DESC, mer.request_id DESC
            LIMIT 1
          )
        LEFT JOIN course_retake_request retake
          ON retake.request_id = (
            SELECT crr.request_id
            FROM course_retake_request crr
            WHERE crr.student_id = sc.student_id
              AND crr.score_id = sc.score_id
            ORDER BY crr.created_at DESC, crr.request_id DESC
            LIMIT 1
          )
        WHERE sc.student_id = #{studentId}
          AND sc.is_submitted = 1
          AND sc.final_score < 60
        ORDER BY sem.semester_name DESC, c.course_name
        """)
    List<Map<String, Object>> selectAcademicRemediationScores(@Param("studentId") String studentId);

    @Select("""
        SELECT
            mer.request_id AS requestId,
            mer.score_id AS scoreId,
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
            mer.admin_reviewed_at AS adminReviewedAt,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            sc.final_score AS finalScore
        FROM makeup_exam_request mer
        JOIN score sc ON mer.score_id = sc.score_id
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        LEFT JOIN classroom cl ON mer.classroom_id = cl.classroom_id
        WHERE mer.student_id = #{studentId}
        ORDER BY mer.created_at DESC, mer.request_id DESC
        """)
    List<Map<String, Object>> selectMakeupExamRequests(@Param("studentId") String studentId);

    @Select("""
        SELECT
            crr.request_id AS requestId,
            crr.score_id AS scoreId,
            crr.request_reason AS requestReason,
            crr.request_status AS requestStatus,
            crr.review_comment AS reviewComment,
            crr.created_at AS createdAt,
            crr.reviewed_at AS reviewedAt,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            sc.final_score AS finalScore
        FROM course_retake_request crr
        JOIN score sc ON crr.score_id = sc.score_id
        JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        WHERE crr.student_id = #{studentId}
        ORDER BY crr.created_at DESC, crr.request_id DESC
        """)
    List<Map<String, Object>> selectRetakeRequests(@Param("studentId") String studentId);

    @Select("""
        SELECT DISTINCT
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
            tc.course_start_date AS courseStartDate,
            tc.max_students AS maxStudents,
            tc.selected_count AS selectedCount,
            cl.building_name AS buildingName,
            cl.room_name AS roomName,
            sem.course_select_start AS courseSelectStart,
            sem.course_select_end AS courseSelectEnd,
            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM course_selection selected
                    WHERE selected.student_id = #{studentId}
                      AND selected.teaching_class_id = tc.teaching_class_id
                      AND selected.selection_status = '已选'
                ) THEN 0
                WHEN NOW() NOT BETWEEN sem.course_select_start AND sem.course_select_end THEN 0
                WHEN tc.selected_count >= tc.max_students THEN 0
                WHEN EXISTS (
                    SELECT 1
                    FROM course_selection selected
                    JOIN teaching_class selected_tc ON selected.teaching_class_id = selected_tc.teaching_class_id
                    WHERE selected.student_id = #{studentId}
                      AND selected.selection_status = '已选'
                      AND selected_tc.semester_id = tc.semester_id
                      AND selected_tc.week_day = tc.week_day
                      AND selected_tc.teaching_class_id != tc.teaching_class_id
                      AND NOT (
                          CAST(REPLACE(SUBSTRING_INDEX(selected_tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                              < CAST(SUBSTRING_INDEX(tc.section_desc, '-', 1) AS UNSIGNED)
                          OR CAST(SUBSTRING_INDEX(selected_tc.section_desc, '-', 1) AS UNSIGNED)
                              > CAST(REPLACE(SUBSTRING_INDEX(tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                      )
                ) THEN 0
                ELSE 1
            END AS canSelectRetake,
            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM course_selection selected
                    WHERE selected.student_id = #{studentId}
                      AND selected.teaching_class_id = tc.teaching_class_id
                      AND selected.selection_status = '已选'
                ) THEN '已选择该重修班'
                WHEN NOW() < sem.course_select_start THEN '重修班选课未开始'
                WHEN NOW() > sem.course_select_end THEN '重修班选课已结束'
                WHEN tc.selected_count >= tc.max_students THEN '重修班容量已满'
                WHEN EXISTS (
                    SELECT 1
                    FROM course_selection selected
                    JOIN teaching_class selected_tc ON selected.teaching_class_id = selected_tc.teaching_class_id
                    WHERE selected.student_id = #{studentId}
                      AND selected.selection_status = '已选'
                      AND selected_tc.semester_id = tc.semester_id
                      AND selected_tc.week_day = tc.week_day
                      AND selected_tc.teaching_class_id != tc.teaching_class_id
                      AND NOT (
                          CAST(REPLACE(SUBSTRING_INDEX(selected_tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                              < CAST(SUBSTRING_INDEX(tc.section_desc, '-', 1) AS UNSIGNED)
                          OR CAST(SUBSTRING_INDEX(selected_tc.section_desc, '-', 1) AS UNSIGNED)
                              > CAST(REPLACE(SUBSTRING_INDEX(tc.section_desc, '-', -1), '节', '') AS UNSIGNED)
                      )
                ) THEN '与已选课程时间冲突'
                ELSE '可选重修班'
            END AS retakeActionReason
        FROM score sc
        JOIN teaching_class failed_tc ON sc.teaching_class_id = failed_tc.teaching_class_id
        JOIN teaching_class tc ON tc.course_id = failed_tc.course_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id
        WHERE sc.student_id = #{studentId}
          AND sc.is_submitted = 1
          AND sc.final_score < 60
          AND COALESCE(tc.is_retake_class, 0) = 1
          AND NOT EXISTS (
              SELECT 1
              FROM makeup_exam_request passed_makeup
              WHERE passed_makeup.student_id = sc.student_id
                AND passed_makeup.score_id = sc.score_id
                AND passed_makeup.result_status = '已通过'
          )
          AND (sem.is_current = 1 OR sem.end_date >= CURDATE())
        ORDER BY sem.semester_name DESC, c.course_name, tc.week_day, tc.section_desc
        """)
    List<Map<String, Object>> selectRetakeClasses(@Param("studentId") String studentId);

    @Select("""
        SELECT
            req.requirement_id AS requirementId,
            req.enrollment_year AS enrollmentYear,
            req.major_id AS majorId,
            m.major_name AS majorName,
            req.course_type AS courseType,
            req.required_credit AS requiredCredit,
            COALESCE(earned.earned_credit, 0) AS earnedCredit,
            CASE
                WHEN req.required_credit - COALESCE(earned.earned_credit, 0) > 0
                THEN req.required_credit - COALESCE(earned.earned_credit, 0)
                ELSE 0
            END AS remainingCredit,
            req.remark AS remark
        FROM student s
        JOIN cultivation_plan_requirement req
          ON req.major_id = s.major_id
         AND req.enrollment_year = s.enrollment_year
        LEFT JOIN major m ON req.major_id = m.major_id
        LEFT JOIN (
            SELECT passed.course_type, SUM(passed.credit) AS earned_credit
            FROM (
                SELECT c.course_id, c.course_type, MAX(c.credit) AS credit
                FROM score sc
                JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
                JOIN course c ON tc.course_id = c.course_id
                WHERE sc.student_id = #{studentId}
                  AND sc.is_submitted = 1
                  AND sc.final_score >= 60
                GROUP BY c.course_id, c.course_type
            ) passed
            GROUP BY passed.course_type
        ) earned ON earned.course_type = req.course_type
        WHERE s.student_id = #{studentId}
        ORDER BY req.course_type
        """)
    List<Map<String, Object>> selectAcademicProgress(@Param("studentId") String studentId);

    @Select("""
        <script>
        SELECT
            ea.exam_id AS examId,
            ea.teaching_class_id AS teachingClassId,
            sem.semester_id AS semesterId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
            c.course_type AS courseType,
            c.credit AS credit,
            t.teacher_name AS teacherName,
            ea.exam_date AS examDate,
            ea.start_time AS startTime,
            ea.end_time AS endTime,
            ea.classroom_id AS classroomId,
            cl.building_name AS buildingName,
            cl.room_name AS roomName,
            ea.exam_status AS examStatus
        FROM course_selection cs
        JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id
        JOIN exam_arrangement ea ON ea.teaching_class_id = tc.teaching_class_id
        JOIN semester sem ON tc.semester_id = sem.semester_id
        JOIN course c ON tc.course_id = c.course_id
        JOIN teacher t ON tc.teacher_id = t.teacher_id
        LEFT JOIN classroom cl ON ea.classroom_id = cl.classroom_id
        WHERE cs.student_id = #{studentId}
          AND cs.selection_status = '已选'
          <choose>
            <when test="semesterId != null">
              AND tc.semester_id = #{semesterId}
            </when>
            <otherwise>
              AND sem.is_current = 1
            </otherwise>
          </choose>
        ORDER BY ea.exam_date, ea.start_time, c.course_name
        </script>
        """)
    List<Map<String, Object>> selectExams(
        @Param("studentId") String studentId,
        @Param("semesterId") Integer semesterId
    );
}
