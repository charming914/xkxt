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
            v-for="course in getCourses(day.value, block)"
            :key="`${course.teachingClassId}-${course.courseId}-${course.weekDay}-${course.sectionDesc}`"
            class="course-card"
          >
            <strong>{{ course.courseName }}</strong>
            <span>{{ course.sectionDesc }}<template v-if="course.weekParity && course.weekParity !== '全周'">（{{ course.weekParity }}）</template></span>
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
      <el-empty v-if="!loading && selectedCourses.length === 0" description="当前暂无已选课程列表数据" />
      <template v-else>
        <el-table v-loading="loading" :data="pagedData" border>
          <el-table-column prop="selectionId" label="选课编号" width="100" />
          <el-table-column prop="courseId" label="课程号" width="120" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column prop="courseType" label="课程种类" width="120" />
          <el-table-column prop="credit" label="学分" width="80" />
          <el-table-column prop="teacherName" label="任课教师" width="110" />
          <el-table-column label="上课时间" min-width="180">
            <template #default="{ row }">
              {{ row.scheduleText || '待排课' }}（{{ row.weekRange }}）
            </template>
          </el-table-column>
          <el-table-column label="上课地点" min-width="140">
            <template #default="{ row }">
              {{ row.buildingName || '-' }} {{ row.roomName || '' }}
            </template>
          </el-table-column>
          <el-table-column prop="selectionTime" label="选课时间" min-width="170" />
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getSelectedCourses } from '../../api/student'
import { useUserStore } from '../../stores/user'
import { useClientPagination } from '../../composables/useClientPagination'

const userStore = useUserStore()
const loading = ref(false)
const selectedCourses = ref([])
const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(selectedCourses, 10)
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

function parseSection(sectionDesc) {
  const nums = String(sectionDesc).match(/\d+/g)
  if (!nums || nums.length === 0) {
    return { start: 0, end: 0 }
  }
  return { start: parseInt(nums[0], 10), end: parseInt(nums[nums.length - 1], 10) }
}

function parseSessions(course) {
  let sessions = course.sessionsJson
  if (typeof sessions === 'string') {
    try {
      sessions = JSON.parse(sessions)
    } catch {
      sessions = null
    }
  }
  if (!Array.isArray(sessions) || sessions.length === 0) {
    return []
  }
  return sessions.map(s => ({ ...course, ...s }))
}

function getCourses(weekDay, block) {
  const result = []
  selectedCourses.value.forEach(item => {
    parseSessions(item).forEach(session => {
      if (Number(session.weekDay) !== weekDay) {
        return
      }
      const { start, end } = parseSection(session.sectionDesc)
      if (start <= block.end && block.start <= end) {
        result.push(session)
      }
    })
  })
  return result
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
  border-radius: 8px;
  background: linear-gradient(135deg, #e7f8ff 0%, #f8fdff 100%);
  border: 1px solid #d6e9dc;
  font-size: 13px;
}

.course-card strong {
  color: var(--sky-700);
}

.selected-section {
  margin-top: 28px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>

