<template>
  <section class="page-card">
    <div class="setting-card">
      <div class="setting-header">
        <div>
          <h2>选课设置</h2>
          <p>管理员可以为当前学期设置选课时间，并开启或关闭选课。</p>
        </div>
        <div class="toolbar">
          <el-button @click="loadSetting">刷新设置</el-button>
          <el-button type="primary" @click="handleSaveSetting">保存设置</el-button>
        </div>
      </div>

      <div class="setting-grid">
        <div class="setting-item">
          <span class="setting-label">当前学期</span>
          <strong>{{ setting.semesterName || '-' }}</strong>
        </div>
        <div class="setting-item">
          <span class="setting-label">选课状态</span>
          <el-switch
            v-model="isCourseSelectionOpen"
            active-text="开启"
            inactive-text="关闭"
            inline-prompt
          />
        </div>
        <div class="setting-item">
          <span class="setting-label">选课开始时间</span>
          <el-date-picker
            v-model="setting.courseSelectStart"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开始时间"
          />
        </div>
        <div class="setting-item">
          <span class="setting-label">选课结束时间</span>
          <el-date-picker
            v-model="setting.courseSelectEnd"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择结束时间"
          />
        </div>
      </div>
    </div>

    <div class="page-header">
      <div>
        <h1>统计分析</h1>
        <p>管理员查看教学班选课情况和容量使用率。</p>
      </div>
      <el-button @click="loadStatistics">刷新统计</el-button>
    </div>

    <div class="summary-row">
      <div class="summary-card">
        <span class="summary-label">教学班数量</span>
        <strong>{{ statistics.length }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">总选课人数</span>
        <strong>{{ totalSelected }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">平均容量使用率</span>
        <strong>{{ averageUsage }}</strong>
      </div>
    </div>

    <el-empty v-if="!loading && statistics.length === 0" description="暂无统计数据" />

    <el-table v-else v-loading="loading" :data="statistics" border>
      <el-table-column prop="semesterName" label="学期" min-width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="teachingClassId" label="教学班编号" width="120" />
      <el-table-column prop="teacherName" label="任课教师" width="120" />
      <el-table-column label="选课人数" width="100">
        <template #default="{ row }">
          {{ row.selectedCount }}
        </template>
      </el-table-column>
      <el-table-column label="容量上限" width="100">
        <template #default="{ row }">
          {{ row.maxStudents }}
        </template>
      </el-table-column>
      <el-table-column label="使用率" width="120">
        <template #default="{ row }">
          {{ usageRate(row) }}
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAdminCourseSelectionStatistics,
  getCourseSelectionSetting,
  updateCourseSelectionSetting
} from '../../api/admin'

const loading = ref(false)
const statistics = ref([])
const setting = reactive({
  semesterName: '',
  courseSelectStart: '',
  courseSelectEnd: '',
  teachingClassStatus: '未开始'
})

const isCourseSelectionOpen = computed({
  get: () => setting.teachingClassStatus === '选课中',
  set: (value) => {
    setting.teachingClassStatus = value ? '选课中' : '未开始'
  }
})

const totalSelected = computed(() => {
  return statistics.value.reduce((sum, item) => sum + Number(item.selectedCount || 0), 0)
})

const averageUsage = computed(() => {
  if (!statistics.value.length) {
    return '0.00%'
  }
  const total = statistics.value.reduce((sum, item) => {
    const max = Number(item.maxStudents || 0)
    const selected = Number(item.selectedCount || 0)
    return sum + (max ? selected / max : 0)
  }, 0)
  return `${((total / statistics.value.length) * 100).toFixed(2)}%`
})

function usageRate(row) {
  const max = Number(row.maxStudents || 0)
  const selected = Number(row.selectedCount || 0)
  if (!max) {
    return '0.00%'
  }
  return `${((selected / max) * 100).toFixed(2)}%`
}

async function loadStatistics() {
  loading.value = true
  try {
    const response = await getAdminCourseSelectionStatistics()
    statistics.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadSetting() {
  const response = await getCourseSelectionSetting()
  Object.assign(setting, response.data || {})
}

async function handleSaveSetting() {
  await updateCourseSelectionSetting({
    courseSelectStart: setting.courseSelectStart,
    courseSelectEnd: setting.courseSelectEnd,
    teachingClassStatus: setting.teachingClassStatus
  })
  ElMessage.success('选课设置更新成功')
  await loadSetting()
}

onMounted(() => {
  loadSetting()
  loadStatistics()
})
</script>

<style scoped>
.setting-card {
  margin-bottom: 24px;
  padding: 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eefaff 0%, #f8fdff 100%);
  border: 1px solid #c8ecff;
}

.setting-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.setting-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.setting-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-label {
  color: var(--text-muted);
  font-size: 13px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  gap: 12px;
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
  background: linear-gradient(135deg, #e7f8ff 0%, #f8fdff 100%);
  border: 1px solid #c8ecff;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: var(--sky-700);
}

.summary-label {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
