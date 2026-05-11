# 教学事务管理系统实体结构说明

下面这份说明和 [school.sql](C:/JavaProject/数据库/school.sql) 对应，适合你后面写 Spring Boot 实体类、DTO、VO 和数据库设计说明。

## 1. department
说明：院系表

字段：
- `department_id`：院系编号，主键
- `department_name`：院系名称
- `office_address`：办公地点
- `contact_phone`：联系电话

建议 Java 类名：
- `Department`

## 2. major
说明：专业表

字段：
- `major_id`：专业编号，主键
- `major_name`：专业名称
- `department_id`：所属院系

建议 Java 类名：
- `Major`

## 3. class_info
说明：行政班表

字段：
- `class_id`：班级编号，主键
- `class_name`：班级名称
- `grade_year`：年级
- `major_id`：所属专业
- `counselor_name`：辅导员

建议 Java 类名：
- `ClassInfo`

## 4. student
说明：学生表

字段：
- `student_id`：学号，主键
- `student_name`：姓名
- `gender`：性别
- `birth_date`：出生日期
- `hometown`：籍贯
- `phone`：手机号
- `email`：邮箱
- `enrollment_year`：入学年份
- `department_id`：院系编号
- `major_id`：专业编号
- `class_id`：班级编号
- `status`：学籍状态

建议 Java 类名：
- `Student`

常用 DTO：
- `StudentCreateDTO`
- `StudentUpdateDTO`
- `StudentQueryDTO`

## 5. teacher
说明：教师表

字段：
- `teacher_id`：工号，主键
- `teacher_name`：姓名
- `gender`：性别
- `birth_date`：出生日期
- `title_name`：职称
- `education`：学历
- `phone`：手机号
- `email`：邮箱
- `department_id`：所属院系
- `status`：教师状态

建议 Java 类名：
- `Teacher`

## 6. sys_role
说明：角色表

字段：
- `role_id`：角色编号，主键
- `role_code`：角色编码，如 `ADMIN`
- `role_name`：角色名称

建议 Java 类名：
- `SysRole`

## 7. sys_user
说明：系统用户表

字段：
- `user_id`：用户编号，主键
- `username`：登录账号
- `password`：登录密码
- `real_name`：真实姓名
- `user_type`：用户类型
- `ref_id`：关联的学生编号或教师编号
- `is_enabled`：是否启用
- `created_at`：创建时间

建议 Java 类名：
- `SysUser`

说明：
- 管理员账号可以不关联 `ref_id`
- 学生账号的 `ref_id = student.student_id`
- 教师账号的 `ref_id = teacher.teacher_id`

## 8. sys_user_role
说明：用户角色关联表

字段：
- `user_id`：用户编号，联合主键
- `role_id`：角色编号，联合主键

建议 Java 类名：
- `SysUserRole`

## 9. semester
说明：学期表

字段：
- `semester_id`：学期编号，主键
- `semester_name`：学期名称
- `school_year`：学年
- `term_code`：学期代码
- `start_date`：开始日期
- `end_date`：结束日期
- `course_select_start`：选课开始时间
- `course_select_end`：选课结束时间
- `is_current`：是否当前学期

建议 Java 类名：
- `Semester`

## 10. course
说明：课程表

字段：
- `course_id`：课程编号，主键
- `course_name`：课程名称
- `course_type`：课程类型
- `credit`：学分
- `total_hours`：总学时
- `assessment_method`：考核方式
- `department_id`：开课院系
- `is_required`：是否必修
- `description`：课程说明

建议 Java 类名：
- `Course`

## 11. classroom
说明：教室表

字段：
- `classroom_id`：教室编号，主键
- `building_name`：教学楼
- `room_name`：教室名称
- `capacity`：容量

建议 Java 类名：
- `Classroom`

## 12. teaching_class
说明：教学班表，是本系统核心实体之一

字段：
- `teaching_class_id`：教学班编号，主键
- `semester_id`：学期编号
- `course_id`：课程编号
- `teacher_id`：任课教师工号
- `classroom_id`：教室编号
- `week_day`：星期几
- `section_desc`：节次
- `week_range`：周次范围
- `max_students`：容量上限
- `selected_count`：已选人数
- `status`：教学班状态

建议 Java 类名：
- `TeachingClass`

常用 DTO：
- `TeachingClassCreateDTO`
- `TeachingClassUpdateDTO`
- `TeachingClassQueryDTO`

## 13. course_selection
说明：选课表

字段：
- `selection_id`：选课记录编号，主键
- `student_id`：学号
- `teaching_class_id`：教学班编号
- `selection_time`：选课时间
- `selection_status`：选课状态

建议 Java 类名：
- `CourseSelection`

说明：
- 这一张表表示“谁选了哪门教学班”
- 学生课表主要从这张表和 `teaching_class` 联查得到

## 14. score
说明：成绩表

字段：
- `score_id`：成绩编号，主键
- `student_id`：学号
- `teaching_class_id`：教学班编号
- `usual_score`：平时成绩
- `exam_score`：考试成绩
- `final_score`：总评成绩
- `gpa_point`：绩点
- `is_submitted`：是否提交
- `entered_at`：录入时间

建议 Java 类名：
- `Score`

说明：
- 录入 `usual_score` 和 `exam_score` 后
- 触发器自动计算 `final_score`、`gpa_point`

常用 DTO：
- `ScoreInputDTO`
- `ScoreUpdateDTO`

## 15. operation_log
说明：操作日志表

字段：
- `log_id`：日志编号，主键
- `user_id`：操作人
- `operation_type`：操作类型
- `operation_content`：操作内容
- `operation_time`：操作时间

建议 Java 类名：
- `OperationLog`

## 16. 视图

### `v_student_timetable`
用途：
- 学生查看个人课表
- 后端可以直接查询该视图返回课表信息

### `v_student_score`
用途：
- 学生查看成绩
- 管理员查看成绩明细

## 17. 建议的 Java 分层结构

实体类：
- `entity`
  - `Department`
  - `Major`
  - `ClassInfo`
  - `Student`
  - `Teacher`
  - `SysUser`
  - `SysRole`
  - `SysUserRole`
  - `Semester`
  - `Course`
  - `Classroom`
  - `TeachingClass`
  - `CourseSelection`
  - `Score`
  - `OperationLog`

数据访问层：
- `mapper` 或 `repository`

业务层：
- `service`

接口层：
- `controller`

传输对象：
- `dto`
- `vo`

## 18. 优先开发的核心实体

如果你先做系统主流程，建议优先写这几个实体：
- `SysUser`
- `Student`
- `Teacher`
- `Semester`
- `Course`
- `TeachingClass`
- `CourseSelection`
- `Score`

这样就能先跑通：
- 登录
- 选课
- 课表查询
- 成绩录入
- 成绩查询
