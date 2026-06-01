<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>已选课程</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadSelectedCourses">刷新</el-button>
    </div>

    <div class="sub-header">
      <h2>现有课表</h2>
    </div>
    <el-empty v-if="!loading && selectedCourses.length === 0" description="当前暂无已选课程" />
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
            v-for="course in getCourses(day.value, block.key)"
            :key="`${course.teachingClassId}-${course.courseId}`"
            class="course-card"
          >
            <strong>{{ course.courseName }}</strong>
            <span>{{ course.sectionDesc }}</span>
            <span>{{ course.teacherName }}</span>
            <span>{{ course.weekRange }}</span>
            <span>{{ course.buildingName || '-' }} {{ course.roomName || '' }}</span>
          </div>
        </div>
      </template>
    </div>

    <div class="selected-section">
      <div class="sub-header">
        <h2>已选课程列表</h2>
      </div>
      <el-table v-loading="loading" :data="selectedCourses" border>
        <el-table-column prop="selectionId" label="选课编号" width="100" />
        <el-table-column prop="courseId" label="课程号" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="courseType" label="课程种类" width="120" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="teacherName" label="任课教师" width="110" />
        <el-table-column label="上课时间" min-width="180">
          <template #default="{ row }">
            周{{ weekLabel(row.weekDay) }} / {{ row.sectionDesc }} / {{ row.weekRange }}
          </template>
        </el-table-column>
        <el-table-column label="上课地点" min-width="140">
          <template #default="{ row }">
            {{ row.buildingName || '-' }} {{ row.roomName || '' }}
          </template>
        </el-table-column>
        <el-table-column prop="selectionTime" label="选课时间" min-width="170" />
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getSelectedCourses } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const selectedCourses = ref([])
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

function getCourses(weekDay, blockKey) {
  return selectedCourses.value.filter(item => item.weekDay === weekDay && resolveBlock(item.sectionDesc) === blockKey)
}

async function loadSelectedCourses() {
  loading.value = true
  try {
    const response = await getSelectedCourses()
    selectedCourses.value = response.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSelectedCourses()
})
</script>

<style scoped>
.page-header,
.sub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.sub-header h2 {
  margin: 0;
}

.calendar-shell {
  display: grid;
  grid-template-columns: 110px repeat(7, minmax(0, 1fr));
  border: 1px solid #dce6df;
  border-radius: 8px;
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
  background: #edf7f1;
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
  border-radius: 8px;
  background: linear-gradient(135deg, #e9f6ef 0%, #f8fbfd 100%);
  border: 1px solid #d6e9dc;
  font-size: 13px;
}

.course-card strong {
  color: #173f35;
}

.selected-section {
  margin-top: 28px;
}
</style>

