# 学生端开发文档

本目录用于集中记录学生端功能、接口、代码位置和后续维护注意事项。

## 文档索引

| 文档 | 内容 |
| --- | --- |
| [backend.md](backend.md) | 学生端后端结构、身份解析、核心 SQL 逻辑 |
| [frontend.md](frontend.md) | 学生端前端页面、路由、API 模块 |
| [api.md](api.md) | 学生端接口清单、请求头、主要字段 |
| [2026-06-07-log.md](2026-06-07-log.md) | 2026-06-07 学生端开发与修复记录 |

## 当前学生端功能

```text
学生选课：查询可选课程、直接选课、提交补选申请。
已选课程：查看已选课程列表和课表格视图。
退课：按开课时间判断直接退课、提交退课申请、查看退课申请记录。
课表：查看当前学生个人课表。
成绩：查看已提交成绩、等级、绩点、平均总评、加权平均绩点。
成绩复核：学生提交复核申请，管理员审核并记录状态和备注。
```

## 核心规则

```text
学生身份统一由请求头 X-Username 解析。
后端通过 sys_user.username -> sys_user.ref_id 得到真实 student_id。
学生请求体不再传 studentId，避免伪造或误用身份。

学生可选课程不展示当前学生已选课程。
学生成绩只展示教师已提交成绩，即 score.is_submitted = 1。
绩点由数据库 score 表触发器计算，当前规则为十一档绩点规则。
成绩复核依赖 score_review_request 表。
```

## 关键代码位置

```text
后端控制器：
backend/src/main/java/com/school/student/controller/StudentController.java

后端服务：
backend/src/main/java/com/school/student/service/StudentService.java

后端 Mapper：
backend/src/main/java/com/school/student/mapper/StudentMapper.java

学生 DTO：
backend/src/main/java/com/school/student/dto/

前端 API：
frontend/src/api/student.js
frontend/src/api/request.js

学生页面：
frontend/src/views/student/

学生路由：
frontend/src/router/index.js
```

## 常用验证命令

```powershell
cd D:\SHU\xkxt\backend
mvn -q -DskipTests compile
```

```powershell
cd D:\SHU\xkxt\frontend
npm run build
```

学生接口只读验证示例：

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/student/scores' -Headers @{ 'X-Username' = '20240002' }
```

## 后续开发建议

```text
新增学生端接口时，优先从 X-Username 解析 student_id，不要让前端传 studentId。
新增成绩相关功能时，注意只使用已提交成绩。
涉及绩点规则时，同步检查数据库触发器、历史数据重算脚本和前端展示。
涉及申请类功能时，建议同时提供学生申请列表和管理员审核列表。
```
