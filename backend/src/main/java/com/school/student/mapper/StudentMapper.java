package com.school.student.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentMapper {

    @Select("""
        SELECT
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_id AS courseId,
            c.course_name AS courseName,
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
        ORDER BY tc.week_day, tc.section_desc
        """)
    List<Map<String, Object>> selectAvailableCourses();

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
