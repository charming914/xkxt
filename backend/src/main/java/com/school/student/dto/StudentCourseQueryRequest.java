package com.school.student.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class StudentCourseQueryRequest {

    private Integer semesterId;

    private String teacherName;

    private String courseName;

    private String courseId;

    private String courseType;

    private BigDecimal credit;

    private Integer weekDay;

    private String sectionDesc;
}

