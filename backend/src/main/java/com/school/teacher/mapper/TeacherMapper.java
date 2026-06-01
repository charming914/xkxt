package com.school.teacher.mapper;

import com.school.teacher.dto.ScoreInputRequest;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TeacherMapper {

    @Select("""
        SELECT
            tc.teaching_class_id AS teachingClassId,
            sem.semester_name AS semesterName,
            c.course_name AS courseName,
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
            tc.week_range AS weekRange,
            tc.selected_count AS selectedCount,
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
}
