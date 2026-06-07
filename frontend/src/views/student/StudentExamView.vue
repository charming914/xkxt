<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>考试安排</h1>
        <p>查看本学期已选课程的考试时间与地点。</p>
      </div>
      <el-button @click="loadExams">刷新</el-button>
    </div>

    <el-empty v-if="!loading && exams.length === 0" description="本学期暂无考试安排" />
    <el-table v-else v-loading="loading" :data="exams" border>
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="courseType" label="类型" width="100" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="teacherName" label="任课教师" width="110" />
      <el-table-column label="考试时间" min-width="210">
        <template #default="{ row }">
          {{ formatDate(row.examDate) }} {{ formatTimeRange(row.startTime, row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="考试地点" min-width="160">
        <template #default="{ row }">
          {{ row.buildingName ? `${row.buildingName} ${row.roomName || ''}` : '未指定' }}
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getStudentExams } from '../../api/student'

const loading = ref(false)
const exams = ref([])

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function formatTimeRange(start, end) {
  if (!start || !end) return ''
  return `${String(start).slice(0, 5)}-${String(end).slice(0, 5)}`
}

async function loadExams() {
  loading.value = true
  try {
    const response = await getStudentExams()
    exams.value = response.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadExams()
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
</style>
