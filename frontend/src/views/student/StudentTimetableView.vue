<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学生课表</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadTimetable">刷新课表</el-button>
    </div>

    <el-empty v-if="!loading && timetable.length === 0" description="当前暂无课表数据" />

    <el-table v-else v-loading="loading" :data="timetable" border>
      <el-table-column prop="semesterName" label="学期" min-width="130" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="teacherName" label="任课教师" width="120" />
      <el-table-column label="上课时间" min-width="180">
        <template #default="{ row }">
          周{{ weekLabel(row.weekDay) }} / {{ row.sectionDesc }} / {{ row.weekRange }}
        </template>
      </el-table-column>
      <el-table-column label="上课地点" min-width="160">
        <template #default="{ row }">
          {{ row.buildingName || '-' }} {{ row.roomName || '' }}
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getStudentTimetable } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const timetable = ref([])

function weekLabel(weekDay) {
  const map = {
    1: '一',
    2: '二',
    3: '三',
    4: '四',
    5: '五',
    6: '六',
    7: '日'
  }
  return map[weekDay] || weekDay
}

async function loadTimetable() {
  loading.value = true
  try {
    const response = await getStudentTimetable()
    timetable.value = response.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTimetable()
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
