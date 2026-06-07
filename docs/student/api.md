# 学生端接口说明

## 通用约定

基础路径：

```text
/api/student
```

请求头：

```text
X-Username: 登录账号
```

身份解析：

```text
后端使用 X-Username 查询 sys_user.ref_id。
ref_id 即 student.student_id。
请求体不传 studentId。
```

统一响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 课程

### 查询可选课程

```text
GET /api/student/courses/available
```

查询参数：

```text
teacherName
courseName
courseId
courseType
credit
weekDay
sectionDesc
```

规则：

```text
只返回当前学期、选课中的教学班。
排除当前学生已选教学班。
```

### 直接选课

```text
POST /api/student/course-selections
```

请求体：

```json
{
  "teachingClassId": 1
}
```

规则：

```text
后端调用 sp_select_course。
存储过程负责选课时间、容量、重复选课、时间冲突等校验。
```

### 查询已选课程

```text
GET /api/student/selected-courses
GET /api/student/course-selections
```

返回字段包含：

```text
selectionId
selectionTime
selectionStatus
teachingClassId
semesterName
courseId
courseName
courseType
credit
teacherName
weekDay
sectionDesc
weekRange
buildingName
roomName
```

### 提交补选申请

```text
POST /api/student/course-selection-requests
```

请求体：

```json
{
  "teachingClassId": 1,
  "requestReason": "说明补选原因"
}
```

### 查询补选申请

```text
GET /api/student/course-selection-requests
```

## 退课

### 查询可退课程

```text
GET /api/student/drop-courses
```

返回关键字段：

```text
dropMode
dropModeLabel
canDropDirectly
canRequestDrop
hasPendingDropRequest
courseStartDate
```

退课模式：

```text
DIRECT：可直接退课。
REQUEST：需提交退课申请。
FORBIDDEN：不可退课。
```

### 直接退课

```text
DELETE /api/student/course-selections/{teachingClassId}
```

### 提交退课申请

```text
POST /api/student/course-drop-requests
POST /api/student/drop-requests
```

请求体：

```json
{
  "teachingClassId": 1,
  "requestReason": "说明退课原因"
}
```

### 查询退课申请

```text
GET /api/student/course-drop-requests
GET /api/student/drop-requests
```

## 课表

### 查询个人课表

```text
GET /api/student/timetable
```

返回字段：

```text
studentId
studentName
semesterName
courseName
teacherName
weekDay
sectionDesc
weekRange
buildingName
roomName
```

## 成绩

### 查询个人成绩

```text
GET /api/student/scores
```

规则：

```text
只返回当前学生成绩。
只返回 is_submitted=1 的已提交成绩。
返回最新一条成绩复核申请状态。
若 score_review_request 表缺失，后端降级为无复核信息的成绩查询。
```

返回关键字段：

```text
scoreId
studentId
studentName
semesterName
teachingClassId
courseId
courseName
courseType
credit
teacherName
usualScore
examScore
finalScore
gpaPoint
gradeLevel
isSubmitted
scoreReviewRequestId
scoreReviewStatus
hasPendingScoreReview
```

### 提交成绩复核申请

```text
POST /api/student/score-review-requests
```

请求体：

```json
{
  "scoreId": 1,
  "requestReason": "说明需要复核的原因"
}
```

规则：

```text
只能复核本人已提交成绩。
同一成绩存在待审核复核申请时，不能重复提交。
```

### 查询成绩复核申请

```text
GET /api/student/score-review-requests
```

## 只读接口验证示例

```powershell
$headers = @{ 'X-Username' = '20240002' }
Invoke-RestMethod -Uri 'http://localhost:8080/api/student/courses/available' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8080/api/student/selected-courses' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8080/api/student/timetable' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8080/api/student/scores' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8080/api/student/drop-courses' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8080/api/student/score-review-requests' -Headers $headers
```
