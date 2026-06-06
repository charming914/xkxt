<template>
  <section class="dashboard-page">
    <div class="hero-card">
      <div>
        <span class="hero-tag">{{ roleLabel }}</span>
        <h1>欢迎回来，{{ userStore.realName || '用户' }}</h1>
        <p>{{ description }}</p>
      </div>
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
import { computed } from 'vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

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
</style>
