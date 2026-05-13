package com.school.student.service;

import com.school.common.exception.BusinessException;
import com.school.student.dto.CourseSelectionRequestCreateRequest;
import com.school.student.dto.SelectCourseRequest;
import com.school.student.mapper.StudentMapper;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;
    private final DataSource dataSource;

    public List<Map<String, Object>> availableCourses() {
        return studentMapper.selectAvailableCourses();
    }

    public void selectCourse(SelectCourseRequest request) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = connection.prepareCall("{call sp_select_course(?, ?)}")) {
            statement.setString(1, request.getStudentId());
            statement.setInt(2, request.getTeachingClassId());
            statement.execute();
        } catch (SQLException e) {
            throw new BusinessException(resolveSelectCourseMessage(e.getMessage()));
        }
    }

    public List<Map<String, Object>> timetable(String studentId) {
        return studentMapper.selectTimetable(studentId);
    }

    public List<Map<String, Object>> scores(String studentId) {
        return studentMapper.selectScores(studentId);
    }

    public void createCourseSelectionRequest(CourseSelectionRequestCreateRequest request) {
        if (studentMapper.countSelectedCourse(request.getStudentId(), request.getTeachingClassId()) > 0) {
            throw new BusinessException("你已选上该课程，无需重复申请补选");
        }
        if (studentMapper.countPendingRequest(request.getStudentId(), request.getTeachingClassId()) > 0) {
            throw new BusinessException("该课程已有待审核的补选申请");
        }
        studentMapper.insertCourseSelectionRequest(
            request.getStudentId(),
            request.getTeachingClassId(),
            request.getRequestReason()
        );
    }

    public List<Map<String, Object>> courseSelectionRequests(String studentId) {
        return studentMapper.selectCourseSelectionRequests(studentId);
    }

    private String resolveSelectCourseMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "选课失败，请稍后重试";
        }
        if (rawMessage.contains("选课时间冲突")) {
            return "选课失败：该课程与已选课程时间冲突";
        }
        if (rawMessage.contains("当前不在选课时间范围内")) {
            return "选课失败：当前不在选课开放时间内";
        }
        if (rawMessage.contains("人数已满")) {
            return "选课失败：该教学班人数已满";
        }
        if (rawMessage.contains("已选该课程")) {
            return "选课失败：你已选过该课程";
        }
        return "选课失败：" + rawMessage;
    }
}
