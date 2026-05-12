# 教学事务管理系统

本项目是一个基于 B/S 架构的教学事务管理系统，支持 `系统管理员`、`教师`、`学生` 三类角色，包含选课、课表、成绩录入、统计分析、基础信息维护等功能。

## 项目结构

```text
.
├─ backend    Spring Boot 后端
├─ frontend   Vue 3 + Vite 前端
├─ sql        数据库脚本与设计文档
└─ docs       其他文档
```

## 技术栈

后端：
- `Java 21`
- `Spring Boot 3`
- `MyBatis-Plus`
- `MySQL`
- `Maven`

前端：
- `Node.js`
- `Vue 3`
- `Vite`
- `Vue Router`
- `Pinia`
- `Element Plus`

## 主要功能

管理员：
- 学生管理：查询、新增、编辑、删除
- 教师管理：查询、新增、编辑、删除
- 课程管理：查询、新增、编辑、删除
- 选课时间设置：开启或关闭选课、设置选课起止时间
- 统计分析：查看教学班数量、选课人数、容量使用率

教师：
- 查看授课列表
- 查看已选课学生
- 录入和提交成绩

学生：
- 查看可选课程
- 在线选课
- 查看个人课表
- 查看个人成绩

## 运行前准备

### 1. 数据库

项目默认使用 MySQL。

当前数据库可以有两种使用方式：

1. 使用现成云数据库
2. 本地自行创建 MySQL 数据库并导入脚本

如果使用云数据库，请在数据库客户端或 IDE 中新建连接，按实际分发的信息填写：
- 主机名
- 端口
- 用户名
- 密码

如果使用本地数据库：
- 先安装 MySQL 8.x
- 创建数据库连接
- 执行 [sql/school.sql](../xkxt/sql/school.sql)

补充文件：
- [sql/school_queries.sql](../xkxt/sql/school_queries.sql)：常用测试 SQL
- [sql/api_design.md](../xkxt/sql/api_design.md)：接口设计说明
- [sql/entity_design.md](../xkxt/sql/entity_design.md)：实体结构说明

### 2. Java 和 Maven 安装

后端运行需要先安装：
- `JDK 21`(详细版本见下)
- `Maven 3.9.x`(详细版本见下)

#### 安装 Java

1. 去 Oracle 或 OpenJDK 官网下载安装 `JDK 21`
2. 安装完成后配置环境变量：

系统变量：
```text
JAVA_HOME = C:\Program Files\Java\jdk-21
```

在 `Path` 中加入：
```text
%JAVA_HOME%\bin
```

3. 打开新的 PowerShell 或 CMD，执行：

```powershell
java -version
```

如果看到类似输出，说明安装成功：

```text
java version "21.0.8"
```

#### 安装 Maven

1. 下载 Maven 3.9.x 二进制压缩包
2. 解压到本地目录，例如：

```text
D:\apache-maven-3.9.15
```

3. 配置环境变量：

系统变量：
```text
MAVEN_HOME = D:\apache-maven-3.9.15
```

在 `Path` 中加入：
```text
%MAVEN_HOME%\bin
```

4. 打开新的 PowerShell 或 CMD，执行：

```powershell
mvn -v
```

如果看到类似输出，说明 Maven 安装成功：

```text
Maven home: D:\apache-maven-3.9.15
Java version: 21.0.8
```

### 3. Node.js 和 npm 安装

前端运行需要先安装：
- `Node.js v22.20.0`
- `npm v10.9.3`

建议安装 `Node.js LTS` 版本，安装 Node 时会自动安装 npm。

安装完成后，在 PowerShell 或 CMD 中执行：

```powershell
node -v
npm -v
```

如果都能输出版本号，说明前端运行环境安装成功。

## 后端启动说明

### 1. 修改数据库配置

打开后端配置文件：

[backend/src/main/resources/application.yml](D:/xkxt/backend/src/main/resources/application.yml)

根据自己的数据库修改以下内容：

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/school?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
```

如果使用云数据库，就把 `url`、`username`、`password` 改成对应信息。

### 2. 启动后端

在项目根目录打开终端，执行：

```powershell
cd ..\xkxt\backend
mvn spring-boot:run
```

默认端口：
```text
8080
```

如果启动时报：
```text
Port 8080 was already in use
```

大概率是之前的终端没关闭，页可以修改 [application.yml](D:/xkxt/backend/src/main/resources/application.yml)：

```yml
server:
  port: 8081
```

### 3. 验证后端是否启动成功（可忽略）

浏览器或 Postman 访问：

```text
http://localhost:8080/api/auth/profile
```

如果你改了端口，就把 `8080` 替换成你实际端口。

## 前端启动说明

### 1. 安装依赖

在项目根目录打开终端，执行：

```powershell
cd ..\xkxt\frontend
npm install
```

如果你之前遇到过 npm 缓存权限问题，可以使用：

```powershell
npm install --cache C:\Users\你的用户名\.npm-cache
```

### 2. 启动前端

执行：

```powershell
npm run dev
```

默认端口：
```text
5173
```

启动后浏览器访问：

```text
http://localhost:5173
```

### 3. 前端代理配置

前端通过 Vite 代理调用后端接口，配置文件在：

[frontend/vite.config.js](..xkxt/frontend/vite.config.js)

默认代理到：

```js
target: 'http://localhost:8080'
```

如果后端改成了 `8081`，这里也要一起改成：

```js
target: 'http://localhost:8081'
```

改完后重新执行：

```powershell
npm run dev
```

## 系统测试账号

默认测试账号如下：

管理员：
- 账号：`admin`
- 密码：`123456`

学生：
- 账号：`20240001`
- 密码：`123456`

教师：
- 账号：`T0101`
- 密码：`123456`

## 密码加密说明

系统用户密码已改为加密存储，后端使用 `BCrypt` 进行密码加密与校验。

说明：
- 数据库中的 `sys_user.password` 不再以明文保存
- 管理员新增学生或教师账号时，默认密码 `123456` 会以加密形式写入数据库
- 用户修改密码后，新密码也会以加密形式保存
- 为兼容旧数据，系统启动时会自动将历史明文密码升级为 BCrypt 密文

## 选课开启方式

学生能否成功选课，同时受两个条件控制：
- 当前时间必须在选课时间范围内
- 当前学期教学班状态必须是 `选课中`

推荐通过管理员页面设置：

1. 管理员登录
2. 打开 `统计分析`
3. 在顶部 `选课设置` 区域中：
   - 设置选课开始时间
   - 设置选课结束时间
   - 打开“开启/关闭选课”开关
4. 点击“保存设置”

然后学生就可以在学生端进行选课。

## 角色权限说明

学生：
- 只显示学生端页面
- 看不到教师端和管理员端页面

教师：
- 只显示教师端页面
- 看不到学生端和管理员端页面

管理员：
- 只显示管理员端页面
- 可以管理基础数据和选课设置

## 常用启动命令

后端启动：

```powershell
cd D:\xkxt\backend
mvn spring-boot:run
```

前端安装依赖：

```powershell
cd D:\xkxt\frontend
npm install
```

前端启动：

```powershell
cd D:\xkxt\frontend
npm run dev
```

## 常见问题

### 1. `mvn` 命令无法识别

原因：
- Maven 没安装
- Maven 没配环境变量

处理：
- 检查 `MAVEN_HOME`
- 检查 `Path` 是否包含 `%MAVEN_HOME%\bin`
- 重新打开终端后执行 `mvn -v`，看是否有相应版本

### 2. `npm install` 报 EPERM

原因：
- npm 缓存目录权限异常

处理：

```powershell
npm install --cache C:\Users\你的用户名\.npm-cache
```

或者执行：

```powershell
npm config set cache "C:\Users\你的用户名\.npm-cache" --global
```

**再或者以管理员权限执行`npm install`**

### 3. 后端启动时报端口被占用

处理方法：
- 修改后端端口
- 或结束占用 `8080` 的进程

## 说明

本项目当前已经完成：
- 三类角色页面隔离
- 学生选课
- 学生课表与成绩查询
- 教师授课与成绩录入
- 管理员基础数据管理
- 管理员选课设置与统计分析

AI建议如果后续继续扩展，可以增加：
- 更完整的分页查询
- 更细致的权限校验
- 管理员教学班管理
- 成绩图表分析
