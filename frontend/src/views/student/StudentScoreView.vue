<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学生成绩</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadScores">刷新成绩</el-button>
    </div>

    <div class="summary-row">
      <div class="summary-card">
        <span class="summary-label">已出成绩课程</span>
        <strong>{{ scores.length }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">平均总评</span>
        <strong>{{ averageScore }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">平均绩点</span>
        <strong>{{ averageGpa }}</strong>
      </div>
    </div>

    <el-empty v-if="!loading && scores.length === 0" description="当前暂无成绩数据" />

    <el-table v-else v-loading="loading" :data="scores" border>
      <el-table-column prop="semesterName" label="学期" min-width="130" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="usualScore" label="平时成绩" width="110" />
      <el-table-column prop="examScore" label="考试成绩" width="110" />
      <el-table-column prop="finalScore" label="总评成绩" width="110" />
      <el-table-column prop="gpaPoint" label="绩点" width="90" />
      <el-table-column label="成绩状态" width="110">
        <template #default="{ row }">
          <el-tag :type="scoreTagType(row.finalScore)">
            {{ scoreStatus(row.finalScore) }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getStudentScores } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const scores = ref([])

const averageScore = computed(() => {
  if (!scores.value.length) {
    return '0.00'
  }
  const total = scores.value.reduce((sum, item) => sum + Number(item.finalScore || 0), 0)
  return (total / scores.value.length).toFixed(2)
})

const averageGpa = computed(() => {
  if (!scores.value.length) {
    return '0.00'
  }
  const total = scores.value.reduce((sum, item) => sum + Number(item.gpaPoint || 0), 0)
  return (total / scores.value.length).toFixed(2)
})

function scoreStatus(finalScore) {
  return Number(finalScore) >= 60 ? '通过' : '未通过'
}

function scoreTagType(finalScore) {
  return Number(finalScore) >= 60 ? 'success' : 'danger'
}

async function loadScores() {
  loading.value = true
  try {
    const response = await getStudentScores()
    scores.value = response.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadScores()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 18px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #edf7f1 0%, #f8fbfd 100%);
  border: 1px solid #d8e9df;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: #173f35;
}

.summary-label {
  color: #5b6d68;
  font-size: 13px;
}
</style>
