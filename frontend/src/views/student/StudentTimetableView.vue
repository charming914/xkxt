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
            v-for="course in getCourses(day.value, block)"
            :key="`${course.courseName}-${course.teacherName}-${course.weekRange}`"
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
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getStudentTimetable } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const timetable = ref([])
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
  { key: 's1_2', label: '第1-2节', time: '08:00-09:40', start: 1, end: 2 },
  { key: 's3_4', label: '第3-4节', time: '10:00-11:40', start: 3, end: 4 },
  { key: 's5_6', label: '第5-6节', time: '13:00-14:40', start: 5, end: 6 },
  { key: 's7_8', label: '第7-8节', time: '15:00-16:40', start: 7, end: 8 },
  { key: 's9_10', label: '第9-10节', time: '18:00-19:40', start: 9, end: 10 },
  { key: 's11_12', label: '第11-12节', time: '20:00-21:40', start: 11, end: 12 }
]

// 解析节次描述（如 "1-2节"、"3-4节"、"1-4节"）的起止节
function parseSection(sectionDesc) {
  const nums = String(sectionDesc).match(/\d+/g)
  if (!nums || nums.length === 0) {
    return { start: 0, end: 0 }
  }
  const start = parseInt(nums[0], 10)
  const end = parseInt(nums[nums.length - 1], 10)
  return { start, end }
}

function getCourses(weekDay, block) {
  return timetable.value.filter(item => {
    if (item.weekDay !== weekDay) {
      return false
    }
    // 课程节次区间与该行节次区间有重叠即显示
    const { start, end } = parseSection(item.sectionDesc)
    return start <= block.end && block.start <= end
  })
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
  background: var(--sky-100);
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
  color: var(--text-muted);
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
  background: linear-gradient(135deg, #e7f8ff 0%, #f8fdff 100%);
  border: 1px solid #d6e9dc;
  font-size: 13px;
}

.course-card strong {
  color: var(--sky-700);
}
</style>
