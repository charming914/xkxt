<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学业进度</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadProgress">刷新进度</el-button>
    </div>

    <el-empty v-if="!loading && progressRows.length === 0" description="暂无培养方案，请管理员导入培养方案表" />

    <template v-else>
      <div class="summary-row">
        <div class="summary-card">
          <span class="summary-label">要求总学分</span>
          <strong>{{ totalRequired }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">已获学分</span>
          <strong>{{ totalEarned }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">剩余学分</span>
          <strong>{{ totalRemaining }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">总体完成度</span>
          <strong>{{ overallPercent }}%</strong>
        </div>
      </div>

      <el-table v-loading="loading" :data="progressRows" border>
        <el-table-column prop="enrollmentYear" label="入学年份" width="110" />
        <el-table-column prop="majorName" label="专业" min-width="140" />
        <el-table-column prop="courseType" label="课程类型" min-width="140" />
        <el-table-column prop="requiredCredit" label="要求学分" width="110" />
        <el-table-column prop="earnedCredit" label="已获学分" width="110" />
        <el-table-column prop="remainingCredit" label="剩余学分" width="110" />
        <el-table-column label="完成度" min-width="220">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress :percentage="progressPercent(row)" :stroke-width="12" />
              <span>{{ progressPercent(row) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" />
      </el-table>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAcademicProgress } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const progressRows = ref([])

const totalRequiredNumber = computed(() => sumBy(progressRows.value, 'requiredCredit'))
const totalEarnedNumber = computed(() => sumBy(progressRows.value, 'earnedCredit'))
const totalRemainingNumber = computed(() => sumBy(progressRows.value, 'remainingCredit'))

const totalRequired = computed(() => totalRequiredNumber.value.toFixed(1))
const totalEarned = computed(() => totalEarnedNumber.value.toFixed(1))
const totalRemaining = computed(() => totalRemainingNumber.value.toFixed(1))
const overallPercent = computed(() => {
  if (!totalRequiredNumber.value) {
    return 0
  }
  return Math.min(100, Math.round(totalEarnedNumber.value / totalRequiredNumber.value * 100))
})

function sumBy(rows, key) {
  return rows.reduce((sum, row) => sum + Number(row[key] || 0), 0)
}

function progressPercent(row) {
  const required = Number(row.requiredCredit || 0)
  if (!required) {
    return 0
  }
  return Math.min(100, Math.round(Number(row.earnedCredit || 0) / required * 100))
}

async function loadProgress() {
  loading.value = true
  try {
    const response = await getAcademicProgress()
    progressRows.value = response.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProgress()
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 18px 20px;
  border: 1px solid #d8e9df;
  border-radius: 8px;
  background: #f8fdff;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  color: var(--sky-700);
  font-size: 26px;
}

.summary-label {
  color: var(--text-muted);
  font-size: 13px;
}

.progress-cell {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 44px;
  align-items: center;
  gap: 10px;
}

@media (max-width: 960px) {
  .summary-row {
    grid-template-columns: 1fr;
  }
}
</style>
