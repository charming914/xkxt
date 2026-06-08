<template>
  <div class="layout-shell">
    <header class="topbar">
      <div class="brand-area" role="button" tabindex="0" @click="goHome" @keyup.enter="goHome">
        <img class="brand-mark" :src="shuLogo" alt="上海大学校徽" />
        <div>
          <h2>教务系统</h2>
          <p>Teaching Affairs</p>
        </div>
      </div>
      <nav class="top-nav">
        <template v-for="item in navItems" :key="item.label">
          <el-dropdown
            v-if="item.children"
            trigger="hover"
            popper-class="nav-dropdown-popper"
            :hide-on-click="true"
          >
            <span class="nav-link nav-group" :class="{ 'is-active': isGroupActive(item) }">
              {{ item.label }}
              <span class="caret">▾</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="child in item.children"
                  :key="child.path"
                  :class="{ 'is-active': route.path === child.path }"
                  @click="router.push(child.path)"
                >
                  {{ child.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <RouterLink v-else :to="item.path" class="nav-link">
            {{ item.label }}
          </RouterLink>
        </template>
      </nav>
      <div v-if="showSemesterBadge" class="semester-badge">
        <span>当前学期</span>
        <strong>{{ currentSemester.semesterName || '-' }}</strong>
        <small>{{ semesterDateRange }}</small>
      </div>
      <el-dropdown trigger="hover" popper-class="nav-dropdown-popper">
        <div class="user-panel">
          <div>
            <div class="user-name">{{ userStore.realName || '未登录' }}</div>
            <div class="user-role">{{ roleLabel }}</div>
          </div>
          <span class="caret">▾</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="router.push('/password')">修改密码</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getStudentCurrentSemester } from '../api/student'
import { getTeacherCurrentSemester } from '../api/teacher'
import shuLogo from '../assets/shu-logo.png'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentSemester = ref({})

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
const showSemesterBadge = computed(() => {
  return (canViewTeacher.value || canViewStudent.value) && Boolean(currentSemester.value.semesterName)
})
const semesterDateRange = computed(() => {
  const start = formatDate(currentSemester.value.startDate)
  const end = formatDate(currentSemester.value.endDate)
  if (!start && !end) {
    return '起止时间未设置'
  }
  return `${start || '-'} 至 ${end || '-'}`
})

const navItems = computed(() => {
  const items = []
  if (canViewStudent.value) {
    items.push(
      { path: '/student/profile', label: '个人信息' },
      {
        label: '选课管理',
        children: [
          { path: '/student/courses', label: '学生选课' },
          { path: '/student/course-plans', label: '选课计划' },
          { path: '/student/selected-courses', label: '已选课程' },
          { path: '/student/drop-courses', label: '退课' }
        ]
      },
      {
        label: '学期信息',
        children: [
          { path: '/student/semesters', label: '学期总览' },
          { path: '/student/exams', label: '考试安排' }
        ]
      },
      {
        label: '成绩学业',
        children: [
          { path: '/student/scores', label: '学生成绩' },
          { path: '/student/remediation', label: '补考重修' },
          { path: '/student/academic-progress', label: '学业进度' }
        ]
      }
    )
  }
  if (canViewTeacher.value) {
    items.push(
      { path: '/teacher/profile', label: '个人信息' },
      { path: '/teacher/classes', label: '教师授课' },
      { path: '/teacher/scores', label: '学生成绩录入' },
      { path: '/teacher/exams', label: '考试安排' },
      { path: '/teacher/schedule-adjust-requests', label: '调课申请' },
      {
        label: '审核处理',
        children: [
          { path: '/teacher/course-drop-requests', label: '退课审核' },
          { path: '/teacher/makeup-requests', label: '补考确认' }
        ]
      }
    )
  }
  if (canViewAdmin.value) {
    items.push(
      {
        label: '基础数据',
        children: [
          { path: '/admin/students', label: '学生管理' },
          { path: '/admin/teachers', label: '教师管理' },
          { path: '/admin/courses', label: '课程管理' },
          { path: '/admin/semesters', label: '学期管理' },
          { path: '/admin/teaching-classes', label: '教学班管理' },
          { path: '/admin/exams', label: '考试管理' },
          { path: '/admin/cultivation-plan', label: '培养方案' }
        ]
      },
      {
        label: '审核管理',
        children: [
          { path: '/admin/course-selection-requests', label: '补选审核' },
          { path: '/admin/course-drop-requests', label: '退课审核' },
          { path: '/admin/schedule-adjust-requests', label: '调课审核' },
          { path: '/admin/exam-change-requests', label: '考试修改审核' },
          { path: '/admin/score-change-requests', label: '成绩修改审核' },
          { path: '/admin/score-review-requests', label: '成绩复核审核' },
          { path: '/admin/remediation-requests', label: '补考重修审核' }
        ]
      },
      {
        label: '统计与日志',
        children: [
          { path: '/admin/statistics', label: '统计分析' },
          { path: '/admin/score-analysis', label: '成绩分析' },
          { path: '/admin/operation-logs', label: '操作日志' }
        ]
      }
    )
  }
  return items
})

function isGroupActive(item) {
  return Boolean(item.children?.some((child) => route.path === child.path))
}

function goHome() {
  router.push('/')
}

function handleLogout() {
  userStore.clearUser()
  router.push('/login')
}

function formatDate(value) {
  if (!value) {
    return ''
  }
  return String(value).slice(0, 10)
}

async function loadCurrentSemester() {
  currentSemester.value = {}
  try {
    if (canViewTeacher.value) {
      const response = await getTeacherCurrentSemester()
      currentSemester.value = response.data || {}
      return
    }
    if (canViewStudent.value) {
      const response = await getStudentCurrentSemester()
      currentSemester.value = response.data || {}
    }
  } catch {
    currentSemester.value = {}
  }
}

watch(
  () => userStore.roleCode,
  () => {
    loadCurrentSemester()
  }
)

onMounted(() => {
  loadCurrentSemester()
})
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
  cursor: pointer;
  padding: 6px 10px;
  margin: -6px -10px;
  border-radius: 16px;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.brand-area:hover {
  background: rgba(255, 255, 255, 0.14);
  transform: translateY(-1px);
}

.brand-area:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.7);
  outline-offset: 2px;
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

.semester-badge {
  display: grid;
  gap: 2px;
  flex: 0 0 auto;
  min-width: 180px;
  padding: 9px 12px;
  border-radius: 8px;
  color: #eff8ff;
  background: rgba(255, 255, 255, 0.16);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.22);
}

.semester-badge span {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.74);
}

.semester-badge strong {
  font-size: 13px;
  line-height: 1.2;
}

.semester-badge small {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.78);
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
  padding: 9px 14px;
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  outline: none;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.nav-link:hover,
.nav-link.router-link-active,
.nav-link.is-active {
  color: #075985;
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.caret {
  font-size: 11px;
  opacity: 0.85;
  line-height: 1;
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
  padding: 10px 14px;
  border-radius: 18px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.2);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.22);
  transition: background-color 0.2s ease;
}

.user-panel:hover {
  background: rgba(255, 255, 255, 0.3);
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

<style>
/* 下拉菜单挂载在 body 上，需用全局样式定制选中态 */
.nav-dropdown-popper {
  --el-dropdown-menuItem-hover-fill: #e7f5ff;
  --el-dropdown-menuItem-hover-color: #075985;
}

.nav-dropdown-popper .el-dropdown-menu__item {
  font-size: 14px;
  padding: 9px 18px;
}

.nav-dropdown-popper .el-dropdown-menu__item.is-active {
  color: #075985;
  font-weight: 600;
  background: #e7f5ff;
}
</style>
