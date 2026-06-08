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
- `score_submit_start`：成绩提交开始时间
- `score_submit_end`：成绩提交结束时间
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
- `course_start_date`：课程实际开课日期
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
- `(student_id, teaching_class_id)` 有唯一约束
- 退课后记录保留为 `selection_status = '已退'`
- 学生再次选择同一教学班，或管理员补选审核通过同一教学班时，会恢复原记录为 `已选`，而不是重复插入
- 退课和恢复已退选课前会清理对应 `score` 记录，避免旧成绩在重新加入课程后继续显示

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
- 绩点采用 11 档规则：90/85/82/78/75/72/68/66/64/60 分分别对应 4.0/3.7/3.3/3.0/2.7/2.3/2.0/1.7/1.5/1.0，低于 60 为 0.0。
- 页面展示评价符号时与学生成绩视图一致：`4.0/A`、`3.7/A-`、`3.3/B+`、`3.0/B`、`2.7/B-`、`2.3/C+`、`2.0/C`、`1.7/C-`、`1.5/D`、`1.0/D-`、`0.0/F`。
- 学生退课或重新加入曾退选课程时，对应成绩记录会被删除，重新加入后需要重新录入成绩

常用 DTO：
- `ScoreInputDTO`
- `ScoreUpdateDTO`

## 15. course_selection_request
说明：补选申请表

字段：
- `request_id`：补选申请编号，主键
- `student_id`：学号
- `teaching_class_id`：教学班编号
- `request_reason`：申请原因
- `request_status`：申请状态
- `review_comment`：审核备注
- `reviewed_by`：审核管理员
- `reviewed_at`：审核时间
- `created_at`：申请时间

建议 Java 类名：
- `CourseSelectionRequest`

说明：
- 管理员审核通过时，如果学生曾退选同一教学班，系统恢复原 `course_selection` 记录。

## 16. course_drop_request
说明：退课申请表

字段：
- `drop_request_id`：退课申请编号，主键
- `student_id`：学号
- `teaching_class_id`：教学班编号
- `request_reason`：退课原因
- `request_status`：申请状态
- `teacher_status`：教师审核状态
- `teacher_comment`：教师审核备注
- `teacher_reviewed_by`：审核教师工号
- `teacher_reviewed_at`：教师审核时间
- `admin_status`：管理员审核状态
- `admin_comment`：管理员审核备注
- `admin_reviewed_by`：审核管理员用户编号
- `admin_reviewed_at`：管理员审核时间
- `created_at`：申请时间
- `updated_at`：更新时间

建议 Java 类名：
- `CourseDropRequest`

## 17. schedule_adjust_request
说明：调课申请表

字段：
- `adjust_request_id`：调课申请编号，主键
- `teaching_class_id`：教学班编号
- `teacher_id`：申请教师工号
- `original_week_day`：原星期
- `original_section_desc`：原节次
- `original_week_range`：原周次
- `original_classroom_id`：原教室
- `requested_week_day`：申请后星期
- `requested_section_desc`：申请后节次
- `requested_week_range`：申请后周次
- `requested_classroom_id`：申请后教室
- `request_reason`：申请原因
- `request_status`：申请状态
- `review_comment`：审核备注
- `reviewed_by`：审核管理员用户编号
- `reviewed_at`：审核时间
- `created_at`：申请时间
- `updated_at`：更新时间

建议 Java 类名：
- `ScheduleAdjustRequest`

说明：
- 调课申请由教师提交，管理员审核。
- 当前实现不允许调整周次，`requested_week_range` 必须与原周次一致。
- `requested_section_desc` 的节次跨度必须与原节次一致。
- 教室占用不单独建表，来源于 `teaching_class` 的同学期、同星期、节次区间和教室记录；查询空教室时会排除节次重叠的占用。
- 教师提交调课申请和管理员审核通过调课申请时，会检查同一教师在目标星期和节次是否已有其它教学班课程。
- 教师可撤销本人状态为 `待审核` 的调课申请，撤销后 `request_status = '已撤销'`。
- 管理员导出的教室课表同样由 `teaching_class` 的教室、星期、节次、周次和课程安排生成，不新增独立课表实体。

## 18. score_change_request
说明：成绩修改申请表

字段：
- `score_change_request_id`：成绩修改申请编号，主键
- `score_id`：成绩编号
- `teacher_id`：申请教师工号
- `student_id`：学号
- `teaching_class_id`：教学班编号
- `original_usual_score`：原平时成绩
- `original_exam_score`：原考试成绩
- `original_final_score`：原总评成绩
- `original_gpa_point`：原绩点
- `request_reason`：申请原因
- `request_status`：申请状态
- `review_comment`：审核备注
- `reviewed_by`：审核管理员用户编号
- `reviewed_at`：审核时间
- `created_at`：申请时间
- `updated_at`：更新时间

建议 Java 类名：
- `ScoreChangeRequest`

说明：
- 教师只能对本人授课教学班中已提交的成绩发起修改申请。
- 管理员审核通过后，系统将对应成绩退回为未提交状态，教师可重新修改并提交。

## 19. exam_arrangement
说明：考试安排表

字段：
- `exam_id`：考试安排编号，主键
- `teaching_class_id`：教学班编号
- `exam_date`：考试日期
- `start_time`：考试开始时间
- `end_time`：考试结束时间
- `classroom_id`：考试教室
- `exam_status`：考试状态
- `created_at`：创建时间
- `updated_at`：更新时间

建议 Java 类名：
- `ExamArrangement`

说明：
- 每个教学班最多对应一条考试安排，`teaching_class_id` 有唯一约束。
- 考试教室占用来源于 `exam_arrangement` 自身的考试日期、时间段和教室字段，不再单独维护考试占用表。
- 管理员修改考试安排、教师提交考试修改申请和管理员审核考试修改申请时，都会基于考试日期和时间段检查教室占用。
- 同一教师在同一考试时间段不能有两门本人教授课程考试。

## 20. exam_change_request
说明：考试安排修改申请表

字段：
- `exam_change_request_id`：考试修改申请编号，主键
- `exam_id`：考试安排编号
- `teaching_class_id`：教学班编号
- `teacher_id`：申请教师工号
- `original_exam_date`：原考试日期
- `original_start_time`：原开始时间
- `original_end_time`：原结束时间
- `original_classroom_id`：原考试教室
- `requested_exam_date`：申请后考试日期
- `requested_start_time`：申请后开始时间
- `requested_end_time`：申请后结束时间
- `requested_classroom_id`：申请后考试教室
- `request_reason`：申请原因
- `request_status`：申请状态
- `review_comment`：审核备注
- `reviewed_by`：审核管理员用户编号
- `reviewed_at`：审核时间
- `created_at`：申请时间
- `updated_at`：更新时间

建议 Java 类名：
- `ExamChangeRequest`

说明：
- 教师只能对本人授课课程的考试安排发起修改申请。
- 同一考试存在待审核修改申请时不能重复提交。
- `request_status` 可为 `待审核`、`已通过`、`已驳回`、`已撤销`。
- 教师可撤销本人状态为 `待审核` 的考试修改申请。
- 提交申请和管理员审核通过时，系统都会检查目标教室在申请后日期和时间段是否被其它考试占用。
- 提交申请和管理员审核通过时，系统都会检查申请教师在申请后日期和时间段是否已有其它考试。

## 21. operation_log
说明：操作日志表

字段：
- `log_id`：日志编号，主键
- `user_id`：操作人
- `operation_type`：操作类型
- `operation_content`：操作内容
- `operation_time`：操作时间

建议 Java 类名：
- `OperationLog`

## 22. 视图

### `v_student_timetable`
用途：
- 学生查看个人课表
- 后端可以直接查询该视图返回课表信息

### `v_student_score`
用途：
- 学生查看成绩
- 管理员查看成绩明细

## 23. 建议的 Java 分层结构

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
  - `CourseSelectionRequest`
  - `CourseDropRequest`
  - `ScheduleAdjustRequest`
  - `ExamArrangement`
  - `ExamChangeRequest`
  - `ScoreChangeRequest`
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

## 24. 优先开发的核心实体

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
