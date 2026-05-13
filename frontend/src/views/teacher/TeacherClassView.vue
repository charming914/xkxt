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

    <div v-else v-loading="loading" class="calendar-shell">
      <div class="calendar-head empty-cell">时间段</div>
      <div v-for="day in weekDays" :key="day.value" class="calendar-head">周{{ day.label }}</div>

      <template v-for="block in timeBlocks" :key="block.key">
        <div class="section-label">
          <strong>{{ block.label }}</strong>
          <span>{{ block.time }}</span>
        </div>
        <div v-for="day in weekDays" :key="`${day.value}-${block.key}`" class="calendar-cell">
          <div
            v-for="course in getClasses(day.value, block.key)"
            :key="`${course.teachingClassId}-${course.courseName}`"
            class="course-card"
          >
            <strong>{{ course.courseName }}</strong>
            <span>{{ course.sectionDesc }}</span>
            <span>教学班：{{ course.teachingClassId }}</span>
            <span>{{ course.weekRange }}</span>
            <span>{{ course.buildingName || '-' }} {{ course.roomName || '' }}</span>
            <span>选课人数：{{ course.selectedCount }}</span>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getTeachingClasses } from '../../api/teacher'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const classes = ref([])
const weekDays = [
  { value: 1, label: '一' },
  { value: 2, label: '二' },
  { value: 3, label: '三' },
  { value: 4, label: '四' },
  { value: 5, label: '五' },
  { value: 6, label: '六' },
  { value: 7, label: '日' }
]
const timeBlocks = [
  { key: 'morning', label: '上午', time: '1-4节' },
  { key: 'afternoon', label: '下午', time: '5-8节' },
  { key: 'evening', label: '晚上', time: '9-12节' }
]

function resolveBlock(sectionDesc) {
  const firstSection = parseInt(String(sectionDesc).split('-')[0], 10)
  if (Number.isNaN(firstSection)) {
    return 'afternoon'
  }
  if (firstSection <= 4) {
    return 'morning'
  }
  if (firstSection <= 8) {
    return 'afternoon'
  }
  return 'evening'
}

function getClasses(weekDay, blockKey) {
  return classes.value.filter(item => item.weekDay === weekDay && resolveBlock(item.sectionDesc) === blockKey)
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

.calendar-shell {
  display: grid;
  grid-template-columns: 110px repeat(7, minmax(0, 1fr));
  border: 1px solid #dce6df;
  border-radius: 18px;
  overflow: hidden;
  background: #fff;
}

.calendar-head,
.section-label,
.calendar-cell {
  min-height: 108px;
  padding: 12px;
  border-right: 1px solid #e7efea;
  border-bottom: 1px solid #e7efea;
}

.calendar-head {
  background: #edf1fb;
  font-weight: 600;
  text-align: center;
}

.empty-cell,
.section-label {
  background: #f8fbfd;
  font-weight: 600;
}

.section-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section-label span {
  color: #6b7b76;
  font-size: 12px;
}

.calendar-cell {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.course-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  border-radius: 14px;
  background: linear-gradient(135deg, #edf3ff 0%, #f8fbfd 100%);
  border: 1px solid #dbe4fb;
  font-size: 13px;
}

.course-card strong {
  color: #173f35;
}
</style>
