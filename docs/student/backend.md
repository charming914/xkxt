# 学生端后端说明

## 身份解析

学生端接口不信任请求体中的学号，统一从请求头解析身份。

```text
请求头：X-Username
解析链路：sys_user.username -> sys_user.ref_id -> student.student_id
入口方法：StudentService.resolveStudentId
```

相关代码：

```text
backend/src/main/java/com/school/student/controller/StudentController.java
backend/src/main/java/com/school/student/service/StudentService.java
backend/src/main/java/com/school/student/mapper/StudentMapper.java
```

## 控制器接口

`StudentController` 统一挂载在：

```text
/api/student
```

当前学生端接口：

```text
GET    /courses/available              查询可选课程
POST   /course-selections              直接选课
DELETE /course-selections/{id}         直接退课
GET    /course-selections              已选课程
GET    /selected-courses               已选课程别名
GET    /drop-courses                   可退课程与退课模式
POST   /course-drop-requests           提交退课申请
POST   /drop-requests                  提交退课申请别名
GET    /course-drop-requests           退课申请记录
GET    /drop-requests                  退课申请记录别名
GET    /timetable                      个人课表
GET    /scores                         个人成绩
POST   /course-selection-requests      提交补选申请
GET    /course-selection-requests      补选申请记录
POST   /score-review-requests          提交成绩复核申请
GET    /score-review-requests          成绩复核申请记录
```

## 服务层规则

`StudentService` 负责身份解析、业务校验和错误提示转换。

```text
availableCourses：
先解析当前学生 student_id，再查询可选课程。
可选课程会排除该学生已选教学班。

selectCourse：
调用数据库存储过程 sp_select_course。
后端传入当前登录学生 student_id 和 teachingClassId。

dropCourse：
先确认当前学生已选该教学班，再执行退课。

createCourseDropRequest：
先确认当前学生已选该教学班，再检查是否已有待审核退课申请。

scores：
查询当前学生已提交成绩。
若 score_review_request 表缺失，会降级到不关联复核表的成绩查询。

createScoreReviewRequest：
只允许对当前学生本人且 is_submitted=1 的成绩提交复核申请。
```

## Mapper 关键 SQL

### 可选课程

`selectAvailableCourses` 当前规则：

```text
只查当前学期：semester.is_current = 1。
只查选课中教学班：teaching_class.status = '选课中'。
排除当前学生已选教学班：NOT EXISTS selection_status='已选'。
支持按教师、课程名、课程号、课程类型、学分、星期、节次筛选。
```

### 成绩查询

`selectScores` 当前规则：

```text
只查当前学生成绩：score.student_id = 当前登录学生。
只展示已提交成绩：score.is_submitted = 1。
关联课程、教师、学期、学生基础信息。
关联最新一条成绩复核申请。
按学期倒序、课程名排序。
```

如果 `score_review_request` 表不存在，`StudentService.scores` 会调用 `selectScoresWithoutReview`。

### 退课模式

`selectDropCourses` 根据 `teaching_class.course_start_date` 判断：

```text
开课日期为空：FORBIDDEN
开课 7 天内：DIRECT
开课超过 7 天但不超过 21 天：REQUEST
超过 21 天：FORBIDDEN
```

同时返回：

```text
dropMode
dropModeLabel
canDropDirectly
canRequestDrop
hasPendingDropRequest
```

## DTO 约定

学生端请求 DTO 不应包含可由前端伪造的 `studentId`。

当前已清理：

```text
SelectCourseRequest：只保留 teachingClassId。
CourseSelectionRequestCreateRequest：只保留 teachingClassId、requestReason。
```

仍需要请求体字段的 DTO：

```text
CourseDropRequestCreateRequest：teachingClassId、requestReason。
ScoreReviewRequestCreateRequest：scoreId、requestReason。
```

## 数据库依赖

成绩复核表：

```text
sql/0607_score_review_request.sql
score_review_request
```

成绩绩点规则：

```text
sql/0605_score_gpa_update.sql
score 表 trg_score_before_insert / trg_score_before_update
```

当前绩点规则为十一档：

```text
>=90 4.0
>=85 3.7
>=82 3.3
>=78 3.0
>=75 2.7
>=72 2.3
>=68 2.0
>=66 1.7
>=64 1.5
>=60 1.0
<60  0.0
```

## 维护注意

```text
新增学生端接口时，先在 StudentService 中解析 student_id。
不要让前端传 studentId 决定操作对象。
成绩类接口默认只面向已提交成绩。
涉及申请表的新功能，应同时考虑缺表时的友好提示或数据库初始化说明。
```
