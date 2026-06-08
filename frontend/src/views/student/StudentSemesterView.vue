<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学期总览</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <div class="toolbar">
        <el-select
          v-model="selectedSemesterId"
          placeholder="选择学期"
          class="semester-select"
          :disabled="semesterLoading || semesters.length === 0"
          @change="loadSemesterData"
        >
          <el-option
            v-for="semester in semesters"
            :key="semester.semesterId"
            :label="semester.semesterName"
            :value="semester.semesterId"
          />
        </el-select>
        <el-button @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <el-empty v-if="!semesterLoading && semesters.length === 0" description="暂无学期数据" />

    <template v-else>
      <div class="summary-row">
        <div class="metric-tile">
          <span>已选课程</span>
          <strong>{{ selectedCourses.length }}</strong>
        </div>
        <div class="metric-tile">
          <span>考试安排</span>
          <strong>{{ exams.length }}</strong>
        </div>
        <div class="metric-tile">
          <span>平均总评</span>
          <strong>{{ averageScore }}</strong>
        </div>
        <div class="metric-tile">
          <span>学期绩点</span>
          <strong>{{ weightedGpa }}</strong>
        </div>
      </div>

      <div class="semester-meta">
        <strong>{{ selectedSemester?.semesterName || '未选择学期' }}</strong>
        <span>{{ selectedSemesterRange }}</span>
      </div>

      <el-tabs v-model="activePane" class="semester-tabs">
        <el-tab-pane label="课表" name="timetable">
          <el-empty v-if="!dataLoading && timetable.length === 0" description="该学期暂无课表" />
          <div v-else v-loading="dataLoading" class="calendar-shell">
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
                  :key="`${course.teachingClassId || course.courseName}-${course.weekDay}-${course.sectionDesc}`"
                  class="course-card"
                >
                  <strong>{{ course.courseName }}</strong>
                  <span>{{ course.sectionDesc }} / {{ course.weekRange }}</span>
                  <span>{{ course.teacherName }}</span>
                  <span>{{ course.buildingName || '-' }} {{ course.roomName || '' }}</span>
                </div>
              </div>
            </template>
          </div>
        </el-tab-pane>

        <el-tab-pane label="成绩" name="scores">
          <el-empty v-if="!dataLoading && scores.length === 0" description="该学期暂无成绩" />
          <el-table v-else v-loading="dataLoading" :data="scores" border>
            <el-table-column prop="courseId" label="课程号" width="120" />
            <el-table-column prop="courseName" label="课程名称" min-width="160" />
            <el-table-column prop="courseType" label="课程类型" width="120" />
            <el-table-column prop="teacherName" label="任课教师" width="110" />
            <el-table-column prop="credit" label="学分" width="80" />
            <el-table-column prop="usualScore" label="平时成绩" width="100" />
            <el-table-column prop="examScore" label="考试成绩" width="100" />
            <el-table-column prop="finalScore" label="总评" width="90" />
            <el-table-column prop="gpaPoint" label="绩点" width="90" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="scoreTagType(row.finalScore)">{{ scoreStatus(row.finalScore) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="考试" name="exams">
          <el-empty v-if="!dataLoading && exams.length === 0" description="该学期暂无考试安排" />
          <el-table v-else v-loading="dataLoading" :data="exams" border>
            <el-table-column prop="courseName" label="课程名称" min-width="160" />
            <el-table-column prop="courseType" label="类型" width="100" />
            <el-table-column prop="credit" label="学分" width="80" />
            <el-table-column prop="teacherName" label="任课教师" width="110" />
            <el-table-column label="考试时间" min-width="210">
              <template #default="{ row }">
                {{ formatDate(row.examDate) }} {{ formatTimeRange(row.startTime, row.endTime) }}
              </template>
            </el-table-column>
            <el-table-column label="考试地点" min-width="150">
              <template #default="{ row }">
                {{ row.buildingName ? `${row.buildingName} ${row.roomName || ''}` : '未指定' }}
              </template>
            </el-table-column>
            <el-table-column prop="examStatus" label="状态" width="100" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="已选课程" name="courses">
          <el-empty v-if="!dataLoading && selectedCourses.length === 0" description="该学期暂无已选课程" />
          <el-table v-else v-loading="dataLoading" :data="selectedCourses" border>
            <el-table-column prop="courseId" label="课程号" width="120" />
            <el-table-column prop="courseName" label="课程名称" min-width="160" />
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
        </el-tab-pane>
      </el-tabs>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  getSelectedCourses,
  getStudentExams,
  getStudentScores,
  getStudentSemesters,
  getStudentTimetable
} from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const semesterLoading = ref(false)
const dataLoading = ref(false)
const semesters = ref([])
const selectedSemesterId = ref(null)
const activePane = ref('timetable')
const timetable = ref([])
const scores = ref([])
const exams = ref([])
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
  { key: 's1_2', label: '第1-2节', time: '08:00-09:40', start: 1, end: 2 },
  { key: 's3_4', label: '第3-4节', time: '10:00-11:40', start: 3, end: 4 },
  { key: 's5_6', label: '第5-6节', time: '13:00-14:40', start: 5, end: 6 },
  { key: 's7_8', label: '第7-8节', time: '15:00-16:40', start: 7, end: 8 },
  { key: 's9_10', label: '第9-10节', time: '18:00-19:40', start: 9, end: 10 },
  { key: 's11_12', label: '第11-12节', time: '20:00-21:40', start: 11, end: 12 }
]

const selectedSemester = computed(() => {
  return semesters.value.find(item => item.semesterId === selectedSemesterId.value) || null
})

const selectedSemesterRange = computed(() => {
  if (!selectedSemester.value) {
    return '-'
  }
  return `${formatDate(selectedSemester.value.startDate)} 至 ${formatDate(selectedSemester.value.endDate)}`
})

const averageScore = computed(() => {
  if (scores.value.length === 0) {
    return '0.00'
  }
  const total = scores.value.reduce((sum, item) => sum + Number(item.finalScore || 0), 0)
  return (total / scores.value.length).toFixed(2)
})

const weightedGpa = computed(() => {
  const totalCredits = scores.value.reduce((sum, item) => {
    if (item.gpaPoint === null || item.gpaPoint === undefined) {
      return sum
    }
    return sum + Number(item.credit || 0)
  }, 0)
  if (!totalCredits) {
    return '0.00'
  }
  const weightedTotal = scores.value.reduce((sum, item) => {
    if (item.gpaPoint === null || item.gpaPoint === undefined) {
      return sum
    }
    return sum + Number(item.credit || 0) * Number(item.gpaPoint || 0)
  }, 0)
  return (weightedTotal / totalCredits).toFixed(2)
})

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return String(value).slice(0, 10)
}

function formatTimeRange(start, end) {
  if (!start || !end) {
    return ''
  }
  return `${String(start).slice(0, 5)}-${String(end).slice(0, 5)}`
}

function weekLabel(weekDay) {
  const match = weekDays.find(day => day.value === Number(weekDay))
  return match?.label || weekDay
}

function parseSection(sectionDesc) {
  const nums = String(sectionDesc || '').match(/\d+/g)
  if (!nums || nums.length === 0) {
    return { start: 0, end: 0 }
  }
  return { start: parseInt(nums[0], 10), end: parseInt(nums[nums.length - 1], 10) }
}

function getCourses(weekDay, block) {
  return timetable.value.filter(item => {
    if (Number(item.weekDay) !== weekDay) {
      return false
    }
    const { start, end } = parseSection(item.sectionDesc)
    return start <= block.end && block.start <= end
  })
}

function scoreStatus(finalScore) {
  if (finalScore === null || finalScore === undefined) {
    return '未录入'
  }
  return Number(finalScore) >= 60 ? '通过' : '未通过'
}

function scoreTagType(finalScore) {
  if (finalScore === null || finalScore === undefined) {
    return 'info'
  }
  return Number(finalScore) >= 60 ? 'success' : 'danger'
}

async function loadSemesters() {
  semesterLoading.value = true
  try {
    const response = await getStudentSemesters()
    semesters.value = response.data || []
    const stillExists = semesters.value.some(item => item.semesterId === selectedSemesterId.value)
    if (!stillExists) {
      const current = semesters.value.find(item => Number(item.isCurrent) === 1)
      selectedSemesterId.value = current?.semesterId ?? semesters.value[0]?.semesterId ?? null
    }
  } finally {
    semesterLoading.value = false
  }
}

async function loadSemesterData() {
  if (!selectedSemesterId.value) {
    timetable.value = []
    scores.value = []
    exams.value = []
    selectedCourses.value = []
    return
  }
  dataLoading.value = true
  const params = { semesterId: selectedSemesterId.value }
  try {
    const [timetableRes, scoreRes, examRes, selectedCourseRes] = await Promise.all([
      getStudentTimetable(params),
      getStudentScores(params),
      getStudentExams(params),
      getSelectedCourses(params)
    ])
    timetable.value = timetableRes.data || []
    scores.value = scoreRes.data || []
    exams.value = examRes.data || []
    selectedCourses.value = selectedCourseRes.data || []
  } finally {
    dataLoading.value = false
  }
}

async function refreshAll() {
  await loadSemesters()
  await loadSemesterData()
}

onMounted(() => {
  refreshAll()
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.semester-select {
  width: 220px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 0 0 18px;
}

.metric-tile {
  display: grid;
  gap: 6px;
  min-height: 76px;
  padding: 14px;
  border: 1px solid #dce8ef;
  border-radius: 8px;
  background: #f7fbfd;
}

.metric-tile span {
  color: var(--text-muted);
  font-size: 13px;
}

.metric-tile strong {
  font-size: 24px;
  color: #075985;
}

.semester-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 12px;
  border-radius: 8px;
  background: #eef8fb;
  color: var(--text-main);
}

.semester-meta span {
  color: var(--text-muted);
}

.semester-tabs {
  margin-top: 4px;
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
  border: 1px solid #d4e8d8;
  border-radius: 8px;
  background: #f0f9ff;
  font-size: 13px;
}

.course-card strong {
  color: var(--sky-700);
}

@media (max-width: 960px) {
  .page-header,
  .toolbar,
  .semester-meta {
    align-items: stretch;
    flex-direction: column;
  }

  .semester-select {
    width: 100%;
  }

  .summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .calendar-shell {
    min-width: 920px;
  }

  :deep(.el-tab-pane) {
    overflow-x: auto;
  }
}

@media (max-width: 560px) {
  .summary-row {
    grid-template-columns: 1fr;
  }
}
</style>
