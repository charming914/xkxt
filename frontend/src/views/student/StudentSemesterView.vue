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
          filterable
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
        <el-button type="primary" :disabled="dataLoading || timetable.length === 0" @click="handlePrintTimetable">
          打印课表
        </el-button>
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
            <div class="calendar-head empty-cell" :style="gridStyle(1, 1)">节次</div>
            <div
              v-for="day in weekDays"
              :key="day.value"
              class="calendar-head"
              :style="gridStyle(day.value + 1, 1)"
            >
              周{{ day.label }}
            </div>

            <template v-for="(slot, slotIndex) in sectionSlots" :key="slot.key">
              <div class="section-label" :style="gridStyle(1, slotIndex + 2)">
                <strong>{{ slot.label }}</strong>
                <span>{{ slot.time }}</span>
              </div>
              <div
                v-for="day in weekDays"
                :key="`${day.value}-${slot.key}`"
                class="calendar-cell"
                :style="gridStyle(day.value + 1, slotIndex + 2)"
              />
            </template>

            <div
              v-for="course in timetable"
              :key="`${course.teachingClassId || course.courseName}-${course.weekDay}-${course.sectionDesc}-${course.weekRange}`"
              class="course-card"
              :style="courseGridStyle(course)"
            >
              <strong>{{ course.courseName }}</strong>
              <span>{{ course.sectionDesc }}<template v-if="course.weekParity && course.weekParity !== '全周'">（{{ course.weekParity }}）</template></span>
              <span>{{ course.teacherName }}</span>
              <span>{{ course.weekRange }}</span>
              <span>{{ course.buildingName || '-' }} {{ course.roomName || '' }}</span>
            </div>
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
        </el-tab-pane>
      </el-tabs>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
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

const sectionSlots = [
  { key: '1-2', label: '1-2节', time: '08:00-09:40', start: 1, end: 2 },
  { key: '3-4', label: '3-4节', time: '10:00-11:40', start: 3, end: 4 },
  { key: '5-6', label: '5-6节', time: '13:00-14:40', start: 5, end: 6 },
  { key: '7-8', label: '7-8节', time: '15:00-16:40', start: 7, end: 8 },
  { key: '9-10', label: '9-10节', time: '18:00-19:40', start: 9, end: 10 },
  { key: '11-12', label: '11-12节', time: '20:00-21:40', start: 11, end: 12 }
]

const selectedSemester = computed(() => {
  return semesters.value.find(item => Number(item.semesterId) === Number(selectedSemesterId.value)) || null
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

function parseSectionRange(sectionDesc) {
  const matched = String(sectionDesc || '').match(/(\d+)\s*-\s*(\d+)/)
  if (!matched) {
    return { start: 5, end: 6 }
  }
  return {
    start: Number(matched[1]),
    end: Number(matched[2])
  }
}

function findSlotIndex(sectionNumber) {
  const index = sectionSlots.findIndex(slot => sectionNumber >= slot.start && sectionNumber <= slot.end)
  return index >= 0 ? index : 0
}

function gridStyle(column, row) {
  return {
    gridColumn: `${column} / ${column + 1}`,
    gridRow: `${row} / ${row + 1}`
  }
}

function courseGridStyle(course) {
  const range = parseSectionRange(course.sectionDesc)
  const startIndex = findSlotIndex(range.start)
  const endIndex = findSlotIndex(range.end)
  const column = Number(course.weekDay) + 1
  return {
    gridColumn: `${column} / ${column + 1}`,
    gridRow: `${startIndex + 2} / ${endIndex + 3}`
  }
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function handlePrintTimetable() {
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.warning('浏览器阻止了打印窗口，请允许弹窗后重试')
    return
  }
  const headerCells = weekDays.map(day => `
    <div class="calendar-head" style="grid-column:${day.value + 1}/${day.value + 2};grid-row:1/2;">
      周${day.label}
    </div>
  `).join('')
  const sectionCells = sectionSlots.map((slot, index) => `
    <div class="section-label" style="grid-column:1/2;grid-row:${index + 2}/${index + 3};">
      <strong>${escapeHtml(slot.label)}</strong>
      <span>${escapeHtml(slot.time)}</span>
    </div>
  `).join('')
  const backgroundCells = sectionSlots.map((slot, index) => {
    return weekDays.map(day => `
      <div class="calendar-cell" style="grid-column:${day.value + 1}/${day.value + 2};grid-row:${index + 2}/${index + 3};"></div>
    `).join('')
  }).join('')
  const courseCells = timetable.value.map(course => {
    const style = courseGridStyle(course)
    return `
      <div class="print-course" style="grid-column:${style.gridColumn};grid-row:${style.gridRow};">
        <strong>${escapeHtml(course.courseName)}</strong>
        <span>${escapeHtml(course.sectionDesc)}${course.weekParity && course.weekParity !== '全周' ? `（${escapeHtml(course.weekParity)}）` : ''}</span>
        <span>${escapeHtml(course.teacherName)}</span>
        <span>${escapeHtml(course.weekRange)}</span>
        <span>${escapeHtml(course.buildingName || '-')} ${escapeHtml(course.roomName || '')}</span>
      </div>
    `
  }).join('')
  const title = `${userStore.realName || '学生'}课表`
  printWindow.document.write(`
    <!doctype html>
    <html>
      <head>
        <meta charset="utf-8" />
        <title>${escapeHtml(title)}</title>
        <style>
          body { margin: 24px; color: #1f2937; font-family: Arial, "Microsoft YaHei", sans-serif; }
          h1 { margin: 0 0 8px; font-size: 24px; }
          p { margin: 0 0 18px; color: #64748b; }
          .calendar-shell { display: grid; grid-template-columns: 96px repeat(7, minmax(0, 1fr)); grid-template-rows: 42px repeat(6, minmax(82px, auto)); border: 1px solid #cbd5e1; }
          .calendar-head, .section-label, .calendar-cell { padding: 8px; border-right: 1px solid #cbd5e1; border-bottom: 1px solid #cbd5e1; }
          .calendar-head { background: #e0f2fe; font-weight: 700; text-align: center; }
          .empty-cell, .section-label { background: #f8fafc; font-weight: 700; }
          .section-label span { display: block; margin-top: 6px; color: #64748b; font-size: 12px; }
          .print-course { display: grid; gap: 4px; z-index: 2; margin: 5px; padding: 8px; border: 1px solid #bae6fd; border-radius: 6px; background: #f0f9ff; font-size: 12px; }
          .print-course strong { color: #0369a1; font-size: 13px; }
          @media print { body { margin: 12mm; } }
        </style>
      </head>
      <body>
        <h1>${escapeHtml(title)}</h1>
        <p>学号：${escapeHtml(userStore.refId || '-')}　学期：${escapeHtml(selectedSemester.value?.semesterName || '未选择学期')}　导出时间：${escapeHtml(new Date().toLocaleString())}</p>
        <div class="calendar-shell">
          <div class="calendar-head empty-cell" style="grid-column:1/2;grid-row:1/2;">节次</div>
          ${headerCells}
          ${sectionCells}
          ${backgroundCells}
          ${courseCells}
        </div>
      </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  printWindow.print()
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
    const stillExists = semesters.value.some(item => Number(item.semesterId) === Number(selectedSemesterId.value))
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
  position: relative;
  display: grid;
  grid-template-columns: 110px repeat(7, minmax(0, 1fr));
  grid-template-rows: 54px repeat(6, minmax(104px, auto));
  border: 1px solid #dce6df;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.calendar-head,
.section-label,
.calendar-cell {
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
  min-height: 104px;
  background: #fff;
}

.course-card {
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 6px;
  padding: 10px;
  border: 1px solid #d4e8d8;
  border-radius: 8px;
  background: linear-gradient(135deg, #e7f8ff 0%, #f8fdff 100%);
  box-shadow: 0 8px 20px rgba(2, 132, 199, 0.08);
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
