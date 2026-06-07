<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>补考重修审核</h1>
        <p>审核学生补考和重修申请，并登记补考结果。</p>
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
          <el-table-column prop="finalScore" label="原成绩" width="90" />
          <el-table-column prop="requestReason" label="申请原因" min-width="170" />
          <el-table-column label="审核状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
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
                <el-button size="small" type="primary" :disabled="row.requestStatus !== '待审核'" @click="openReview(row, 'MAKEUP', 'APPROVE')">
                  通过
                </el-button>
                <el-button size="small" type="danger" plain :disabled="row.requestStatus !== '待审核'" @click="openReview(row, 'MAKEUP', 'REJECT')">
                  驳回
                </el-button>
                <el-button size="small" :disabled="row.requestStatus !== '已通过'" @click="openResult(row)">
                  登记结果
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="重修申请" name="retake">
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
        <el-form-item label="审核备注">
          <el-input v-model.trim="reviewComment" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item v-if="reviewKind === 'MAKEUP' && reviewAction === 'APPROVE'" label="初始补考状态">
          <el-select v-model="resultStatus">
            <el-option label="待考试" value="待考试" />
            <el-option label="已通过" value="已通过" />
            <el-option label="未通过" value="未通过" />
          </el-select>
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
const reviewVisible = ref(false)
const resultVisible = ref(false)
const currentRow = ref(null)
const reviewKind = ref('MAKEUP')
const reviewAction = ref('APPROVE')
const reviewComment = ref('')
const resultStatus = ref('待考试')

const reviewTitle = computed(() => {
  const typeLabel = reviewKind.value === 'MAKEUP' ? '补考申请' : '重修申请'
  const actionLabel = reviewAction.value === 'APPROVE' ? '通过' : '驳回'
  return `${actionLabel}${typeLabel}`
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
  reviewing.value = true
  try {
    const payload = {
      action: reviewAction.value,
      reviewComment: reviewComment.value,
      resultStatus: resultStatus.value
    }
    if (reviewKind.value === 'MAKEUP') {
      await reviewAdminMakeupRequest(currentRow.value.requestId, payload)
      ElMessage.success('补考申请处理成功')
    } else {
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
