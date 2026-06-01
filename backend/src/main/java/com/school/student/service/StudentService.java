package com.school.student.service;

import com.school.common.exception.BusinessException;
import com.school.student.dto.CourseDropRequestCreateRequest;
import com.school.student.dto.CourseSelectionRequestCreateRequest;
import com.school.student.dto.SelectCourseRequest;
import com.school.student.dto.StudentCourseQueryRequest;
import com.school.student.mapper.StudentMapper;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;
    private final DataSource dataSource;

    public List<Map<String, Object>> availableCourses(StudentCourseQueryRequest query) {
        return studentMapper.selectAvailableCourses(query);
    }

    public List<Map<String, Object>> selectedCourses(String studentId) {
        return studentMapper.selectSelectedCourses(studentId);
    }

    public List<Map<String, Object>> dropCourses(String studentId) {
        return studentMapper.selectDropCourses(studentId);
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
        String studentId = request.getStudentId();
        Integer teachingClassId = request.getTeachingClassId();

        if (studentMapper.countSelectedCourse(studentId, teachingClassId) > 0) {
            throw new BusinessException("你已选上该课程，无需重复申请补选");
        }
        if (studentMapper.countPendingRequest(studentId, teachingClassId) > 0) {
            throw new BusinessException("该课程已有待审核的补选申请");
        }
        studentMapper.insertCourseSelectionRequest(studentId, teachingClassId, request.getRequestReason());
    }

    public List<Map<String, Object>> courseSelectionRequests(String studentId) {
        return studentMapper.selectCourseSelectionRequests(studentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dropCourse(String studentId, Integer teachingClassId) {
        if (studentMapper.countSelectedCourse(studentId, teachingClassId) == 0) {
            throw new BusinessException("未选该课程，不能退课");
        }
        try {
            int updated = studentMapper.dropCourse(studentId, teachingClassId);
            if (updated == 0) {
                throw new BusinessException("退课失败：未找到可退课程");
            }
            studentMapper.insertOperationLog(
                studentId,
                "退课",
                "学生 " + studentId + " 退课成功，教学班编号：" + teachingClassId
            );
        } catch (RuntimeException e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(resolveDropCourseMessage(e.getMessage()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void createCourseDropRequest(String studentId, CourseDropRequestCreateRequest request) {
        Integer teachingClassId = request.getTeachingClassId();
        if (studentMapper.countSelectedCourse(studentId, teachingClassId) == 0) {
            throw new BusinessException("未选该课程，不能提交退课申请");
        }
        if (studentMapper.countPendingDropRequest(studentId, teachingClassId) > 0) {
            throw new BusinessException("该课程已有待审核退课申请");
        }
        try {
            studentMapper.insertCourseDropRequest(studentId, teachingClassId, request.getRequestReason());
            studentMapper.insertOperationLog(
                studentId,
                "退课申请",
                "学生 " + studentId + " 提交退课申请，教学班编号：" + teachingClassId
            );
        } catch (RuntimeException e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(resolveDropCourseMessage(e.getMessage()));
        }
    }

    public List<Map<String, Object>> courseDropRequests(String studentId) {
        return studentMapper.selectCourseDropRequests(studentId);
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

    private String resolveDropCourseMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "退课失败，请稍后重试";
        }
        if (rawMessage.contains("课程开课日期未设置")) {
            return "退课失败：课程开课日期未设置";
        }
        if (rawMessage.contains("已超过开课三周")) {
            return "退课失败：已超过开课三周，不能退课或提交申请";
        }
        if (rawMessage.contains("开课超过一周")) {
            return "退课失败：开课超过一周，需教师和管理员均同意后才能退课";
        }
        if (rawMessage.contains("开课一周内可直接退课")) {
            return "提交失败：开课一周内可直接退课，无需提交申请";
        }
        if (rawMessage.contains("未选该课程")) {
            return "退课失败：未选该课程";
        }
        if (rawMessage.contains("已有待审核退课申请")) {
            return "提交失败：该课程已有待审核退课申请";
        }
        return "退课失败：" + rawMessage;
    }
}
