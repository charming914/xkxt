# 教学事务管理系统接口清单

技术假设：
- 前端：Vue
- 后端：Spring Boot
- 基础路径：`/api`
- 认证方式：登录成功后返回 token

## 1. 认证模块

### `POST /api/auth/login`
用途：用户登录

请求参数：
```json
{
  "username": "20240001",
  "password": "123456"
}
```

返回结果：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "xxxxx",
    "userId": 2,
    "username": "20240001",
    "realName": "李明",
    "roleCode": "STUDENT"
  }
}
```

### `GET /api/auth/profile`
用途：获取当前登录用户信息

### `PUT /api/auth/password`
用途：修改密码

请求参数：
```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

## 2. 管理员模块

### 2.1 院系管理

### `GET /api/admin/departments`
用途：分页查询院系

### `POST /api/admin/departments`
用途：新增院系

### `PUT /api/admin/departments/{departmentId}`
用途：修改院系

### `DELETE /api/admin/departments/{departmentId}`
用途：删除院系

### 2.2 专业与班级管理

### `GET /api/admin/majors`
用途：查询专业列表

### `POST /api/admin/majors`
用途：新增专业

### `GET /api/admin/classes`
用途：查询班级列表

### `POST /api/admin/classes`
用途：新增班级

### 2.3 学生管理

### `GET /api/admin/students`
用途：分页查询学生

查询参数：
- `studentId`
- `studentName`
- `departmentId`
- `majorId`
- `classId`

### `POST /api/admin/students`
用途：新增学生

### `PUT /api/admin/students/{studentId}`
用途：修改学生信息

### `DELETE /api/admin/students/{studentId}`
用途：删除学生

### `GET /api/admin/students/{studentId}`
用途：查看学生详情

### 2.4 教师管理

### `GET /api/admin/teachers`
用途：分页查询教师

### `POST /api/admin/teachers`
用途：新增教师

### `PUT /api/admin/teachers/{teacherId}`
用途：修改教师

### `DELETE /api/admin/teachers/{teacherId}`
用途：删除教师

### 2.5 课程管理

### `GET /api/admin/courses`
用途：分页查询课程

### `POST /api/admin/courses`
用途：新增课程

### `PUT /api/admin/courses/{courseId}`
用途：修改课程

### `DELETE /api/admin/courses/{courseId}`
用途：删除课程

### 2.6 学期与教学班管理

### `GET /api/admin/semesters`
用途：查询学期列表

### `POST /api/admin/semesters`
用途：新增学期

### `PUT /api/admin/semesters/{semesterId}`
用途：修改学期

### `GET /api/admin/teaching-classes`
用途：分页查询教学班

查询参数：
- `semesterId`
- `courseId`
- `teacherId`
- `status`

### `POST /api/admin/teaching-classes`
用途：新增教学班

### `PUT /api/admin/teaching-classes/{teachingClassId}`
用途：修改教学班

### `DELETE /api/admin/teaching-classes/{teachingClassId}`
用途：删除教学班

### 2.7 统计分析

### `GET /api/admin/statistics/course-selection`
用途：查询课程选课人数统计

### `GET /api/admin/statistics/department-offering`
用途：查询院系开课统计

### `GET /api/admin/statistics/score-analysis`
用途：查询成绩分析

### `GET /api/admin/statistics/teacher-workload`
用途：查询教师工作量统计

### `GET /api/admin/statistics/student-credit`
用途：查询学生学分完成情况

## 3. 学生模块

### `GET /api/student/courses/available`
用途：查看当前学期可选课程

查询参数：
- `semesterId`
- `courseName`

### `POST /api/student/course-selections`
用途：学生选课

请求参数：
```json
{
  "studentId": "20240001",
  "teachingClassId": 1
}
```

说明：
- 后端内部调用存储过程 `sp_select_course`

### `DELETE /api/student/course-selections/{teachingClassId}`
用途：学生退课

### `GET /api/student/course-selections`
用途：查看本人已选课程

### `GET /api/student/timetable`
用途：查看本人课表

### `GET /api/student/scores`
用途：查看本人成绩

### `GET /api/student/credits`
用途：查看本人学分统计

## 4. 教师模块

### `GET /api/teacher/teaching-classes`
用途：查看本人授课列表

### `GET /api/teacher/teaching-classes/{teachingClassId}/students`
用途：查看教学班学生名单

### `GET /api/teacher/timetable`
用途：查看本人课表

### `POST /api/teacher/scores`
用途：录入成绩

请求参数：
```json
{
  "studentId": "20240001",
  "teachingClassId": 3,
  "usualScore": 85,
  "examScore": 90,
  "isSubmitted": 1
}
```

说明：
- 插入或更新 `score` 表
- 触发器自动计算 `final_score` 和 `gpa_point`

### `PUT /api/teacher/scores/{scoreId}`
用途：修改成绩

### `GET /api/teacher/scores`
用途：查看成绩列表

查询参数：
- `teachingClassId`
- `studentId`

## 5. 建议的统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 6. 建议的权限控制

- `ADMIN`：访问 `/api/admin/**`
- `TEACHER`：访问 `/api/teacher/**`
- `STUDENT`：访问 `/api/student/**`
- `/api/auth/**`：登录后开放基础访问
