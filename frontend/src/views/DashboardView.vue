<template>
  <section class="dashboard-page">
    <div class="hero-card">
      <div>
        <span class="hero-tag">{{ roleLabel }}</span>
        <h1>欢迎回来，{{ userStore.realName || '用户' }}</h1>
        <p>{{ description }}</p>
      </div>
    </div>

    <div v-if="userStore.isDefaultPassword" class="password-warning">
      <div>
        <strong>当前账号仍在使用默认密码</strong>
        <p>为保护账号安全，请尽快修改默认密码。</p>
      </div>
      <RouterLink to="/password">
        <el-button type="warning">立即修改</el-button>
      </RouterLink>
    </div>

    <div class="card-grid">
      <article class="dashboard-card">
        <h2>当前账号</h2>
        <p>用户名：{{ userStore.username }}</p>
        <p>角色：{{ roleLabel }}</p>
        <p v-if="userStore.refId">关联编号：{{ userStore.refId }}</p>
      </article>

      <article class="dashboard-card">
        <h2>建议操作</h2>
        <ul class="plain-list">
          <li v-for="item in actionList" :key="item">{{ item }}</li>
        </ul>
      </article>

      <article v-if="userStore.roleCode === 'TEACHER'" class="dashboard-card span-2">
        <div class="todo-header">
          <h2>教学待办</h2>
          <el-button size="small" @click="loadTeacherTodos">刷新</el-button>
        </div>
        <div v-loading="todoLoading" class="todo-grid">
          <div class="todo-item">
            <span>退课待审核</span>
            <strong>{{ teacherTodos.pendingDropRequests || 0 }}</strong>
          </div>
          <el-tooltip
            :disabled="scoreSubmitWindowOpen"
            content="当前不在相关事务的开放时间内"
            placement="top"
          >
            <div class="todo-item">
              <span>未录入成绩</span>
              <strong>{{ scoreSubmitWindowOpen ? teacherTodos.missingScores || 0 : '--' }}</strong>
            </div>
          </el-tooltip>
          <el-tooltip
            :disabled="scoreSubmitWindowOpen"
            content="当前不在相关事务的开放时间内"
            placement="top"
          >
            <div class="todo-item">
              <span>未提交成绩</span>
              <strong>{{ scoreSubmitWindowOpen ? teacherTodos.unsubmittedScores || 0 : '--' }}</strong>
            </div>
          </el-tooltip>
          <div class="todo-item">
            <span>调课待审核</span>
            <strong>{{ teacherTodos.pendingScheduleAdjustRequests || 0 }}</strong>
          </div>
        </div>
      </article>

      <article class="dashboard-card span-2">
        <h2>系统说明</h2>
        <p>
          本系统支持管理员、教师、学生三类角色。当前页面会根据登录身份显示不同入口，避免看到不属于本角色的功能。
        </p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getTeacherTodos } from '../api/teacher'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const todoLoading = ref(false)
const teacherTodos = ref({})

const scoreSubmitWindowOpen = computed(() => Number(teacherTodos.value.scoreSubmitWindowOpen || 0) > 0)

const scoreTodoTotal = computed(() => {
  return Number(teacherTodos.value.missingScores || 0) + Number(teacherTodos.value.unsubmittedScores || 0)
})

const roleLabel = computed(() => {
  const roleMap = {
    ADMIN: '系统管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  }
  return roleMap[userStore.roleCode] || '未识别角色'
})

const description = computed(() => {
  const map = {
    ADMIN: '你可以维护学生、教师、课程信息，并设置选课时间与查看统计分析。',
    TEACHER: '你可以查看授课安排，并为已选课学生录入和提交成绩。',
    STUDENT: '你可以查看可选课程、进行选课，并查询个人课表与成绩。'
  }
  return map[userStore.roleCode] || '请选择对应功能开始操作。'
})

const actionList = computed(() => {
  const map = {
    ADMIN: ['检查当前学期选课时间', '维护学生与教师信息', '查看课程与选课统计'],
    TEACHER: ['查看本学期授课列表', '检查学生选课名单', '录入并提交课程成绩'],
    STUDENT: ['查看当前学期可选课程', '完成在线选课', '查看个人课表与成绩']
  }
  return map[userStore.roleCode] || ['查看系统信息']
})

async function loadTeacherTodos() {
  if (userStore.roleCode !== 'TEACHER') {
    return
  }
  todoLoading.value = true
  try {
    const response = await getTeacherTodos()
    teacherTodos.value = response.data || {}
    showScoreSubmitReminder()
  } finally {
    todoLoading.value = false
  }
}

function showScoreSubmitReminder() {
  if (!scoreSubmitWindowOpen.value) {
    return
  }
  const reminderKey = `score-submit-reminder-${userStore.userId}-${userStore.loginAt || 'current'}`
  if (sessionStorage.getItem(reminderKey)) {
    return
  }
  sessionStorage.setItem(reminderKey, '1')
  const missingScores = Number(teacherTodos.value.missingScores || 0)
  const unsubmittedScores = Number(teacherTodos.value.unsubmittedScores || 0)
  const message = scoreTodoTotal.value > 0
    ? `当前处于成绩录入时间范围内。未录入成绩 ${missingScores} 条，未提交成绩 ${unsubmittedScores} 条，请及时处理。`
    : '当前处于成绩录入时间范围内，暂无未录入或未提交成绩。'
  ElMessageBox.alert(message, '成绩录入提醒', {
    type: 'warning',
    confirmButtonText: '知道了'
  })
}

onMounted(() => {
  loadTeacherTodos()
})
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 20px;
}

.hero-card {
  padding: 28px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(224, 247, 255, 0.24), transparent 30%),
    linear-gradient(135deg, #075985 0%, #0369a1 54%, #0284c7 100%);
  color: #fff;
  box-shadow: 0 22px 55px rgba(3, 105, 161, 0.26);
}

.hero-card h1 {
  margin: 10px 0 12px;
  font-size: 32px;
}

.hero-card p {
  margin: 0;
  color: rgba(255, 255, 255, 0.84);
}

.hero-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 13px;
}

.password-warning {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 8px;
  color: #7c2d12;
  background: #fff7ed;
  border: 1px solid #fed7aa;
}

.password-warning p {
  margin: 6px 0 0;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.dashboard-card {
  padding: 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(125, 211, 252, 0.28);
  box-shadow: var(--card-shadow);
}

.dashboard-card h2 {
  margin-top: 0;
}

.todo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.todo-item {
  padding: 16px;
  border-radius: 8px;
  background: #f8fbfd;
  border: 1px solid #c8ecff;
}

.todo-item span {
  display: block;
  color: var(--text-muted);
  font-size: 13px;
}

.todo-item strong {
  display: block;
  margin-top: 8px;
  color: var(--sky-700);
  font-size: 24px;
}

.plain-list {
  margin: 0;
  padding-left: 18px;
}

.plain-list li + li {
  margin-top: 8px;
}

.span-2 {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  .card-grid,
  .todo-grid {
    grid-template-columns: 1fr;
  }
}
</style>
