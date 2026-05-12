<template>
  <div class="layout-shell">
    <aside class="sidebar">
      <h2>教务系统</h2>
      <div class="user-panel">
        <div class="user-name">{{ userStore.realName || '未登录' }}</div>
        <div class="user-role">{{ roleLabel }}</div>
      </div>
      <nav>
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/password">修改密码</RouterLink>
        <RouterLink v-if="canViewStudent" to="/student/courses">学生选课</RouterLink>
        <RouterLink v-if="canViewStudent" to="/student/timetable">学生课表</RouterLink>
        <RouterLink v-if="canViewStudent" to="/student/scores">学生成绩</RouterLink>
        <RouterLink v-if="canViewTeacher" to="/teacher/classes">教师授课</RouterLink>
        <RouterLink v-if="canViewTeacher" to="/teacher/scores">教师成绩录入</RouterLink>
        <RouterLink v-if="canViewAdmin" to="/admin/students">学生管理</RouterLink>
        <RouterLink v-if="canViewAdmin" to="/admin/teachers">教师管理</RouterLink>
        <RouterLink v-if="canViewAdmin" to="/admin/courses">课程管理</RouterLink>
        <RouterLink v-if="canViewAdmin" to="/admin/statistics">统计分析</RouterLink>
      </nav>
      <el-button class="logout-btn" plain @click="handleLogout">退出登录</el-button>
    </aside>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const roleLabel = computed(() => {
  const roleMap = {
    ADMIN: '系统管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  }
  return roleMap[userStore.roleCode] || '演示账号'
})

const canViewStudent = computed(() => userStore.roleCode === 'STUDENT')
const canViewTeacher = computed(() => userStore.roleCode === 'TEACHER')
const canViewAdmin = computed(() => userStore.roleCode === 'ADMIN')

function handleLogout() {
  userStore.clearUser()
  router.push('/login')
}
</script>

<style scoped>
.layout-shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  background: linear-gradient(180deg, #f2f7f2 0%, #f8fbfd 100%);
}

.sidebar {
  padding: 24px;
  background: #173f35;
  color: #fff;
  display: flex;
  flex-direction: column;
}

.sidebar nav {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
  flex: 1;
}

.sidebar a {
  color: #d9efe7;
  text-decoration: none;
}

.user-panel {
  margin-top: 16px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
}

.user-name {
  font-size: 16px;
  font-weight: 600;
}

.user-role {
  margin-top: 6px;
  font-size: 13px;
  color: #b5d6cb;
}

.logout-btn {
  margin-top: 20px;
}

.content {
  padding: 24px;
}
</style>
