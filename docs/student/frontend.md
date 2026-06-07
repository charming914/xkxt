# 学生端前端说明

## 请求身份

前端统一在 `frontend/src/api/request.js` 中注入请求头：

```text
X-Username = localStorage.user.username
```

学生端不再注入 `X-Student-Id`。教师端仍会在教师身份下注入 `X-Teacher-Id`。

## API 模块

学生端 API 集中在：

```text
frontend/src/api/student.js
```

当前方法：

```text
getAvailableCourses(params)
selectCourse(data)
getSelectedCourses()
getStudentTimetable()
getStudentScores()
createCourseSelectionRequest(data)
getCourseSelectionRequests()
getDropCourses()
dropCourse(teachingClassId)
createCourseDropRequest(data)
getCourseDropRequests()
createScoreReviewRequest(data)
getScoreReviewRequests()
```

## 路由

学生端路由在：

```text
frontend/src/router/index.js
```

当前学生页面：

```text
/student/courses            学生选课
/student/selected-courses   已选课程
/student/drop-courses       退课
/student/timetable          学生课表
/student/scores             成绩查询
```

这些路由均配置：

```text
meta.roles = ['STUDENT']
```

## 页面说明

### StudentCourseView.vue

功能：

```text
查询可选课程。
按教师、课程名、课程号、课程类型、学分、星期、节次筛选。
直接选课。
提交补选申请。
查看我的补选申请。
```

当前后端已经保证：

```text
可选课程不再包含当前学生已选教学班。
直接选课时，学生身份由后端根据 X-Username 解析。
```

### StudentSelectedCourseView.vue

功能：

```text
查看已选课程列表。
以周一到周日、上午/下午/晚上展示课表格视图。
```

数据来源：

```text
GET /api/student/selected-courses
```

### StudentDropCourseView.vue

功能：

```text
查看当前已选课程。
展示退课模式：直接退课、申请退课、不可退课。
直接退课前弹出确认框。
提交退课申请。
查看我的退课申请。
```

退课模式字段由后端返回：

```text
dropMode
dropModeLabel
canDropDirectly
canRequestDrop
hasPendingDropRequest
```

### StudentTimetableView.vue

功能：

```text
查看当前学生课表。
按周一到周日、上午/下午/晚上展示。
```

数据来源：

```text
GET /api/student/timetable
```

### StudentScoreView.vue

功能：

```text
查询成绩。
按学期、课程、通过状态筛选。
展示平均总评、学期平均绩点、计入学分。
展示成绩等级、绩点、复核状态。
提交成绩复核申请。
查看我的成绩复核申请。
```

当前后端保证：

```text
只返回 is_submitted=1 的已提交成绩。
绩点由数据库触发器按十一档规则计算。
若存在成绩复核记录，会返回最新一条复核状态。
```

## 构建验证

```powershell
cd D:\SHU\xkxt\frontend
npm run build
```

当前已知构建提示：

```text
Vite 大 chunk 警告。
@vueuse/core 第三方依赖 PURE 注释警告。
```

这些提示不影响学生端功能。

## 维护注意

```text
学生页面显示当前学生身份时使用 userStore.realName 和 userStore.refId。
提交学生操作时不要附带 studentId。
申请类操作提交成功后，应刷新对应列表和主数据。
成绩统计应以后端返回的已提交成绩为基础。
```
