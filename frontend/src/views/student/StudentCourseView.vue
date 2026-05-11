<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学生选课</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadCourses">刷新课程</el-button>
    </div>

    <el-table v-loading="loading" :data="courses" border>
      <el-table-column prop="teachingClassId" label="教学班编号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="teacherName" label="任课教师" width="120" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="totalHours" label="学时" width="80" />
      <el-table-column label="上课时间" min-width="180">
        <template #default="{ row }">
          周{{ weekLabel(row.weekDay) }} / {{ row.sectionDesc }} / {{ row.weekRange }}
        </template>
      </el-table-column>
      <el-table-column label="上课地点" min-width="150">
        <template #default="{ row }">
          {{ row.buildingName || '-' }} {{ row.roomName || '' }}
        </template>
      </el-table-column>
      <el-table-column label="人数" width="120">
        <template #default="{ row }">
          {{ row.selectedCount }}/{{ row.maxStudents }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            :loading="submittingId === row.teachingClassId"
            @click="handleSelect(row)"
          >
            选课
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAvailableCourses, selectCourse } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submittingId = ref(null)
const courses = ref([])

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

async function loadCourses() {
  loading.value = true
  try {
    const response = await getAvailableCourses()
    courses.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function handleSelect(row) {
  if (!userStore.refId) {
    ElMessage.warning('当前未识别学生身份')
    return
  }
  submittingId.value = row.teachingClassId
  try {
    const response = await selectCourse({
      studentId: userStore.refId,
      teachingClassId: row.teachingClassId
    })
    ElMessage.success(response.message || '选课成功')
    await loadCourses()
  } finally {
    submittingId.value = null
  }
}

onMounted(() => {
  loadCourses()
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
