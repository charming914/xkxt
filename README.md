# 教学事务管理系统项目骨架

当前目录已经按前后端分离方式完成基础分类：

## 目录结构

```text
.
├─ backend        Spring Boot 后端
├─ frontend       Vue + Vite 前端
├─ sql            数据库建库脚本
└─ docs           相关文档
```

## 后端说明

- 技术栈：`Spring Boot 3`、`MyBatis-Plus`、`MySQL`
- 启动类：`com.school.TeachingAffairsApplication`
- 接口分层：
  - `auth`
  - `admin`
  - `teacher`
  - `student`
  - `common`

## 前端说明

- 技术栈：`Vue 3`、`Vite`、`Vue Router`、`Pinia`、`Element Plus`
- 目录分层：
  - `layouts`
  - `router`
  - `views/admin`
  - `views/teacher`
  - `views/student`
  - `styles`

## 启动方式

### 1. 数据库

先执行：(不执行用了，已经放在rds里了)

只用在vs里重新创建一个链接，主机名：124.70.200.70，密码填一下就行

- `school.sql`

### 2. 后端

先装java，项目Java version是21.0.8，去官网跟着操作安装，配置环境

再装Maven：[下载直链](https://dlcdn.apache.org/maven/maven-3/3.9.15/binaries/apache-maven-3.9.15-bin.zip)直接下载下来解压到一个地方就行，然后配置环境

最后打开个终端输入`mvn -v`，出现下面文字说明装好了

```cmd
Maven home: D:\apache-maven-3.9.15
Java version: 21.0.8, vendor: Oracle Corporation, runtime: C:\Program Files\Java\jdk-21
Default locale: zh_SG, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
```

进入 `backend` 目录后运行：

```bash
mvn spring-boot:run
```

默认端口：
- `8080`

后端环境比较好安装

### 3. 前端

**需要安装的环境**

- Node.js
  建议装 LTS 版本，带 npm
- npm
  一般跟着 Node 一起装
- 剩下的npm install会自己装

进入 `frontend` 目录后运行：

```bash
npm install
npm run dev
```

默认端口：
- `5173`
