<template>
  <div class="layout-shell">
    <header class="topbar">
      <div class="brand-area">
        <img class="brand-mark" :src="shuLogo" alt="上海大学校徽" />
        <div>
          <h2>教务系统</h2>
          <p>Teaching Affairs</p>
        </div>
      </div>
      <nav class="top-nav">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path">
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="user-panel">
        <div>
          <div class="user-name">{{ userStore.realName || '未登录' }}</div>
          <div class="user-role">{{ roleLabel }}</div>
        </div>
        <el-button class="logout-btn" plain @click="handleLogout">退出登录</el-button>
      </div>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import shuLogo from '../assets/shu-logo.png'

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

const navItems = computed(() => {
  const items = [
    { path: '/', label: '首页' },
    { path: '/password', label: '修改密码' }
  ]
  if (canViewStudent.value) {
    items.push(
      { path: '/student/courses', label: '学生选课' },
      { path: '/student/selected-courses', label: '已选课程' },
      { path: '/student/drop-courses', label: '退课' },
      { path: '/student/timetable', label: '学生课表' },
      { path: '/student/scores', label: '学生成绩' }
    )
  }
  if (canViewTeacher.value) {
    items.push(
      { path: '/teacher/profile', label: '个人信息' },
      { path: '/teacher/classes', label: '教师授课' },
      { path: '/teacher/scores', label: '学生成绩录入' },
      { path: '/teacher/course-drop-requests', label: '退课审核' },
      { path: '/teacher/schedule-adjust-requests', label: '调课申请' }
    )
  }
  if (canViewAdmin.value) {
    items.push(
      { path: '/admin/students', label: '学生管理' },
      { path: '/admin/teachers', label: '教师管理' },
      { path: '/admin/courses', label: '课程管理' },
      { path: '/admin/teaching-classes', label: '教学班管理' },
      { path: '/admin/course-selection-requests', label: '补选审核' },
      { path: '/admin/course-drop-requests', label: '退课审核' },
      { path: '/admin/schedule-adjust-requests', label: '调课审核' },
      { path: '/admin/score-change-requests', label: '成绩修改审核' },
      { path: '/admin/score-review-requests', label: '成绩复核审核' },
      { path: '/admin/statistics', label: '统计分析' },
      { path: '/admin/score-analysis', label: '成绩分析' },
      { path: '/admin/operation-logs', label: '操作日志' }
    )
  }
  return items
})

function handleLogout() {
  userStore.clearUser()
  router.push('/login')
}
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 0%, rgba(2, 132, 199, 0.18), transparent 32%),
    linear-gradient(180deg, #dff2ff 0%, #f7fcff 48%, #e8f6ff 100%);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 84px;
  padding: 16px 28px;
  color: #fff;
  background:
    radial-gradient(circle at 0% 0%, rgba(255, 255, 255, 0.24), transparent 30%),
    linear-gradient(135deg, #075985 0%, #0369a1 48%, #0284c7 100%);
  box-shadow: 0 16px 38px rgba(3, 105, 161, 0.3);
}

.brand-area {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
}

.brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: contain;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 22px rgba(3, 36, 63, 0.18);
}

.brand-area h2 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0.08em;
}

.brand-area p {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
}

.top-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  padding: 6px;
  scrollbar-width: thin;
}

.top-nav a {
  flex: 0 0 auto;
  padding: 9px 13px;
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  font-size: 14px;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.top-nav a:hover,
.top-nav a.router-link-active {
  color: #075985;
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
  padding: 10px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.2);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.22);
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.user-role {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.78);
}

.logout-btn {
  --el-button-bg-color: rgba(255, 255, 255, 0.92);
  --el-button-border-color: rgba(255, 255, 255, 0.7);
  --el-button-text-color: #075985;
}

.content {
  padding: 28px;
}

@media (max-width: 960px) {
  .topbar {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }

  .top-nav {
    width: 100%;
  }

  .user-panel {
    justify-content: space-between;
  }
}

@media (max-width: 640px) {
  .topbar {
    padding: 14px 16px;
  }

  .content {
    padding: 18px 14px;
  }
}
</style>
