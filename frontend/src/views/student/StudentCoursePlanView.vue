<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>选课计划</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadPlans">刷新计划</el-button>
    </div>

    <el-empty v-if="!loading && plans.length === 0" description="暂无选课计划，可在学生选课页加入计划" />

    <el-table v-else v-loading="loading" :data="plans" border>
      <el-table-column prop="semesterName" label="学期" min-width="120" />
      <el-table-column prop="courseId" label="课程号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="courseType" label="课程类型" width="120" />
      <el-table-column prop="teacherName" label="教师" width="110" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column label="时间" min-width="180">
        <template #default="{ row }">
          周{{ weekLabel(row.weekDay) }} / {{ row.sectionDesc }} / {{ row.weekRange }}
        </template>
      </el-table-column>
      <el-table-column label="地点" min-width="130">
        <template #default="{ row }">
          {{ row.buildingName || '-' }} {{ row.roomName || '' }}
        </template>
      </el-table-column>
      <el-table-column label="容量" width="110">
        <template #default="{ row }">
          {{ row.selectedCount }}/{{ row.maxStudents }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="planStatusType(row)">{{ planStatus(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <div class="action-row">
            <el-button size="small" type="primary" :disabled="Number(row.alreadySelected) === 1" @click="handleSelect(row)">
              选课
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">移出</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCoursePlan, getCoursePlans, selectCourse } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const plans = ref([])

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

function planStatus(row) {
  if (Number(row.alreadySelected) === 1) return '已选'
  if (Number(row.isFull) === 1) return '已满'
  return '可尝试'
}

function planStatusType(row) {
  if (Number(row.alreadySelected) === 1) return 'success'
  if (Number(row.isFull) === 1) return 'warning'
  return 'primary'
}

async function loadPlans() {
  loading.value = true
  try {
    const response = await getCoursePlans()
    plans.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function handleSelect(row) {
  const response = await selectCourse({
    studentId: userStore.refId,
    teachingClassId: row.teachingClassId
  })
  ElMessage.success(response.message || '选课成功')
  await loadPlans()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认将 ${row.courseName} 移出选课计划吗？`, '移出计划', {
    type: 'warning',
    confirmButtonText: '移出',
    cancelButtonText: '取消'
  })
  await deleteCoursePlan(row.planId)
  ElMessage.success('已移出选课计划')
  await loadPlans()
}

onMounted(() => {
  loadPlans()
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

.action-row {
  display: flex;
  gap: 8px;
}
</style>
