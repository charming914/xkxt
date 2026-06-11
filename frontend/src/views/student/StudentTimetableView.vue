<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学生课表</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <div class="toolbar">
        <el-select v-model="selectedSemester" clearable filterable placeholder="全部学期" class="semester-select">
          <el-option
            v-for="semester in semesterOptions"
            :key="semester"
            :label="semester"
            :value="semester"
          />
        </el-select>
        <el-button @click="loadTimetable">刷新课表</el-button>
        <el-button type="primary" :disabled="filteredTimetable.length === 0" @click="handlePrintTimetable">
          打印课表
        </el-button>
      </div>
    </div>

    <el-empty v-if="!loading && filteredTimetable.length === 0" :description="emptyDescription" />

    <div v-else v-loading="loading" class="calendar-shell">
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
        v-for="course in filteredTimetable"
        :key="`${course.courseName}-${course.teacherName}-${course.weekDay}-${course.sectionDesc}-${course.weekParity}-${course.weekRange}`"
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
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStudentTimetable } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const timetable = ref([])
const selectedSemester = ref('')
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

const semesterOptions = computed(() => {
  return [...new Set(timetable.value.map(item => item.semesterName).filter(Boolean))]
})

const filteredTimetable = computed(() => {
  if (!selectedSemester.value) {
    return timetable.value
  }
  return timetable.value.filter(item => item.semesterName === selectedSemester.value)
})

const emptyDescription = computed(() => {
  if (selectedSemester.value) {
    return `${selectedSemester.value} 暂无课表数据`
  }
  return '当前暂无课表数据'
})

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
  const courseCells = filteredTimetable.value.map(course => {
    const style = courseGridStyle(course)
    return `
      <div class="print-course" style="grid-column:${style.gridColumn};grid-row:${style.gridRow};">
        <strong>${escapeHtml(course.courseName)}</strong>
        <span>${escapeHtml(course.sectionDesc)}${course.weekParity && course.weekParity !== '全周' ? '（' + escapeHtml(course.weekParity) + '）' : ''}</span>
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
        <p>学号：${escapeHtml(userStore.refId || '-')}　学期：${escapeHtml(selectedSemester.value || '全部学期')}　导出时间：${escapeHtml(new Date().toLocaleString())}</p>
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

async function loadTimetable() {
  loading.value = true
  try {
    const response = await getStudentTimetable()
    timetable.value = response.data || []
    if (selectedSemester.value && !semesterOptions.value.includes(selectedSemester.value)) {
      selectedSemester.value = ''
    }
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.semester-select {
  width: 180px;
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
  border-radius: 8px;
  background: linear-gradient(135deg, #e7f8ff 0%, #f8fdff 100%);
  border: 1px solid #d6e9dc;
  box-shadow: 0 8px 20px rgba(2, 132, 199, 0.08);
  font-size: 13px;
}

.course-card strong {
  color: var(--sky-700);
}
</style>
