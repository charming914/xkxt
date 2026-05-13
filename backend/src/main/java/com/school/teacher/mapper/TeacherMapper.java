package com.school.teacher.mapper;

import com.school.teacher.dto.ScoreInputRequest;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
