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

说明：
- 管理员教学班页面支持按教室本地筛选，并可按当前筛选结果导出教室课表 CSV。
- 教室课表导出数据来源于教学班列表中的学期、教室、星期、节次、周次和课程信息。

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

### `GET /api/admin/score-submit-setting`
用途：查询当前学期成绩提交时间窗口

### `PUT /api/admin/score-submit-setting`
用途：设置当前学期成绩提交时间窗口

请求参数：
```json
{
  "scoreSubmitStart": "2026-06-01 08:00:00",
  "scoreSubmitEnd": "2026-07-20 23:59:59"
}
```

### `GET /api/admin/scores/submitted`
用途：查询已提交成绩列表

### `PUT /api/admin/scores/{scoreId}/rollback`
用途：将已提交成绩退回为可修改状态

### `GET /api/admin/score-change-requests`
用途：查询教师提交的成绩修改申请

### `PUT /api/admin/score-change-requests/{scoreChangeRequestId}/review`
用途：审核成绩修改申请；通过后将对应成绩退回为可修改状态

请求参数：
```json
{
  "action": "APPROVE",
  "reviewComment": "同意修改"
}
```

### `GET /api/admin/schedule-adjust-requests`
用途：查询教师调课申请列表

### `PUT /api/admin/schedule-adjust-requests/{adjustRequestId}/review`
用途：审核教师调课申请；通过后同步更新教学班安排

请求参数：
```json
{
  "action": "APPROVE",
  "reviewComment": "同意调整"
}
```

## 3. 学生模块

### `GET /api/student/courses/available`
用途：查看当前学期可选课程

查询参数：
- `semesterId`
- `courseName`

返回补充字段：
- `canSelectDirectly`：是否可以直接选课
- `canRequestSelection`：是否可以提交补选申请
- `alreadySelected`：当前学生是否已选该教学班
- `hasPendingSelectionRequest`：当前学生是否已有待审核补选申请

说明：
- 正常可直接选课时，补选申请按钮应禁用
- 不可直接选课但允许补选时，直接选课按钮应禁用

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
- 如果学生已退选过同一教学班，再次选课会恢复原 `course_selection` 记录为 `已选`，避免唯一键冲突
- 恢复前会清理该学生在该教学班的旧成绩，避免退课前成绩继续保留

### `DELETE /api/student/course-selections/{teachingClassId}`
用途：学生退课

说明：
- 退课会将选课记录置为 `已退`
- 退课会清理该学生在该教学班的成绩记录

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

### `GET /api/teacher/teaching-classes/{teachingClassId}`
用途：查看本人教学班详情

### `GET /api/teacher/teaching-classes/{teachingClassId}/students`
用途：查看教学班学生名单

### `GET /api/teacher/profile`
用途：查看教师个人信息

### `PUT /api/teacher/profile`
用途：维护教师本人手机号和邮箱

### `GET /api/teacher/todos`
用途：查看教师待办统计，包括退课待审核、未录入成绩、未提交成绩、调课待审核数量

返回补充字段：
- `scoreSubmitWindowOpen`：当前教师是否存在处于成绩提交时间窗口内的教学班

说明：
- `missingScores` 和 `unsubmittedScores` 只统计处于成绩提交时间窗口内的教学班。
- 不在成绩提交时间窗口内时，首页成绩录入待办显示为 `--`，鼠标悬停提示当前不在相关事务开放时间内。

### `GET /api/teacher/timetable`
用途：查看本人课表

说明：
- 教师授课管理页可基于本人授课列表生成接近页面课表样式的打印页，并通过浏览器打印导出 PDF。
- 导出数据来源于教学班列表中的学期、星期、节次、周次、课程和教室信息。

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
- `isSubmitted = 1` 时会校验当前时间是否在管理员设置的成绩提交时间窗口内

### `POST /api/teacher/scores/batch`
用途：批量暂存或批量提交成绩

### `GET /api/teacher/scores`
用途：查看成绩列表

查询参数：
- `teachingClassId`
- `studentId`

### `GET /api/teacher/scores/submit-window/{teachingClassId}`
用途：查看指定教学班所在学期的成绩提交时间窗口

### `GET /api/teacher/score-change-requests`
用途：查询本人提交的成绩修改申请

### `POST /api/teacher/score-change-requests`
用途：对已提交成绩发起修改申请

请求参数：
```json
{
  "scoreId": 1,
  "requestReason": "录入时发现原始试卷核分有误"
}
```

说明：
- 只能申请修改本人授课教学班中的已提交成绩
- 同一成绩存在待审核申请时不能重复提交
- 管理员审核通过后，该成绩会变为未提交状态，教师可重新修改并提交

### `GET /api/teacher/available-classroom-options`
用途：查询某教学班在指定星期和节次下可用于调课的空教室

查询参数：
- `teachingClassId`
- `weekDay`
- `sectionDesc`

说明：
- 空教室由 `teaching_class` 中同学期、同星期、节次重叠的教室占用记录排除得到
- 当前教学班原教室不作为可选目标教室返回

### `GET /api/teacher/schedule-adjust-requests`
用途：查询本人调课申请

### `POST /api/teacher/schedule-adjust-requests`
用途：提交调课申请

请求参数：
```json
{
  "teachingClassId": 1,
  "requestedWeekDay": 3,
  "requestedSectionDesc": "1-4节",
  "requestedWeekRange": "1-16周",
  "requestedClassroomId": "R101",
  "requestReason": "实验室设备维护"
}
```

说明：
- 周次必须与原教学班周次一致，不允许调整
- 节次跨度必须与原节次一致
- 教室应来自空教室选项接口

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
