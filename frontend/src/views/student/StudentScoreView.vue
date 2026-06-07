<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学生成绩</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <div class="toolbar">
        <el-button @click="refreshAll">刷新成绩</el-button>
        <el-button :disabled="filteredScores.length === 0" @click="handleExportCsv">导出 CSV</el-button>
        <el-button type="primary" :disabled="filteredScores.length === 0" @click="handlePrintScores">
          打印成绩单
        </el-button>
      </div>
    </div>

    <el-form :model="queryForm" label-width="78px" class="query-panel">
      <div class="query-grid">
        <el-form-item label="学期">
          <el-select v-model="queryForm.semesterName" clearable placeholder="选择学期">
            <el-option
              v-for="semester in semesterOptions"
              :key="semester"
              :label="semester"
              :value="semester"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-input v-model="queryForm.keyword" clearable placeholder="按课程号或课程名查询" />
        </el-form-item>
        <el-form-item label="成绩状态">
          <el-select v-model="queryForm.status" clearable placeholder="选择状态">
            <el-option label="通过" value="PASS" />
            <el-option label="未通过" value="FAIL" />
          </el-select>
        </el-form-item>
      </div>
      <div class="query-actions">
        <el-button type="primary" @click="applyQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </el-form>

    <div class="summary-row">
      <div class="summary-card">
        <span class="summary-label">查询结果课程</span>
        <strong>{{ filteredScores.length }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">平均总评</span>
        <strong>{{ averageScore }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">平均绩点</span>
        <strong>{{ weightedGpa }}</strong>
        <span class="summary-tip">{{ gpaScopeLabel }}</span>
      </div>
      <div class="summary-card">
        <span class="summary-label">计入学分</span>
        <strong>{{ weightedCredits }}</strong>
      </div>
    </div>

    <div class="analysis-grid">
      <section class="analysis-panel">
        <h2>学期绩点趋势</h2>
        <el-empty v-if="semesterGpaTrend.length === 0" description="暂无绩点趋势数据" />
        <div v-else class="trend-list">
          <div v-for="item in semesterGpaTrend" :key="item.semesterName" class="trend-row">
            <div class="trend-meta">
              <strong>{{ item.semesterName }}</strong>
              <span>{{ item.credits.toFixed(1) }} 学分 / 平均 {{ item.averageScore }}</span>
            </div>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: gpaBarWidth(item.gpa) }" />
            </div>
            <strong class="trend-value">{{ item.gpa }}</strong>
          </div>
        </div>
      </section>

      <section class="analysis-panel">
        <h2>课程类型学分</h2>
        <el-empty v-if="courseTypeCredits.length === 0" description="暂无课程类型统计" />
        <div v-else class="credit-list">
          <div v-for="item in courseTypeCredits" :key="item.courseType" class="credit-row">
            <div class="credit-header">
              <strong>{{ item.courseType }}</strong>
              <span>{{ item.credits.toFixed(1) }} 学分 / {{ item.count }} 门</span>
            </div>
            <div class="bar-track">
              <div class="bar-fill credit-fill" :style="{ width: creditBarWidth(item.credits) }" />
            </div>
          </div>
        </div>
      </section>

      <section class="analysis-panel warning-panel">
        <h2>未通过课程提醒</h2>
        <el-empty v-if="failedScoreList.length === 0" description="暂无未通过课程" />
        <ul v-else class="failed-list">
          <li v-for="item in failedScoreList" :key="`${item.semesterName}-${item.courseId}`">
            <strong>{{ item.courseName }}</strong>
            <span>{{ item.semesterName }} / {{ item.finalScore ?? '-' }} 分 / {{ item.credit || 0 }} 学分</span>
          </li>
        </ul>
      </section>
    </div>

    <el-empty v-if="!loading && filteredScores.length === 0" description="暂无符合条件的成绩数据" />

    <el-table v-else v-loading="loading" :data="filteredScores" border>
      <el-table-column prop="semesterName" label="学期" min-width="130" />
      <el-table-column prop="courseId" label="课程号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="courseType" label="课程类型" width="120" />
      <el-table-column prop="teacherName" label="任课教师" width="110" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="usualScore" label="平时成绩" width="110" />
      <el-table-column prop="examScore" label="考试成绩" width="110" />
      <el-table-column prop="finalScore" label="总评成绩" width="110" />
      <el-table-column prop="gradeLevel" label="等级" width="80" />
      <el-table-column prop="gpaPoint" label="绩点" width="90" />
      <el-table-column label="成绩状态" width="110">
        <template #default="{ row }">
          <el-tag :type="scoreTagType(row.finalScore)">
            {{ scoreStatus(row.finalScore) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="复核状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.scoreReviewStatus" :type="requestStatusType(row.scoreReviewStatus)">
            {{ row.scoreReviewStatus }}
          </el-tag>
          <span v-else>未申请</span>
        </template>
      </el-table-column>
      <el-table-column prop="enteredAt" label="录入时间" min-width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="primary"
            :disabled="!canRequestReview(row)"
            @click="openReviewDialog(row)"
          >
            {{ row.hasPendingScoreReview ? '待审核' : '申请复核' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="request-section">
      <div class="sub-header">
        <h2>我的成绩复核申请</h2>
        <el-button @click="loadReviewRequests">刷新申请记录</el-button>
      </div>
      <el-empty v-if="!requestLoading && reviewRequests.length === 0" description="暂无成绩复核申请记录" />
      <el-table v-else v-loading="requestLoading" :data="reviewRequests" border>
        <el-table-column prop="semesterName" label="学期" min-width="120" />
        <el-table-column prop="courseId" label="课程号" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="teacherName" label="任课教师" width="110" />
        <el-table-column prop="finalScore" label="总评成绩" width="100" />
        <el-table-column prop="requestReason" label="复核原因" min-width="180" />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="requestStatusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核备注" min-width="170" />
        <el-table-column prop="createdAt" label="申请时间" min-width="170" />
        <el-table-column prop="reviewedAt" label="审核时间" min-width="170" />
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="提交成绩复核申请" width="520px">
      <el-form label-position="top">
        <el-form-item label="课程">
          <el-input :model-value="selectedScoreLabel" disabled />
        </el-form-item>
        <el-form-item label="当前成绩">
          <el-input :model-value="selectedScoreValue" disabled />
        </el-form-item>
        <el-form-item label="复核原因">
          <el-input
            v-model.trim="requestReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请说明需要复核的具体原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateReviewRequest">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createScoreReviewRequest, getScoreReviewRequests, getStudentScores } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const requestLoading = ref(false)
const submitting = ref(false)
const scores = ref([])
const reviewRequests = ref([])
const dialogVisible = ref(false)
const selectedScore = ref(null)
const requestReason = ref('')
const queryForm = reactive({
  semesterName: '',
  keyword: '',
  status: ''
})
const appliedQuery = reactive({
  semesterName: '',
  keyword: '',
  status: ''
})

const semesterOptions = computed(() => {
  return [...new Set(scores.value.map(item => item.semesterName).filter(Boolean))]
})

const filteredScores = computed(() => {
  const keyword = appliedQuery.keyword.trim().toLowerCase()
  return scores.value.filter((item) => {
    const matchedSemester = !appliedQuery.semesterName || item.semesterName === appliedQuery.semesterName
    const matchedKeyword = !keyword ||
      String(item.courseName || '').toLowerCase().includes(keyword) ||
      String(item.courseId || '').toLowerCase().includes(keyword)
    const passed = Number(item.finalScore) >= 60
    const matchedStatus = !appliedQuery.status ||
      (appliedQuery.status === 'PASS' && passed) ||
      (appliedQuery.status === 'FAIL' && !passed)
    return matchedSemester && matchedKeyword && matchedStatus
  })
})

const averageScore = computed(() => {
  if (!filteredScores.value.length) {
    return '0.00'
  }
  const total = filteredScores.value.reduce((sum, item) => sum + Number(item.finalScore || 0), 0)
  return (total / filteredScores.value.length).toFixed(2)
})

const gpaBaseScores = computed(() => {
  if (appliedQuery.semesterName) {
    return scores.value.filter(item => item.semesterName === appliedQuery.semesterName)
  }
  return filteredScores.value
})

const weightedCredits = computed(() => {
  const total = gpaBaseScores.value.reduce((sum, item) => {
    if (item.gpaPoint === null || item.gpaPoint === undefined) {
      return sum
    }
    return sum + Number(item.credit || 0)
  }, 0)
  return total.toFixed(1)
})

const weightedGpa = computed(() => {
  const totalCredits = Number(weightedCredits.value)
  if (!totalCredits) {
    return '0.00'
  }
  const weightedTotal = gpaBaseScores.value.reduce((sum, item) => {
    if (item.gpaPoint === null || item.gpaPoint === undefined) {
      return sum
    }
    return sum + Number(item.credit || 0) * Number(item.gpaPoint || 0)
  }, 0)
  return (weightedTotal / totalCredits).toFixed(2)
})

const gpaScopeLabel = computed(() => {
  if (appliedQuery.semesterName) {
    return `按 ${appliedQuery.semesterName} 全部成绩计算`
  }
  return '按当前查询结果计算'
})

const semesterGpaTrend = computed(() => {
  const groups = new Map()
  scores.value.forEach((item) => {
    const semesterName = item.semesterName || '未分学期'
    if (!groups.has(semesterName)) {
      groups.set(semesterName, {
        semesterName,
        credits: 0,
        weightedGpa: 0,
        scoreTotal: 0,
        scoreCount: 0
      })
    }
    const group = groups.get(semesterName)
    const credit = Number(item.credit || 0)
    const gpa = Number(item.gpaPoint)
    if (!Number.isNaN(gpa)) {
      group.credits += credit
      group.weightedGpa += credit * gpa
    }
    if (item.finalScore !== null && item.finalScore !== undefined) {
      group.scoreTotal += Number(item.finalScore || 0)
      group.scoreCount += 1
    }
  })
  return [...groups.values()]
    .map(group => ({
      semesterName: group.semesterName,
      credits: group.credits,
      gpa: group.credits ? (group.weightedGpa / group.credits).toFixed(2) : '0.00',
      averageScore: group.scoreCount ? (group.scoreTotal / group.scoreCount).toFixed(2) : '0.00'
    }))
    .sort((a, b) => String(b.semesterName).localeCompare(String(a.semesterName), 'zh-CN'))
})

const courseTypeCredits = computed(() => {
  const groups = new Map()
  filteredScores.value.forEach((item) => {
    const courseType = item.courseType || '未分类'
    if (!groups.has(courseType)) {
      groups.set(courseType, {
        courseType,
        credits: 0,
        count: 0
      })
    }
    const group = groups.get(courseType)
    group.credits += Number(item.credit || 0)
    group.count += 1
  })
  return [...groups.values()].sort((a, b) => b.credits - a.credits)
})

const maxCourseTypeCredit = computed(() => {
  return Math.max(...courseTypeCredits.value.map(item => item.credits), 0)
})

const failedScoreList = computed(() => {
  return scores.value
    .filter(item => item.finalScore !== null && item.finalScore !== undefined && Number(item.finalScore) < 60)
    .sort((a, b) => String(b.semesterName).localeCompare(String(a.semesterName), 'zh-CN'))
})

const selectedScoreLabel = computed(() => {
  if (!selectedScore.value) return ''
  return `${selectedScore.value.courseName}（${selectedScore.value.courseId || '无课程号'}）`
})

const selectedScoreValue = computed(() => {
  if (!selectedScore.value) return ''
  return `平时 ${selectedScore.value.usualScore ?? '-'} / 考试 ${selectedScore.value.examScore ?? '-'} / 总评 ${selectedScore.value.finalScore ?? '-'}`
})

function scoreStatus(finalScore) {
  return Number(finalScore) >= 60 ? '通过' : '未通过'
}

function scoreTagType(finalScore) {
  return Number(finalScore) >= 60 ? 'success' : 'danger'
}

function requestStatusType(status) {
  if (status === '已通过') return 'success'
  if (status === '已驳回') return 'danger'
  return 'warning'
}

function canRequestReview(row) {
  return Number(row.isSubmitted) === 1 && Number(row.hasPendingScoreReview) !== 1
}

function gpaBarWidth(gpa) {
  const value = Math.max(0, Math.min(Number(gpa || 0), 4))
  return `${Math.max(value / 4 * 100, value > 0 ? 6 : 0)}%`
}

function creditBarWidth(credits) {
  const maxCredit = maxCourseTypeCredit.value
  if (!maxCredit) {
    return '0%'
  }
  const value = Number(credits || 0)
  return `${Math.max(value / maxCredit * 100, value > 0 ? 8 : 0)}%`
}

function formatValue(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return value
}

function escapeCsvCell(value) {
  const text = String(value ?? '')
  return `"${text.replace(/"/g, '""')}"`
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function handleExportCsv() {
  const headers = [
    '学期',
    '课程号',
    '课程名称',
    '课程类型',
    '任课教师',
    '学分',
    '平时成绩',
    '考试成绩',
    '总评成绩',
    '等级',
    '绩点',
    '成绩状态',
    '复核状态',
    '录入时间'
  ]
  const rows = filteredScores.value.map(item => [
    item.semesterName,
    item.courseId,
    item.courseName,
    item.courseType,
    item.teacherName,
    item.credit,
    item.usualScore,
    item.examScore,
    item.finalScore,
    item.gradeLevel,
    item.gpaPoint,
    scoreStatus(item.finalScore),
    item.scoreReviewStatus || '未申请',
    item.enteredAt
  ])
  const csv = [headers, ...rows]
    .map(row => row.map(escapeCsvCell).join(','))
    .join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.href = url
  link.download = `${userStore.refId || 'student'}-成绩单-${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function handlePrintScores() {
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.warning('浏览器阻止了打印窗口，请允许弹窗后重试')
    return
  }
  const rows = filteredScores.value.map(item => `
    <tr>
      <td>${escapeHtml(formatValue(item.semesterName))}</td>
      <td>${escapeHtml(formatValue(item.courseId))}</td>
      <td>${escapeHtml(formatValue(item.courseName))}</td>
      <td>${escapeHtml(formatValue(item.courseType))}</td>
      <td>${escapeHtml(formatValue(item.teacherName))}</td>
      <td>${escapeHtml(formatValue(item.credit))}</td>
      <td>${escapeHtml(formatValue(item.usualScore))}</td>
      <td>${escapeHtml(formatValue(item.examScore))}</td>
      <td>${escapeHtml(formatValue(item.finalScore))}</td>
      <td>${escapeHtml(formatValue(item.gradeLevel))}</td>
      <td>${escapeHtml(formatValue(item.gpaPoint))}</td>
      <td>${escapeHtml(scoreStatus(item.finalScore))}</td>
    </tr>
  `).join('')
  const title = `${userStore.realName || '学生'}成绩单`
  printWindow.document.write(`
    <!doctype html>
    <html>
      <head>
        <meta charset="utf-8" />
        <title>${escapeHtml(title)}</title>
        <style>
          body { margin: 24px; color: #1f2937; font-family: Arial, "Microsoft YaHei", sans-serif; }
          h1 { margin: 0 0 8px; font-size: 24px; }
          p { margin: 0 0 16px; color: #64748b; }
          .summary { display: flex; gap: 16px; margin-bottom: 18px; }
          .summary span { padding: 8px 12px; border: 1px solid #bae6fd; border-radius: 6px; background: #f0f9ff; }
          table { width: 100%; border-collapse: collapse; font-size: 12px; }
          th, td { border: 1px solid #cbd5e1; padding: 8px; text-align: left; }
          th { background: #e0f2fe; font-weight: 700; }
          @media print { body { margin: 12mm; } .summary { flex-wrap: wrap; } }
        </style>
      </head>
      <body>
        <h1>${escapeHtml(title)}</h1>
        <p>学号：${escapeHtml(userStore.refId || '-')}　导出时间：${escapeHtml(new Date().toLocaleString())}</p>
        <div class="summary">
          <span>课程数：${escapeHtml(filteredScores.value.length)}</span>
          <span>平均总评：${escapeHtml(averageScore.value)}</span>
          <span>平均绩点：${escapeHtml(weightedGpa.value)}</span>
          <span>计入学分：${escapeHtml(weightedCredits.value)}</span>
        </div>
        <table>
          <thead>
            <tr>
              <th>学期</th>
              <th>课程号</th>
              <th>课程名称</th>
              <th>课程类型</th>
              <th>教师</th>
              <th>学分</th>
              <th>平时</th>
              <th>考试</th>
              <th>总评</th>
              <th>等级</th>
              <th>绩点</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
      </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  printWindow.print()
}

function applyQuery() {
  Object.assign(appliedQuery, {
    semesterName: queryForm.semesterName,
    keyword: queryForm.keyword,
    status: queryForm.status
  })
}

function resetQuery() {
  Object.assign(queryForm, {
    semesterName: '',
    keyword: '',
    status: ''
  })
  applyQuery()
}

async function loadScores() {
  loading.value = true
  try {
    const response = await getStudentScores()
    scores.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadReviewRequests() {
  requestLoading.value = true
  try {
    const response = await getScoreReviewRequests()
    reviewRequests.value = response.data || []
  } finally {
    requestLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadScores(), loadReviewRequests()])
}

function openReviewDialog(row) {
  selectedScore.value = row
  requestReason.value = ''
  dialogVisible.value = true
}

async function handleCreateReviewRequest() {
  if (!selectedScore.value) {
    return
  }
  if (!requestReason.value) {
    ElMessage.warning('请填写复核原因')
    return
  }
  submitting.value = true
  try {
    const response = await createScoreReviewRequest({
      scoreId: selectedScore.value.scoreId,
      requestReason: requestReason.value
    })
    ElMessage.success(response.message || '成绩复核申请提交成功')
    dialogVisible.value = false
    await refreshAll()
  } finally {
    submitting.value = false
  }
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
  gap: 12px;
}

.query-panel {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid #dce6df;
  border-radius: 8px;
  background: #f8fbfd;
}

.query-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px 14px;
}

.query-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.query-panel :deep(.el-form-item) {
  margin-bottom: 14px;
}

.query-panel :deep(.el-select) {
  width: 100%;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.analysis-panel {
  min-height: 210px;
  padding: 18px;
  border-radius: 8px;
  border: 1px solid #d8e9df;
  background: #fff;
}

.analysis-panel h2 {
  margin: 0 0 14px;
  font-size: 16px;
}

.trend-list,
.credit-list {
  display: grid;
  gap: 14px;
}

.trend-row,
.credit-row {
  display: grid;
  gap: 8px;
}

.trend-row {
  grid-template-columns: minmax(110px, 1fr) minmax(120px, 1.4fr) 48px;
  align-items: center;
}

.trend-meta,
.credit-header {
  display: grid;
  gap: 4px;
}

.trend-meta span,
.credit-header span {
  color: var(--text-muted);
  font-size: 12px;
}

.trend-value {
  color: var(--sky-700);
  text-align: right;
}

.bar-track {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5eef5;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: #0284c7;
}

.credit-fill {
  background: #059669;
}

.warning-panel {
  border-color: #fed7aa;
  background: #fffaf4;
}

.failed-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.failed-list li {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #fed7aa;
  background: #fff7ed;
}

.failed-list span {
  color: #9a3412;
  font-size: 12px;
}

.summary-card {
  padding: 18px 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e7f8ff 0%, #f8fdff 100%);
  border: 1px solid #d8e9df;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: var(--sky-700);
}

.summary-label {
  color: var(--text-muted);
  font-size: 13px;
}

.summary-tip {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

.request-section {
  margin-top: 28px;
}

.sub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

@media (max-width: 960px) {
  .query-grid,
  .summary-row,
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}
</style>
