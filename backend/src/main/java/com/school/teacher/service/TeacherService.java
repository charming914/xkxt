package com.school.teacher.service;

import com.school.teacher.dto.ScoreInputRequest;
import com.school.teacher.mapper.TeacherMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherMapper teacherMapper;

    public List<Map<String, Object>> teachingClasses(String teacherId) {
        return teacherMapper.selectTeachingClasses(teacherId);
    }

    public List<Map<String, Object>> scores(String teacherId) {
        return teacherMapper.selectScores(teacherId);
    }

    public void saveScore(ScoreInputRequest request) {
        teacherMapper.saveOrUpdateScore(request, request.getIsSubmitted() == null ? 0 : request.getIsSubmitted());
    }
}
