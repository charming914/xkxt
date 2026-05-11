<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>教师授课管理</h1>
        <p>当前登录教师：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadTeachingClasses">刷新列表</el-button>
    </div>

    <el-empty v-if="!loading && classes.length === 0" description="当前暂无授课数据" />

    <el-table v-else v-loading="loading" :data="classes" border>
      <el-table-column prop="teachingClassId" label="教学班编号" width="120" />
      <el-table-column prop="semesterName" label="学期" min-width="130" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column label="上课时间" min-width="180">
        <template #default="{ row }">
          周{{ weekLabel(row.weekDay) }} / {{ row.sectionDesc }} / {{ row.weekRange }}
        </template>
      </el-table-column>
      <el-table-column prop="selectedCount" label="选课人数" width="100" />
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getTeachingClasses } from '../../api/teacher'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const classes = ref([])

function weekLabel(weekDay) {
  const map = { 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六', 7: '日' }
  return map[weekDay] || weekDay
}

async function loadTeachingClasses() {
  loading.value = true
  try {
    const response = await getTeachingClasses()
    classes.value = response.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTeachingClasses()
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
