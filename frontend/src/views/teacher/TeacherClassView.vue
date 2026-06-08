<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>教师授课管理</h1>
        <p>当前登录教师：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <div class="toolbar-actions">
        <el-button :disabled="filteredClasses.length === 0" @click="exportTeacherTimetablePdf">导出PDF课表</el-button>
        <el-button @click="loadTeachingClasses">刷新列表</el-button>
      </div>
    </div>

    <div v-if="classes.length > 0" class="semester-toolbar">
      <el-form-item label="课表学期">
        <el-select v-model="selectedSemesterId" placeholder="请选择学期" class="semester-select">
          <el-option
            v-for="item in semesterOptions"
            :key="item.semesterId"
            :label="item.semesterName"
            :value="item.semesterId"
          />
        </el-select>
      </el-form-item>
      <div v-if="selectedSemester" class="semester-summary">
        <strong>{{ selectedSemester.semesterName }}</strong>
        <span>{{ formatDate(selectedSemester.semesterStartDate) }} 至 {{ formatDate(selectedSemester.semesterEndDate) }}</span>
        <el-tag v-if="Number(selectedSemester.isCurrent) === 1" type="success">当前</el-tag>
        <el-tag :type="statusTagType(selectionStatus(selectedSemester))">
          {{ selectionStatus(selectedSemester) }}
        </el-tag>
      </div>
    </div>

    <el-empty v-if="!loading && classes.length === 0" description="当前暂无授课数据" />
    <el-empty v-else-if="!loading && filteredClasses.length === 0" description="该学期暂无授课数据" />

    <template v-else>
    <div v-loading="loading" class="calendar-shell">
      <div class="calendar-head empty-cell">时间段</div>
      <div v-for="day in weekDays" :key="day.value" class="calendar-head">周{{ day.label }}</div>

      <template v-for="block in timeBlocks" :key="block.key">
        <div class="section-label">
          <strong>{{ block.label }}</strong>
          <span>{{ block.time }}</span>
        </div>
        <div v-for="day in weekDays" :key="`${day.value}-${block.key}`" class="calendar-cell">
          <div
            v-for="course in getClasses(day.value, block)"
            :key="`${course.teachingClassId}-${course.courseName}-${course.weekDay}-${course.sectionDesc}`"
            class="course-card"
          >
            <strong>{{ course.courseName }}</strong>
            <span>{{ course.sectionDesc }}<template v-if="course.weekParity && course.weekParity !== '全周'">（{{ course.weekParity }}）</template></span>
            <span>教学班：{{ course.teachingClassId }}</span>
            <span>{{ course.weekRange }}</span>
            <span>{{ course.buildingName || '-' }} {{ course.roomName || '' }}</span>
            <span>选课人数：{{ course.selectedCount }} / {{ course.maxStudents }}</span>
            <el-button size="small" type="primary" plain @click="openClassDetail(course)">查看详情</el-button>
          </div>
        </div>
      </template>
    </div>
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

    <el-drawer v-model="detailVisible" size="64%" title="教学班详情">
      <div v-loading="detailLoading">
        <el-descriptions v-if="selectedClass" :column="2" border class="detail-block">
          <el-descriptions-item label="教学班编号">{{ selectedClass.teachingClassId }}</el-descriptions-item>
          <el-descriptions-item label="课程名称">{{ selectedClass.courseName }}</el-descriptions-item>
          <el-descriptions-item label="课程编号">{{ selectedClass.courseId }}</el-descriptions-item>
          <el-descriptions-item label="课程类型">{{ selectedClass.courseType }}</el-descriptions-item>
          <el-descriptions-item label="学分">{{ selectedClass.credit }}</el-descriptions-item>
          <el-descriptions-item label="考核方式">{{ selectedClass.assessmentMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="平时:考试">{{ scoreRatioLabel(selectedClass) }}</el-descriptions-item>
          <el-descriptions-item label="学期">
            {{ selectedClass.semesterName }}
            <el-tag v-if="Number(selectedClass.isCurrent) === 1" class="inline-tag" type="success">当前</el-tag>
            <el-tag class="inline-tag" :type="statusTagType(selectionStatus(selectedClass))">
              {{ selectionStatus(selectedClass) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="学期起止">
            {{ formatDate(selectedClass.semesterStartDate) }} 至 {{ formatDate(selectedClass.semesterEndDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="开课院系">{{ selectedClass.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上课时间">
            {{ selectedClass.scheduleText || '待排课' }}（{{ selectedClass.weekRange }}）
          </el-descriptions-item>
          <el-descriptions-item label="教室">
            {{ selectedClass.buildingName || '-' }} {{ selectedClass.roomName || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="选课人数">
            {{ selectedClass.selectedCount }} / {{ selectedClass.maxStudents }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ selectedClass.status }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-toolbar">
          <h2>学生名单</h2>
          <div class="toolbar-actions">
            <el-button @click="printRoster">打印名单</el-button>
            <el-button @click="loadClassStudents">刷新名单</el-button>
          </div>
        </div>

        <el-empty v-if="!studentLoading && students.length === 0" description="当前教学班暂无学生" />
        <el-table v-else v-loading="studentLoading" :data="students" border>
          <el-table-column type="index" label="序号" width="70" />
          <el-table-column prop="studentId" label="学号" width="120" />
          <el-table-column prop="studentName" label="姓名" width="110" />
          <el-table-column prop="gender" label="性别" width="80" />
          <el-table-column prop="className" label="行政班" min-width="140" />
          <el-table-column prop="majorName" label="专业" min-width="150" />
          <el-table-column prop="departmentName" label="院系" min-width="150" />
          <el-table-column prop="phone" label="联系电话" width="140" />
          <el-table-column label="成绩状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.isSubmitted ? 'success' : row.finalScore == null ? 'info' : 'warning'">
                {{ row.isSubmitted ? '已提交' : row.finalScore == null ? '未录入' : '已暂存' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeachingClassDetail, getTeachingClasses, getTeachingClassStudents } from '../../api/teacher'
import { useUserStore } from '../../stores/user'
import { useClientPagination } from '../../composables/useClientPagination'

const userStore = useUserStore()
const loading = ref(false)
const detailLoading = ref(false)
const studentLoading = ref(false)
const classes = ref([])
const selectedSemesterId = ref(null)
const semesterOptions = computed(() => {
  const map = new Map()
  classes.value.forEach(item => {
    if (!map.has(item.semesterId)) {
      map.set(item.semesterId, {
        semesterId: item.semesterId,
        semesterName: item.semesterName,
        semesterStartDate: item.semesterStartDate,
        semesterEndDate: item.semesterEndDate,
        courseSelectStart: item.courseSelectStart,
        courseSelectEnd: item.courseSelectEnd,
        isCurrent: item.isCurrent
      })
    }
  })
  return Array.from(map.values())
})
const selectedSemester = computed(() => {
  return semesterOptions.value.find(item => item.semesterId === selectedSemesterId.value) || null
})
const filteredClasses = computed(() => {
  if (!selectedSemesterId.value) {
    return classes.value
  }
  return classes.value.filter(item => item.semesterId === selectedSemesterId.value)
})
const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(filteredClasses, 10)
const students = ref([])
const selectedClass = ref(null)
const detailVisible = ref(false)
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

function getClasses(weekDay, block) {
  const result = []
  pagedData.value.forEach(item => {
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

function formatWeekDay(weekDay) {
  return weekDays.find(item => item.value === weekDay)?.label || weekDay
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function parseTime(str) {
  if (!str) return NaN
  return new Date(String(str).replace('T', ' ').replace(/-/g, '/')).getTime()
}

function selectionStatus(row) {
  if (!row?.courseSelectStart || !row?.courseSelectEnd) return '未设置'
  const now = Date.now()
  const start = parseTime(row.courseSelectStart)
  const end = parseTime(row.courseSelectEnd)
  if (now < start) return '未开始'
  if (now > end) return '已结束'
  return '选课中'
}

function statusTagType(status) {
  if (status === '选课中') return 'success'
  if (status === '已结束') return 'info'
  return 'warning'
}

function classroomLabel(row) {
  return [row.buildingName, row.roomName].filter(Boolean).join(' ') || '-'
}

function scoreRatioLabel(row) {
  if (row?.usualRatio == null || row?.examRatio == null) {
    return '-'
  }
  return `${row.usualRatio} : ${row.examRatio}`
}

function escapeHtml(value) {
  const text = value == null ? '' : String(value)
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function buildPrintableCourseCard(course) {
  return `
    <div class="course-card">
      <strong>${escapeHtml(course.courseName)}</strong>
      <span>${escapeHtml(course.sectionDesc)}${course.weekParity && course.weekParity !== '全周' ? `（${escapeHtml(course.weekParity)}）` : ''} / ${escapeHtml(course.weekRange)}</span>
      <span>教学班：${escapeHtml(course.teachingClassId)}</span>
      <span>教室：${escapeHtml(classroomLabel(course))}</span>
      <span>人数：${escapeHtml(course.selectedCount)} / ${escapeHtml(course.maxStudents)}</span>
    </div>
  `
}

function exportTeacherTimetablePdf() {
  if (filteredClasses.value.length === 0) {
    ElMessage.warning('当前没有可导出的教师课表')
    return
  }

  const teacherLabel = `${userStore.realName || '教师'}（${userStore.refId || '-'}）`
  const semesterLabel = selectedSemester.value
    ? `${selectedSemester.value.semesterName}（${selectionStatus(selectedSemester.value)}）`
    : '全部学期'
  const rows = timeBlocks.map(block => {
    const cells = weekDays.map(day => {
      const cards = filteredClasses.value
        .flatMap(item => parseSessions(item))
        .filter(session => {
          if (Number(session.weekDay) !== day.value) {
            return false
          }
          const { start, end } = parseSection(session.sectionDesc)
          return start <= block.end && block.start <= end
        })
        .map(buildPrintableCourseCard).join('')
      return `<td>${cards || '<span class="empty-text">无课程</span>'}</td>`
    }).join('')
    return `
      <tr>
        <th class="section-label">
          <strong>${escapeHtml(block.label)}</strong>
          <span>${escapeHtml(block.time)}</span>
        </th>
        ${cells}
      </tr>
    `
  }).join('')
  const html = `
    <!doctype html>
    <html lang="zh-CN">
      <head>
        <meta charset="utf-8" />
        <title>${escapeHtml(teacherLabel)}教师课表</title>
        <style>
          @page { size: A4 landscape; margin: 12mm; }
          * { box-sizing: border-box; }
          body {
            margin: 0;
            color: #1f2937;
            font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
            background: #fff;
          }
          .print-header {
            display: flex;
            align-items: flex-end;
            justify-content: space-between;
            gap: 24px;
            margin-bottom: 14px;
          }
          h1 {
            margin: 0 0 6px;
            color: #075985;
            font-size: 24px;
          }
          .meta {
            margin: 0;
            color: #4b5563;
            font-size: 13px;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            table-layout: fixed;
            border: 1px solid #dce6df;
          }
          th,
          td {
            min-height: 100px;
            padding: 9px;
            vertical-align: top;
            border: 1px solid #dce6df;
          }
          thead th {
            color: #0f172a;
            background: #edf1fb;
            text-align: center;
            font-size: 14px;
          }
          .section-label {
            width: 82px;
            background: #f8fbfd;
            text-align: left;
          }
          .section-label strong,
          .section-label span {
            display: block;
          }
          .section-label span {
            margin-top: 6px;
            color: #64748b;
            font-size: 12px;
          }
          .course-card {
            display: grid;
            gap: 3px;
            break-inside: avoid;
            margin-bottom: 7px;
            padding: 8px;
            border-radius: 8px;
            background: #f8fbfd;
            border: 1px solid #dbe4fb;
            font-size: 12px;
          }
          .course-card strong {
            color: #0369a1;
            font-size: 13px;
          }
          .course-card span,
          .empty-text {
            color: #475569;
          }
          .empty-text {
            font-size: 12px;
          }
        </style>
      </head>
      <body>
        <header class="print-header">
          <div>
            <h1>教师课表</h1>
            <p class="meta">教师：${escapeHtml(teacherLabel)} ｜ 学期：${escapeHtml(semesterLabel)}</p>
          </div>
          <p class="meta">导出时间：${escapeHtml(new Date().toLocaleString())}</p>
        </header>
        <table>
          <thead>
            <tr>
              <th class="section-label">时间段</th>
              ${weekDays.map(day => `<th>周${escapeHtml(day.label)}</th>`).join('')}
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
        <script>
          window.addEventListener('load', () => {
            window.focus();
            setTimeout(() => window.print(), 200);
          });
        <\/script>
      </body>
    </html>
  `
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.warning('浏览器阻止了导出窗口，请允许弹窗后重试')
    return
  }
  printWindow.document.open()
  printWindow.document.write(html)
  printWindow.document.close()
}

async function loadTeachingClasses() {
  loading.value = true
  try {
    const response = await getTeachingClasses({ includeAllSemesters: true })
    classes.value = response.data || []
    if (!semesterOptions.value.some(item => item.semesterId === selectedSemesterId.value)) {
      const currentSemester = semesterOptions.value.find(item => Number(item.isCurrent) === 1)
      selectedSemesterId.value = currentSemester?.semesterId ?? semesterOptions.value[0]?.semesterId ?? null
    }
  } finally {
    loading.value = false
  }
}

async function openClassDetail(course) {
  detailVisible.value = true
  selectedClass.value = course
  students.value = []
  await Promise.all([
    loadClassDetail(course.teachingClassId),
    loadClassStudents(course.teachingClassId)
  ])
}

async function loadClassDetail(teachingClassId = selectedClass.value?.teachingClassId) {
  if (!teachingClassId) {
    return
  }
  detailLoading.value = true
  try {
    const response = await getTeachingClassDetail(teachingClassId)
    selectedClass.value = response.data
  } finally {
    detailLoading.value = false
  }
}

async function loadClassStudents(teachingClassId = selectedClass.value?.teachingClassId) {
  if (!teachingClassId) {
    return
  }
  studentLoading.value = true
  try {
    const response = await getTeachingClassStudents(teachingClassId)
    students.value = response.data || []
  } finally {
    studentLoading.value = false
  }
}

function printRoster() {
  window.print()
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

.semester-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbfd;
}

.semester-toolbar :deep(.el-form-item) {
  margin-bottom: 0;
}

.semester-select {
  width: 220px;
}

.semester-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--text-muted);
}

.semester-summary strong {
  color: var(--text-main);
}

.inline-tag {
  margin-left: 6px;
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
  background: #f8fbfd;
  border: 1px solid #dbe4fb;
  font-size: 13px;
}

.course-card strong {
  color: var(--sky-700);
}

.detail-block {
  margin-bottom: 20px;
}

.drawer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 18px 0 12px;
}

.drawer-toolbar h2 {
  margin: 0;
  font-size: 18px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

@media print {
  .page-header,
  .calendar-shell,
  .toolbar-actions {
    display: none !important;
  }
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 720px) {
  .page-header,
  .semester-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .semester-select {
    width: 100%;
  }
}
</style>
