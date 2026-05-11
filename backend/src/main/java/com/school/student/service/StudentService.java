package com.school.student.service;

import com.school.common.exception.BusinessException;
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
            throw new BusinessException("选课失败: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> timetable(String studentId) {
        return studentMapper.selectTimetable(studentId);
    }

    public List<Map<String, Object>> scores(String studentId) {
        return studentMapper.selectScores(studentId);
    }
}
