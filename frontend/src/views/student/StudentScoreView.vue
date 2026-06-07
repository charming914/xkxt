<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>成绩查询</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadScores">刷新成绩</el-button>
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
        <span class="summary-label">学期平均绩点</span>
        <strong>{{ semesterWeightedGpa }}</strong>
        <span class="summary-tip">{{ selectedSemesterLabel }}</span>
      </div>
      <div class="summary-card">
        <span class="summary-label">计入学分</span>
        <strong>{{ weightedCredits }}</strong>
      </div>
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
            v-model="requestReason"
            type="textarea"
            :rows="4"
            placeholder="请说明需要复核的具体原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateReviewRequest">提交申请</el-button>
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

const semesterWeightedGpa = computed(() => {
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

const selectedSemesterLabel = computed(() => {
  if (appliedQuery.semesterName) {
    return `按 ${appliedQuery.semesterName} 全部成绩计算`
  }
  return '未选择学期时按当前查询结果计算'
})

const selectedScoreLabel = computed(() => {
  if (!selectedScore.value) return ''
  return `${selectedScore.value.courseName}（${selectedScore.value.courseId}）`
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
    if (!appliedQuery.semesterName && semesterOptions.value.length === 1) {
      queryForm.semesterName = semesterOptions.value[0]
      applyQuery()
    }
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

function openReviewDialog(row) {
  selectedScore.value = row
  requestReason.value = ''
  dialogVisible.value = true
}

async function handleCreateReviewRequest() {
  if (!selectedScore.value) {
    return
  }
  if (!requestReason.value.trim()) {
    ElMessage.warning('请填写复核原因')
    return
  }
  const response = await createScoreReviewRequest({
    scoreId: selectedScore.value.scoreId,
    requestReason: requestReason.value.trim()
  })
  ElMessage.success(response.message || '成绩复核申请提交成功')
  dialogVisible.value = false
  await loadScores()
  await loadReviewRequests()
}

onMounted(() => {
  loadScores()
  loadReviewRequests()
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

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 18px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #edf7f1 0%, #f8fbfd 100%);
  border: 1px solid #d8e9df;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: #173f35;
}

.summary-label {
  color: #5b6d68;
  font-size: 13px;
}

.summary-tip {
  display: block;
  margin-top: 6px;
  color: #6b7b76;
  font-size: 12px;
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

.request-section {
  margin-top: 28px;
}

.sub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
</style>
