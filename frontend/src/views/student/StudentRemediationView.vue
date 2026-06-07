<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>补考重修</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="refreshAll">刷新数据</el-button>
    </div>

    <el-alert
      title="申请规则：课程成绩未通过后先申请补考；补考审核通过且结果登记为未通过后，才能申请重修。"
      type="info"
      :closable="false"
      class="tip-alert"
    />

    <section class="section-block">
      <div class="sub-header">
        <h2>未通过课程</h2>
        <span class="muted-text">仅显示已提交且总评低于 60 分的成绩</span>
      </div>
      <el-empty v-if="!scoreLoading && failedScores.length === 0" description="暂无需要补考或重修的课程" />
      <el-table v-else v-loading="scoreLoading" :data="failedScores" border>
        <el-table-column prop="semesterName" label="学期" min-width="120" />
        <el-table-column prop="courseId" label="课程号" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="160" />
        <el-table-column prop="courseType" label="课程类型" width="120" />
        <el-table-column prop="teacherName" label="任课教师" width="110" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="finalScore" label="总评" width="80" />
        <el-table-column label="补考状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.makeupStatus" :type="statusType(row.makeupStatus)">{{ row.makeupStatus }}</el-tag>
            <span v-else>未申请</span>
          </template>
        </el-table-column>
        <el-table-column label="补考结果" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.makeupResultStatus" :type="makeupResultType(row.makeupResultStatus)">
              {{ row.makeupResultStatus }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="重修状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.retakeStatus" :type="statusType(row.retakeStatus)">{{ row.retakeStatus }}</el-tag>
            <span v-else>未申请</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" type="primary" :disabled="Number(row.canApplyMakeup) !== 1" @click="openRequest(row, 'MAKEUP')">
                申请补考
              </el-button>
              <el-button size="small" type="warning" :disabled="Number(row.canApplyRetake) !== 1" @click="openRequest(row, 'RETAKE')">
                申请重修
              </el-button>
            </div>
            <span class="reason-text">{{ remediationHint(row) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div class="record-grid">
      <section class="section-block">
        <div class="sub-header">
          <h2>我的补考申请</h2>
          <el-button size="small" @click="loadMakeupRequests">刷新</el-button>
        </div>
        <el-empty v-if="!makeupLoading && makeupRequests.length === 0" description="暂无补考申请记录" />
        <el-table v-else v-loading="makeupLoading" :data="makeupRequests" border>
          <el-table-column prop="semesterName" label="学期" min-width="120" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column prop="finalScore" label="原成绩" width="90" />
          <el-table-column prop="requestReason" label="申请原因" min-width="160" />
          <el-table-column label="审核状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="补考结果" width="110">
            <template #default="{ row }">
              <el-tag :type="makeupResultType(row.resultStatus)">{{ row.resultStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewComment" label="审核备注" min-width="150" />
          <el-table-column prop="createdAt" label="申请时间" min-width="170" />
        </el-table>
      </section>

      <section class="section-block">
        <div class="sub-header">
          <h2>我的重修申请</h2>
          <el-button size="small" @click="loadRetakeRequests">刷新</el-button>
        </div>
        <el-empty v-if="!retakeLoading && retakeRequests.length === 0" description="暂无重修申请记录" />
        <el-table v-else v-loading="retakeLoading" :data="retakeRequests" border>
          <el-table-column prop="semesterName" label="学期" min-width="120" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column prop="finalScore" label="原成绩" width="90" />
          <el-table-column prop="requestReason" label="申请原因" min-width="160" />
          <el-table-column label="审核状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewComment" label="审核备注" min-width="150" />
          <el-table-column prop="createdAt" label="申请时间" min-width="170" />
        </el-table>
      </section>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form label-position="top">
        <el-form-item label="课程">
          <el-input :model-value="selectedCourseLabel" disabled />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input
            v-model.trim="requestReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请说明申请原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createMakeupRequest,
  createRetakeRequest,
  getAcademicRemediationScores,
  getMakeupRequests,
  getRetakeRequests
} from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const scoreLoading = ref(false)
const makeupLoading = ref(false)
const retakeLoading = ref(false)
const submitting = ref(false)
const failedScores = ref([])
const makeupRequests = ref([])
const retakeRequests = ref([])
const dialogVisible = ref(false)
const selectedScore = ref(null)
const requestType = ref('MAKEUP')
const requestReason = ref('')

const dialogTitle = computed(() => requestType.value === 'MAKEUP' ? '提交补考申请' : '提交重修申请')

const selectedCourseLabel = computed(() => {
  if (!selectedScore.value) return ''
  return `${selectedScore.value.courseName}（${selectedScore.value.courseId || '无课程号'}，总评 ${selectedScore.value.finalScore ?? '-'}）`
})

function statusType(status) {
  if (status === '已通过') return 'success'
  if (status === '已驳回') return 'danger'
  return 'warning'
}

function makeupResultType(status) {
  if (status === '已通过') return 'success'
  if (status === '未通过') return 'danger'
  if (status === '未安排') return 'info'
  return 'warning'
}

function remediationHint(row) {
  if (Number(row.canApplyMakeup) === 1) {
    return '可先申请补考'
  }
  if (Number(row.canApplyRetake) === 1) {
    return '补考未通过，可申请重修'
  }
  if (row.retakeStatus === '待审核') {
    return '重修申请待审核'
  }
  if (row.retakeStatus === '已通过') {
    return '重修申请已通过'
  }
  if (row.makeupStatus === '待审核') {
    return '补考申请待审核'
  }
  if (row.makeupStatus === '已通过' && row.makeupResultStatus === '待考试') {
    return '补考已安排，等待考试结果'
  }
  if (row.makeupStatus === '已通过' && row.makeupResultStatus === '已通过') {
    return '补考已通过，无需重修'
  }
  if (row.makeupStatus === '已驳回') {
    return '补考申请被驳回，可重新申请'
  }
  return '需先完成补考流程'
}

async function loadFailedScores() {
  scoreLoading.value = true
  try {
    const response = await getAcademicRemediationScores()
    failedScores.value = response.data || []
  } finally {
    scoreLoading.value = false
  }
}

async function loadMakeupRequests() {
  makeupLoading.value = true
  try {
    const response = await getMakeupRequests()
    makeupRequests.value = response.data || []
  } finally {
    makeupLoading.value = false
  }
}

async function loadRetakeRequests() {
  retakeLoading.value = true
  try {
    const response = await getRetakeRequests()
    retakeRequests.value = response.data || []
  } finally {
    retakeLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadFailedScores(), loadMakeupRequests(), loadRetakeRequests()])
}

function openRequest(row, type) {
  selectedScore.value = row
  requestType.value = type
  requestReason.value = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!selectedScore.value) {
    return
  }
  if (!requestReason.value) {
    ElMessage.warning('请填写申请原因')
    return
  }
  submitting.value = true
  try {
    const payload = {
      scoreId: selectedScore.value.scoreId,
      requestReason: requestReason.value
    }
    const response = requestType.value === 'MAKEUP'
      ? await createMakeupRequest(payload)
      : await createRetakeRequest(payload)
    ElMessage.success(response.message || '申请提交成功')
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
.page-header,
.sub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.tip-alert {
  margin-bottom: 18px;
}

.section-block {
  margin-top: 22px;
}

.record-grid {
  display: grid;
  gap: 24px;
}

.muted-text,
.reason-text {
  color: var(--text-muted);
  font-size: 12px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}
</style>
