<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>补考重修审核</h1>
        <p>安排教师已确认的补考教室，并登记补考结果；重修由教学班管理开放重修班。</p>
      </div>
      <el-button @click="refreshAll">刷新列表</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="补考申请" name="makeup">
        <el-empty v-if="!makeupLoading && makeupRequests.length === 0" description="暂无补考申请" />
        <el-table v-else v-loading="makeupLoading" :data="makeupRequests" border>
          <el-table-column prop="requestId" label="编号" width="90" />
          <el-table-column prop="studentId" label="学号" width="120" />
          <el-table-column prop="studentName" label="学生" width="100" />
          <el-table-column prop="semesterName" label="学期" min-width="120" />
          <el-table-column prop="courseId" label="课程号" width="120" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column prop="teacherName" label="任课教师" width="110" />
          <el-table-column prop="finalScore" label="原成绩" width="90" />
          <el-table-column prop="requestReason" label="申请原因" min-width="170" />
          <el-table-column prop="requestedExamTime" label="学生申请时间" min-width="170" />
          <el-table-column label="教师确认" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.teacherStatus)">{{ row.teacherStatus || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="confirmedExamTime" label="确认补考时间" min-width="170" />
          <el-table-column label="教室状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.adminStatus)">{{ row.adminStatus || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="补考教室" min-width="130">
            <template #default="{ row }">
              {{ classroomLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="补考结果" width="120">
            <template #default="{ row }">
              <el-tag :type="makeupResultType(row.resultStatus)">{{ row.resultStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewComment" label="审核备注" min-width="150" />
          <el-table-column prop="createdAt" label="申请时间" min-width="170" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <div class="action-row">
                <el-button size="small" type="primary" :disabled="row.requestStatus !== '待管理员排教室'" @click="openArrange(row, 'APPROVE')">
                  安排教室
                </el-button>
                <el-button size="small" type="danger" plain :disabled="row.requestStatus !== '待管理员排教室'" @click="openArrange(row, 'REJECT')">
                  驳回
                </el-button>
                <el-button size="small" :disabled="row.requestStatus !== '已安排'" @click="openResult(row)">
                  登记结果
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="历史重修申请" name="retake">
        <el-empty v-if="!retakeLoading && retakeRequests.length === 0" description="暂无重修申请" />
        <el-table v-else v-loading="retakeLoading" :data="retakeRequests" border>
          <el-table-column prop="requestId" label="编号" width="90" />
          <el-table-column prop="studentId" label="学号" width="120" />
          <el-table-column prop="studentName" label="学生" width="100" />
          <el-table-column prop="semesterName" label="学期" min-width="120" />
          <el-table-column prop="courseId" label="课程号" width="120" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column prop="finalScore" label="原成绩" width="90" />
          <el-table-column prop="requestReason" label="申请原因" min-width="170" />
          <el-table-column label="审核状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewComment" label="审核备注" min-width="150" />
          <el-table-column prop="createdAt" label="申请时间" min-width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="action-row">
                <el-button size="small" type="primary" :disabled="row.requestStatus !== '待审核'" @click="openReview(row, 'RETAKE', 'APPROVE')">
                  通过
                </el-button>
                <el-button size="small" type="danger" plain :disabled="row.requestStatus !== '待审核'" @click="openReview(row, 'RETAKE', 'REJECT')">
                  驳回
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="reviewVisible" :title="reviewTitle" width="500px">
      <el-form label-position="top">
        <template v-if="reviewKind === 'MAKEUP'">
          <el-form-item label="确认补考时间">
            <el-input :model-value="currentRow?.confirmedExamTime || '-'" disabled />
          </el-form-item>
          <el-form-item v-if="reviewAction === 'APPROVE'" label="补考教室">
            <el-select v-model="classroomId" filterable clearable placeholder="请选择空余教室">
              <el-option v-for="item in classrooms" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item :label="reviewKind === 'MAKEUP' ? '处理备注' : '审核备注'">
          <el-input v-model.trim="reviewComment" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item v-if="reviewKind === 'RETAKE'" label="重修说明">
          <el-alert title="新流程下重修由教学班管理开放重修班，学生直接选课；此处仅保留历史申请处理。" type="info" :closable="false" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="handleReview">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resultVisible" title="登记补考结果" width="420px">
      <el-form label-position="top">
        <el-form-item label="补考结果">
          <el-select v-model="resultStatus">
            <el-option label="待考试" value="待考试" />
            <el-option label="已通过" value="已通过" />
            <el-option label="未通过" value="未通过" />
            <el-option label="未安排" value="未安排" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="handleUpdateResult">保存结果</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getClassroomOptions,
  getAdminMakeupRequests,
  getAdminRetakeRequests,
  reviewAdminMakeupRequest,
  reviewAdminRetakeRequest,
  updateAdminMakeupResult
} from '../../api/admin'

const activeTab = ref('makeup')
const makeupLoading = ref(false)
const retakeLoading = ref(false)
const reviewing = ref(false)
const makeupRequests = ref([])
const retakeRequests = ref([])
const classrooms = ref([])
const reviewVisible = ref(false)
const resultVisible = ref(false)
const currentRow = ref(null)
const reviewKind = ref('MAKEUP')
const reviewAction = ref('APPROVE')
const reviewComment = ref('')
const resultStatus = ref('待考试')
const classroomId = ref('')

const reviewTitle = computed(() => {
  if (reviewKind.value === 'MAKEUP') {
    return reviewAction.value === 'APPROVE' ? '安排补考教室' : '驳回补考安排'
  }
  const actionLabel = reviewAction.value === 'APPROVE' ? '通过' : '驳回'
  return `${actionLabel}历史重修申请`
})

function statusType(status) {
  if (['已通过', '已确认', '已安排'].includes(status)) return 'success'
  if (status === '已驳回') return 'danger'
  if (status === '待安排') return 'info'
  return 'warning'
}

function makeupResultType(status) {
  if (status === '已通过') return 'success'
  if (status === '未通过') return 'danger'
  if (status === '未安排') return 'info'
  return 'warning'
}

function classroomLabel(row) {
  return [row.buildingName, row.roomName].filter(Boolean).join(' ') || '-'
}

async function loadOptions() {
  const response = await getClassroomOptions()
  classrooms.value = response.data || []
}

async function loadMakeupRequests() {
  makeupLoading.value = true
  try {
    const response = await getAdminMakeupRequests()
    makeupRequests.value = response.data || []
  } finally {
    makeupLoading.value = false
  }
}

async function loadRetakeRequests() {
  retakeLoading.value = true
  try {
    const response = await getAdminRetakeRequests()
    retakeRequests.value = response.data || []
  } finally {
    retakeLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadMakeupRequests(), loadRetakeRequests()])
}

function openReview(row, kind, action) {
  currentRow.value = row
  reviewKind.value = kind
  reviewAction.value = action
  reviewComment.value = ''
  resultStatus.value = action === 'APPROVE' ? '待考试' : '未安排'
  classroomId.value = ''
  reviewVisible.value = true
}

function openArrange(row, action) {
  currentRow.value = row
  reviewKind.value = 'MAKEUP'
  reviewAction.value = action
  reviewComment.value = ''
  classroomId.value = row.classroomId || ''
  resultStatus.value = '待考试'
  reviewVisible.value = true
}

function openResult(row) {
  currentRow.value = row
  resultStatus.value = row.resultStatus || '待考试'
  resultVisible.value = true
}

async function handleReview() {
  if (!currentRow.value) {
    return
  }
  if (reviewKind.value === 'MAKEUP' && reviewAction.value === 'APPROVE' && !classroomId.value) {
    ElMessage.warning('请选择补考教室')
    return
  }
  if (reviewKind.value === 'MAKEUP' && reviewAction.value === 'REJECT' && !reviewComment.value) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  reviewing.value = true
  try {
    const payload = {
      action: reviewAction.value,
      reviewComment: reviewComment.value
    }
    if (reviewKind.value === 'MAKEUP') {
      payload.classroomId = classroomId.value
      await reviewAdminMakeupRequest(currentRow.value.requestId, payload)
      ElMessage.success(reviewAction.value === 'APPROVE' ? '补考教室安排成功' : '补考安排已驳回')
    } else {
      payload.resultStatus = resultStatus.value
      await reviewAdminRetakeRequest(currentRow.value.requestId, payload)
      ElMessage.success('重修申请处理成功')
    }
    reviewVisible.value = false
    await refreshAll()
  } finally {
    reviewing.value = false
  }
}

async function handleUpdateResult() {
  if (!currentRow.value) {
    return
  }
  reviewing.value = true
  try {
    await updateAdminMakeupResult(currentRow.value.requestId, {
      action: 'RESULT',
      resultStatus: resultStatus.value
    })
    ElMessage.success('补考结果登记成功')
    resultVisible.value = false
    await loadMakeupRequests()
  } finally {
    reviewing.value = false
  }
}

onMounted(() => {
  loadOptions()
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

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.el-select) {
  width: 100%;
}
</style>
