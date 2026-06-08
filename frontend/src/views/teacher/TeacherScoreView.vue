<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学生成绩录入</h1>
        <p>当前登录教师：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadScores">刷新成绩列表</el-button>
    </div>

    <div class="score-toolbar">
      <el-select v-model="selectedTeachingClassId" clearable placeholder="按教学班筛选" class="class-filter">
        <el-option
          v-for="item in classOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <div class="toolbar-actions">
        <el-dropdown trigger="click" :disabled="filteredScores.length === 0" @command="handleExportCommand">
          <el-button :disabled="filteredScores.length === 0">导出成绩</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="xlsx">导出 XLSX</el-dropdown-item>
              <el-dropdown-item command="csv">导出 CSV</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button :loading="importing" :disabled="filteredScores.length === 0" @click="openScoreImport">导入成绩</el-button>
        <el-button :disabled="editableScores.length === 0" @click="handleBatchSave(0)">批量暂存</el-button>
        <el-button type="primary" :disabled="editableScores.length === 0" @click="handleBatchSave(1)">批量提交</el-button>
      </div>
      <input
        ref="scoreImportInput"
        class="hidden-file-input"
        type="file"
        accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        @change="handleScoreImportFile"
      />
    </div>

    <el-alert
      class="deadline-alert"
      :title="scoreDeadlineReminder.title"
      :description="scoreDeadlineReminder.description"
      :type="scoreDeadlineReminder.type"
      show-icon
      :closable="false"
    />

    <div class="summary-row">
      <div class="summary-card">
        <span>学生人数</span>
        <strong>{{ scoreStats.total }}</strong>
      </div>
      <div class="summary-card">
        <span>平均分</span>
        <strong>{{ scoreStats.average }}</strong>
      </div>
      <div class="summary-card">
        <span>最高分</span>
        <strong>{{ scoreStats.max }}</strong>
      </div>
      <div class="summary-card">
        <span>最低分</span>
        <strong>{{ scoreStats.min }}</strong>
      </div>
      <div class="summary-card">
        <span>及格率</span>
        <strong>{{ scoreStats.passRate }}</strong>
      </div>
    </div>

    <div class="gpa-panel">
      <div class="gpa-panel-header">
        <h2>绩点分布</h2>
        <span>按当前筛选范围内已生成绩点的成绩统计</span>
      </div>
      <el-empty v-if="gpaDistribution.total === 0" description="暂无绩点分布数据" />
      <div v-else class="gpa-grid">
        <div v-for="item in gpaDistribution.items" :key="item.gpa" class="gpa-item">
          <div class="gpa-main">
            <strong>{{ item.gpa }}</strong>
            <el-tag size="small" :type="gradeTagType(item.grade)">{{ item.grade }}</el-tag>
          </div>
          <div class="gpa-count">
            <span>{{ item.count }} 人</span>
            <em>{{ item.percent }}</em>
          </div>
          <div class="gpa-bar">
            <div class="gpa-bar-fill" :style="{ width: item.percent }" />
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && filteredScores.length === 0" description="当前暂无成绩录入数据" />

    <template v-else>
    <el-table v-loading="loading" :data="pagedData" border>
      <el-table-column prop="teachingClassId" label="教学班编号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column label="平时:考试" width="110">
        <template #default="{ row }">{{ scoreRatioLabel(row) }}</template>
      </el-table-column>
      <el-table-column prop="studentId" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="110" />
      <el-table-column label="平时成绩" width="130">
        <template #default="{ row }">
          <el-input-number v-model="row.usualScore" :min="0" :max="100" :controls="false" :disabled="Boolean(row.isSubmitted)" />
        </template>
      </el-table-column>
      <el-table-column label="考试成绩" width="130">
        <template #default="{ row }">
          <el-input-number v-model="row.examScore" :min="0" :max="100" :controls="false" :disabled="Boolean(row.isSubmitted)" />
        </template>
      </el-table-column>
      <el-table-column prop="finalScore" label="总评成绩" width="110" />
      <el-table-column prop="gpaPoint" label="绩点" width="90" />
      <el-table-column label="提交状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.isSubmitted ? 'success' : 'info'">
            {{ row.isSubmitted ? '已提交' : '未提交' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <div class="action-row">
            <el-button size="small" :disabled="Boolean(row.isSubmitted)" @click="handleSave(row, 0)">暂存</el-button>
            <el-button type="primary" size="small" :disabled="Boolean(row.isSubmitted)" @click="handleSave(row, 1)">提交</el-button>
            <el-button size="small" type="warning" plain :disabled="!row.isSubmitted" @click="openScoreChangeDialog(row)">申请修改</el-button>
          </div>
        </template>
      </el-table-column>
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

    <div class="request-section">
      <div class="sub-header">
        <h2>成绩修改申请</h2>
        <el-button @click="loadScoreChangeRequests">刷新申请</el-button>
      </div>
      <el-empty v-if="!requestLoading && scoreChangeRequests.length === 0" description="暂无成绩修改申请" />
      <el-table v-else v-loading="requestLoading" :data="scoreChangeRequests" border>
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="originalFinalScore" label="原总评" width="100" />
        <el-table-column prop="requestReason" label="申请原因" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="requestStatusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核备注" min-width="160" />
      </el-table>
    </div>

    <el-dialog v-model="scoreChangeVisible" title="申请修改已提交成绩" width="520px">
      <el-form label-position="top">
        <el-form-item label="成绩记录">
          <el-input :model-value="selectedScoreLabel" disabled />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model.trim="scoreChangeReason" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scoreChangeVisible = false">取消</el-button>
        <el-button type="primary" :loading="requestSubmitting" @click="handleCreateScoreChangeRequest">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createTeacherScoreChangeRequest,
  getTeacherScoreChangeRequests,
  getTeacherScores,
  getScoreSubmitWindow,
  saveTeacherScore,
  saveTeacherScores
} from '../../api/teacher'
import { useUserStore } from '../../stores/user'
import { useClientPagination } from '../../composables/useClientPagination'

const userStore = useUserStore()
const loading = ref(false)
const requestLoading = ref(false)
const requestSubmitting = ref(false)
const importing = ref(false)
const scores = ref([])
const scoreChangeRequests = ref([])
const scoreSubmitWindows = ref({})
const scoreImportInput = ref(null)
const selectedScore = ref(null)
const scoreChangeReason = ref('')
const scoreChangeVisible = ref(false)
const selectedTeachingClassId = ref('')

const classOptions = computed(() => {
  const classes = new Map()
  scores.value.forEach(item => {
    if (!classes.has(item.teachingClassId)) {
      classes.set(item.teachingClassId, buildTeachingClassOptionLabel(item))
    }
  })
  return Array.from(classes, ([value, label]) => ({ value, label }))
})

const filteredScores = computed(() => {
  if (!selectedTeachingClassId.value) {
    return scores.value
  }
  return scores.value.filter(item => item.teachingClassId === selectedTeachingClassId.value)
})

const editableScores = computed(() => filteredScores.value.filter(item => !item.isSubmitted))
const filteredTeachingClassIds = computed(() => {
  return Array.from(new Set(filteredScores.value.map(item => item.teachingClassId).filter(Boolean)))
})
const gpaLevels = [
  { gpa: '4.0', grade: 'A' },
  { gpa: '3.7', grade: 'A-' },
  { gpa: '3.3', grade: 'B+' },
  { gpa: '3.0', grade: 'B' },
  { gpa: '2.7', grade: 'B-' },
  { gpa: '2.3', grade: 'C+' },
  { gpa: '2.0', grade: 'C' },
  { gpa: '1.7', grade: 'C-' },
  { gpa: '1.5', grade: 'D' },
  { gpa: '1.0', grade: 'D-' },
  { gpa: '0.0', grade: 'F' }
]

// 分页基于 filteredScores（完整筛选结果）；批量保存/导出/统计仍引用完整数据，不受当前页影响
const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(filteredScores, 10)

const selectedScoreLabel = computed(() => {
  if (!selectedScore.value) {
    return ''
  }
  return `${selectedScore.value.courseName} / ${selectedScore.value.studentName}（总评 ${selectedScore.value.finalScore ?? '-'}）`
})

const scoreStats = computed(() => {
  const finalScores = filteredScores.value
    .filter(item => item.finalScore != null)
    .map(item => Number(item.finalScore))
    .filter(item => !Number.isNaN(item))
  const total = filteredScores.value.length
  if (!finalScores.length) {
    return {
      total,
      average: '-',
      max: '-',
      min: '-',
      passRate: '0.00%'
    }
  }
  const sum = finalScores.reduce((current, item) => current + item, 0)
  const passed = finalScores.filter(item => item >= 60).length
  return {
    total,
    average: (sum / finalScores.length).toFixed(2),
    max: Math.max(...finalScores).toFixed(2),
    min: Math.min(...finalScores).toFixed(2),
    passRate: `${((passed / finalScores.length) * 100).toFixed(2)}%`
  }
})

const scoreDeadlineReminder = computed(() => {
  if (loading.value) {
    return {
      title: '成绩录入截止时间加载中',
      description: '正在读取教学班成绩提交时间窗口。',
      type: 'info'
    }
  }
  if (filteredScores.value.length === 0) {
    return {
      title: '成绩录入截止时间',
      description: '当前没有可录入成绩的教学班。',
      type: 'info'
    }
  }
  const pendingClassIds = filteredTeachingClassIds.value.filter(teachingClassId => !isTeachingClassCompleted(teachingClassId))
  if (pendingClassIds.length === 0) {
    return {
      title: '成绩录入已完成',
      description: '',
      type: 'success'
    }
  }
  const windows = pendingClassIds
    .map(id => ({ teachingClassId: id, ...(scoreSubmitWindows.value[id] || {}) }))
    .filter(item => item.scoreSubmitEnd)
  if (windows.length === 0) {
    return {
      title: '成绩录入截止时间未设置',
      description: selectedTeachingClassId.value
        ? '当前教学班尚未设置成绩提交截止时间。'
        : '当前列表中的教学班尚未设置成绩提交截止时间。',
      type: 'warning'
    }
  }
  const now = Date.now()
  const futureWindows = windows
    .filter(item => parseDateTime(item.scoreSubmitEnd) >= now)
    .sort((a, b) => parseDateTime(a.scoreSubmitEnd) - parseDateTime(b.scoreSubmitEnd))
  const overdueWindows = windows
    .filter(item => parseDateTime(item.scoreSubmitEnd) < now)
    .sort((a, b) => parseDateTime(b.scoreSubmitEnd) - parseDateTime(a.scoreSubmitEnd))
  const selectedWindow = selectedTeachingClassId.value
    ? windows.find(item => item.teachingClassId === selectedTeachingClassId.value)
    : null
  const target = selectedWindow || futureWindows[0] || overdueWindows[0]
  const deadline = formatDateTime(target.scoreSubmitEnd)
  const start = formatDateTime(target.scoreSubmitStart)
  const expired = parseDateTime(target.scoreSubmitEnd) < now
  return {
    title: selectedTeachingClassId.value
      ? `成绩录入截止时间：${deadline}`
      : `最近成绩录入截止时间：${deadline}`,
    description: selectedTeachingClassId.value
      ? `教学班 ${target.teachingClassId} 的成绩提交窗口：${start} 至 ${deadline}`
      : `当前列表中最近截止的教学班为 ${target.teachingClassId}；选择具体教学班可查看对应截止时间。`,
    type: expired ? 'error' : 'warning'
  }
})

const gpaDistribution = computed(() => {
  const counts = new Map(gpaLevels.map(item => [item.gpa, 0]))
  filteredScores.value.forEach(item => {
    if (item.gpaPoint === null || item.gpaPoint === undefined) {
      return
    }
    const value = Number(item.gpaPoint)
    if (Number.isNaN(value)) {
      return
    }
    const key = value.toFixed(1)
    if (counts.has(key)) {
      counts.set(key, counts.get(key) + 1)
    }
  })
  const total = Array.from(counts.values()).reduce((sum, count) => sum + count, 0)
  return {
    total,
    items: gpaLevels.map(item => {
      const count = counts.get(item.gpa) || 0
      return {
        ...item,
        count,
        percent: total ? `${((count / total) * 100).toFixed(1)}%` : '0.0%'
      }
    })
  }
})

async function loadScores() {
  loading.value = true
  try {
    const response = await getTeacherScores()
    scores.value = (response.data || []).map(item => ({
      ...item,
      usualScore: item.usualScore ?? 0,
      examScore: item.examScore ?? 0
    }))
    await loadScoreSubmitWindows()
  } finally {
    loading.value = false
  }
}

async function loadScoreSubmitWindows() {
  const teachingClassIds = Array.from(new Set(scores.value.map(item => item.teachingClassId).filter(Boolean)))
  const entries = await Promise.all(teachingClassIds.map(async teachingClassId => {
    try {
      const response = await getScoreSubmitWindow(teachingClassId)
      return [teachingClassId, response.data || {}]
    } catch (error) {
      return [teachingClassId, {}]
    }
  }))
  scoreSubmitWindows.value = Object.fromEntries(entries)
}

async function loadScoreChangeRequests() {
  requestLoading.value = true
  try {
    const response = await getTeacherScoreChangeRequests()
    scoreChangeRequests.value = response.data || []
  } finally {
    requestLoading.value = false
  }
}

async function handleSave(row, isSubmitted) {
  if (row.isSubmitted) {
    ElMessage.warning('已提交成绩不可重复修改')
    return
  }
  await saveTeacherScore({
    studentId: row.studentId,
    teachingClassId: row.teachingClassId,
    usualScore: Number(row.usualScore),
    examScore: Number(row.examScore),
    isSubmitted
  })
  ElMessage.success(isSubmitted ? '成绩提交成功' : '成绩暂存成功')
  await loadScores()
}

async function handleBatchSave(isSubmitted) {
  const payload = editableScores.value.map(row => ({
    studentId: row.studentId,
    teachingClassId: row.teachingClassId,
    usualScore: Number(row.usualScore),
    examScore: Number(row.examScore),
    isSubmitted
  }))
  if (payload.length === 0) {
    ElMessage.warning('当前没有可保存的成绩')
    return
  }
  await saveTeacherScores(payload)
  ElMessage.success(isSubmitted ? '成绩批量提交成功' : '成绩批量暂存成功')
  await loadScores()
}

function openScoreImport() {
  if (filteredScores.value.length === 0) {
    ElMessage.warning('当前没有可导入的成绩列表')
    return
  }
  if (scoreImportInput.value) {
    scoreImportInput.value.value = ''
    scoreImportInput.value.click()
  }
}

async function handleScoreImportFile(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }
  importing.value = true
  try {
    const rows = await readScoreImportRows(file)
    const result = buildScoreImportPayload(rows)
    if (result.payload.length === 0) {
      ElMessage.warning(`未导入成绩：${result.message}`)
      return
    }
    await saveTeacherScores(result.payload)
    ElMessage.success(`导入成功，已暂存 ${result.payload.length} 条；跳过 ${result.skipped} 条`)
    await loadScores()
  } finally {
    importing.value = false
    if (event.target) {
      event.target.value = ''
    }
  }
}

function requestStatusType(status) {
  if (status === '已通过') return 'success'
  if (status === '已驳回') return 'danger'
  return 'warning'
}

function gradeTagType(grade) {
  if (grade === 'A' || grade === 'A-') return 'success'
  if (grade === 'F') return 'danger'
  if (grade.startsWith('D')) return 'warning'
  return 'info'
}

function scoreRatioLabel(row) {
  if (row.usualRatio == null || row.examRatio == null) {
    return '-'
  }
  return `${row.usualRatio} : ${row.examRatio}`
}

function buildTeachingClassOptionLabel(row) {
  const base = `${row.teachingClassId} - ${row.courseName}`
  const details = [
    row.semesterName,
    formatTeachingTime(row)
  ].filter(Boolean).join('｜')
  return details ? `${base}｜${details}` : base
}

function formatTeachingTime(row) {
  const parts = []
  if (row.scheduleText) {
    parts.push(row.scheduleText)
  }
  if (row.weekRange) {
    parts.push(row.weekRange)
  }
  return parts.join(' ')
}

function formatWeekDay(weekDay) {
  return {
    1: '一',
    2: '二',
    3: '三',
    4: '四',
    5: '五',
    6: '六',
    7: '日'
  }[Number(weekDay)] || weekDay
}

function isTeachingClassCompleted(teachingClassId) {
  const classScores = scores.value.filter(item => item.teachingClassId === teachingClassId)
  return classScores.length > 0 && classScores.every(item => Boolean(item.isSubmitted))
}

function parseDateTime(value) {
  if (!value) return Number.POSITIVE_INFINITY
  return new Date(String(value).replace('T', ' ').replace(/-/g, '/')).getTime()
}

function formatDateTime(value) {
  if (!value) return '未设置'
  return String(value).replace('T', ' ').slice(0, 19)
}

async function readScoreImportRows(file) {
  if (isXlsxFile(file)) {
    return readXlsxRows(file)
  }
  const text = await readTextFile(file)
  return parseCsv(text)
}

function readTextFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error)
    reader.readAsText(file, 'utf-8')
  })
}

function readArrayBufferFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(reader.error)
    reader.readAsArrayBuffer(file)
  })
}

async function readXlsxRows(file) {
  const XLSX = await loadXlsx()
  const buffer = await readArrayBufferFile(file)
  const workbook = XLSX.read(buffer, { type: 'array' })
  const firstSheetName = workbook.SheetNames[0]
  if (!firstSheetName) {
    return []
  }
  const worksheet = workbook.Sheets[firstSheetName]
  return XLSX.utils.sheet_to_json(worksheet, { header: 1, defval: '' })
}

function isXlsxFile(file) {
  return /\.xlsx$/i.test(file?.name || '')
}

function buildScoreImportPayload(rows) {
  if (rows.length < 2) {
    return { payload: [], skipped: 0, message: '文件中没有可读取的数据行' }
  }
  const headers = rows[0].map(normalizeHeader)
  const studentIdIndex = findHeaderIndex(headers, ['学号', 'studentid'])
  const studentNameIndex = findHeaderIndex(headers, ['姓名', '学生', '学生姓名', 'studentname', 'name'])
  const usualScoreIndex = findHeaderIndex(headers, ['平时成绩', 'usualscore'])
  const examScoreIndex = findHeaderIndex(headers, ['考试成绩', 'examscore'])
  const teachingClassIndex = findHeaderIndex(headers, ['教学班编号', '教学班', 'teachingclassid'])
  if (studentIdIndex < 0 || studentNameIndex < 0 || usualScoreIndex < 0 || examScoreIndex < 0) {
    return {
      payload: [],
      skipped: Math.max(rows.length - 1, 0),
      message: '需要包含学号、姓名、平时成绩和考试成绩列'
    }
  }

  const editableRows = filteredScores.value.filter(item => !item.isSubmitted)
  const byIdentity = new Map()
  editableRows.forEach(row => {
    const key = scoreImportKey(row.studentId, row.studentName)
    if (!byIdentity.has(key)) {
      byIdentity.set(key, [])
    }
    byIdentity.get(key).push(row)
  })

  const latestByClassAndStudent = new Map()
  let skipped = 0
  rows.slice(1).forEach(row => {
    const studentId = normalizeCell(row[studentIdIndex])
    const studentName = normalizeCell(row[studentNameIndex])
    const usualScore = parseImportScore(row[usualScoreIndex])
    const examScore = parseImportScore(row[examScoreIndex])
    if (!studentId || !studentName || usualScore === null || examScore === null) {
      skipped += 1
      return
    }
    const matches = byIdentity.get(scoreImportKey(studentId, studentName)) || []
    const teachingClassId = teachingClassIndex >= 0 ? normalizeCell(row[teachingClassIndex]) : ''
    const matchedRows = teachingClassId
      ? matches.filter(item => String(item.teachingClassId) === teachingClassId)
      : matches
    if (matchedRows.length !== 1) {
      skipped += 1
      return
    }
    const matched = matchedRows[0]
    latestByClassAndStudent.set(`${matched.teachingClassId}:${matched.studentId}`, {
      studentId: matched.studentId,
      teachingClassId: matched.teachingClassId,
      usualScore,
      examScore,
      isSubmitted: 0
    })
  })

  return {
    payload: Array.from(latestByClassAndStudent.values()),
    skipped,
    message: '没有匹配到可暂存的未提交成绩'
  }
}

function parseCsv(text) {
  const normalized = String(text || '').replace(/^\uFEFF/, '')
  const rows = []
  let current = ''
  let row = []
  let inQuotes = false
  for (let i = 0; i < normalized.length; i += 1) {
    const char = normalized[i]
    const next = normalized[i + 1]
    if (char === '"') {
      if (inQuotes && next === '"') {
        current += '"'
        i += 1
      } else {
        inQuotes = !inQuotes
      }
      continue
    }
    if (char === ',' && !inQuotes) {
      row.push(current)
      current = ''
      continue
    }
    if ((char === '\n' || char === '\r') && !inQuotes) {
      if (char === '\r' && next === '\n') {
        i += 1
      }
      row.push(current)
      if (row.some(cell => normalizeCell(cell) !== '')) {
        rows.push(row)
      }
      row = []
      current = ''
      continue
    }
    current += char
  }
  row.push(current)
  if (row.some(cell => normalizeCell(cell) !== '')) {
    rows.push(row)
  }
  return rows
}

function findHeaderIndex(headers, aliases) {
  return headers.findIndex(header => aliases.includes(header))
}

function normalizeHeader(value) {
  return normalizeCell(value).replace(/\s+/g, '').replace(/[:：]/g, '').toLowerCase()
}

function normalizeCell(value) {
  return value == null ? '' : String(value).trim()
}

function scoreImportKey(studentId, studentName) {
  return `${normalizeCell(studentId)}::${normalizeCell(studentName)}`
}

function parseImportScore(value) {
  const text = normalizeCell(value)
  if (!text) {
    return null
  }
  const score = Number(text)
  if (!Number.isFinite(score) || score < 0 || score > 100) {
    return null
  }
  return score
}

function openScoreChangeDialog(row) {
  selectedScore.value = row
  scoreChangeReason.value = ''
  scoreChangeVisible.value = true
}

async function handleCreateScoreChangeRequest() {
  if (!selectedScore.value) {
    return
  }
  if (!scoreChangeReason.value) {
    ElMessage.warning('请填写申请原因')
    return
  }
  requestSubmitting.value = true
  try {
    await createTeacherScoreChangeRequest({
      scoreId: selectedScore.value.scoreId,
      requestReason: scoreChangeReason.value
    })
    ElMessage.success('成绩修改申请提交成功')
    scoreChangeVisible.value = false
    await loadScoreChangeRequests()
  } finally {
    requestSubmitting.value = false
  }
}

function escapeCsv(value) {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

async function handleExportCommand(command) {
  if (command === 'csv') {
    exportScoresCsv()
    return
  }
  await exportScoresXlsx()
}

function buildScoreExportRows() {
  const headers = ['教学班编号', '课程名称', '平时:考试', '学号', '姓名', '平时成绩', '考试成绩', '总评成绩', '绩点', '提交状态']
  const rows = filteredScores.value.map(row => [
    row.teachingClassId,
    row.courseName,
    scoreRatioLabel(row),
    row.studentId,
    row.studentName,
    row.usualScore,
    row.examScore,
    row.finalScore,
    row.gpaPoint,
    row.isSubmitted ? '已提交' : '未提交'
  ])
  return { headers, rows }
}

function exportScoresCsv() {
  const { headers, rows } = buildScoreExportRows()
  const content = [headers, ...rows].map(row => row.map(escapeCsv).join(',')).join('\n')
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `teacher-scores-${selectedTeachingClassId.value || 'all'}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

async function exportScoresXlsx() {
  const XLSX = await loadXlsx()
  const { headers, rows } = buildScoreExportRows()
  const worksheet = XLSX.utils.aoa_to_sheet([headers, ...rows])
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '成绩录入')
  XLSX.writeFile(workbook, `teacher-scores-${selectedTeachingClassId.value || 'all'}.xlsx`)
}

async function loadXlsx() {
  return import('xlsx')
}

onMounted(() => {
  loadScores()
  loadScoreChangeRequests()
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

.score-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.class-filter {
  width: 520px;
}

.deadline-alert {
  margin-bottom: 16px;
}

.hidden-file-input {
  display: none;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card {
  padding: 16px;
  border-radius: 8px;
  background: #f8fbfd;
  border: 1px solid #c8ecff;
}

.summary-card span {
  display: block;
  color: var(--text-muted);
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  color: var(--sky-700);
  font-size: 24px;
}

.gpa-panel {
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #dbeafe;
}

.gpa-panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.gpa-panel-header h2 {
  margin: 0;
  font-size: 16px;
}

.gpa-panel-header span {
  color: var(--text-muted);
  font-size: 13px;
}

.gpa-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.gpa-item {
  display: grid;
  gap: 8px;
  min-height: 104px;
  padding: 12px;
  border-radius: 8px;
  background: #f8fbfd;
  border: 1px solid #e2e8f0;
}

.gpa-main,
.gpa-count {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.gpa-main strong {
  color: var(--sky-700);
  font-size: 20px;
}

.gpa-count span {
  color: var(--text-main);
  font-weight: 600;
}

.gpa-count em {
  color: var(--text-muted);
  font-style: normal;
  font-size: 12px;
}

.gpa-bar {
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.gpa-bar-fill {
  height: 100%;
  border-radius: inherit;
  background: #0284c7;
}

.toolbar-actions,
.action-row {
  display: flex;
  gap: 10px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.request-section {
  margin-top: 28px;
}

.sub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 720px) {
  .score-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .class-filter {
    width: 100%;
  }

  .summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .gpa-panel-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
