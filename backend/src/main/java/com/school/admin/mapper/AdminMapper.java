package com.school.admin.mapper;

import com.school.admin.dto.AdminCourseRequest;
import com.school.admin.dto.AdminStudentRequest;
import com.school.admin.dto.AdminTeacherRequest;
import com.school.admin.dto.CourseSelectionRequestReviewRequest;
import com.school.admin.dto.CourseSelectionSettingRequest;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
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
            assessment_method, department_id, is_required, description
        ) VALUES (
            #{request.courseId}, #{request.courseName}, #{request.courseType}, #{request.credit}, #{request.totalHours},
            #{request.assessmentMethod}, #{request.departmentId}, #{request.isRequired}, #{request.description}
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
            is_current AS isCurrent
        FROM semester
        WHERE is_current = 1
        LIMIT 1
        """)
    Map<String, Object> selectCurrentSemesterSetting();

    @Select("""
        SELECT COALESCE(MIN(status), '未开始')
        FROM teaching_class
        WHERE semester_id = #{semesterId}
        """)
    String selectTeachingClassStatusBySemesterId(@Param("semesterId") Integer semesterId);

    @Update("""
        UPDATE semester
        SET course_select_start = #{request.courseSelectStart},
            course_select_end = #{request.courseSelectEnd}
        WHERE semester_id = #{semesterId}
        """)
    int updateCourseSelectionTime(
        @Param("semesterId") Integer semesterId,
        @Param("request") CourseSelectionSettingRequest request
    );

    @Update("""
        UPDATE teaching_class
        SET status = #{status}
        WHERE semester_id = #{semesterId}
        """)
    int updateTeachingClassStatusBySemesterId(
        @Param("semesterId") Integer semesterId,
        @Param("status") String status
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
            tc.week_day AS weekDay,
            tc.section_desc AS sectionDesc,
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

    @Insert("""
        INSERT INTO course_selection (student_id, teaching_class_id, selection_status)
        VALUES (#{studentId}, #{teachingClassId}, '已选')
        """)
    int insertCourseSelectionByAdmin(
        @Param("studentId") String studentId,
        @Param("teachingClassId") Integer teachingClassId
    );

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
}
