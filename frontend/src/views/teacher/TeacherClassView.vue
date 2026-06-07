<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>教师授课管理</h1>
        <p>当前登录教师：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <div class="toolbar-actions">
        <el-button :disabled="classes.length === 0" @click="exportTeacherTimetablePdf">导出PDF课表</el-button>
        <el-button @click="loadTeachingClasses">刷新列表</el-button>
      </div>
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
            <span>选课人数：{{ course.selectedCount }} / {{ course.maxStudents }}</span>
            <el-button size="small" type="primary" plain @click="openClassDetail(course)">查看详情</el-button>
          </div>
        </div>
      </template>
    </div>

    <el-drawer v-model="detailVisible" size="64%" title="教学班详情">
      <div v-loading="detailLoading">
        <el-descriptions v-if="selectedClass" :column="2" border class="detail-block">
          <el-descriptions-item label="教学班编号">{{ selectedClass.teachingClassId }}</el-descriptions-item>
          <el-descriptions-item label="课程名称">{{ selectedClass.courseName }}</el-descriptions-item>
          <el-descriptions-item label="课程编号">{{ selectedClass.courseId }}</el-descriptions-item>
          <el-descriptions-item label="课程类型">{{ selectedClass.courseType }}</el-descriptions-item>
          <el-descriptions-item label="学分">{{ selectedClass.credit }}</el-descriptions-item>
          <el-descriptions-item label="考核方式">{{ selectedClass.assessmentMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学期">{{ selectedClass.semesterName }}</el-descriptions-item>
          <el-descriptions-item label="开课院系">{{ selectedClass.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上课时间">
            周{{ formatWeekDay(selectedClass.weekDay) }} {{ selectedClass.sectionDesc }} {{ selectedClass.weekRange }}
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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeachingClassDetail, getTeachingClasses, getTeachingClassStudents } from '../../api/teacher'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const detailLoading = ref(false)
const studentLoading = ref(false)
const classes = ref([])
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

function formatWeekDay(weekDay) {
  return weekDays.find(item => item.value === weekDay)?.label || weekDay
}

function classroomLabel(row) {
  return [row.buildingName, row.roomName].filter(Boolean).join(' ') || '-'
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
      <span>${escapeHtml(course.sectionDesc)} / ${escapeHtml(course.weekRange)}</span>
      <span>教学班：${escapeHtml(course.teachingClassId)}</span>
      <span>教室：${escapeHtml(classroomLabel(course))}</span>
      <span>人数：${escapeHtml(course.selectedCount)} / ${escapeHtml(course.maxStudents)}</span>
    </div>
  `
}

function exportTeacherTimetablePdf() {
  if (classes.value.length === 0) {
    ElMessage.warning('当前没有可导出的教师课表')
    return
  }

  const teacherLabel = `${userStore.realName || '教师'}（${userStore.refId || '-'}）`
  const semesterLabel = [...new Set(classes.value.map(item => item.semesterName).filter(Boolean))].join('、') || '全部学期'
  const rows = timeBlocks.map(block => {
    const cells = weekDays.map(day => {
      const cards = getClasses(day.value, block.key).map(buildPrintableCourseCard).join('')
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
    const response = await getTeachingClasses()
    classes.value = response.data || []
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
</style>
