<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>补考确认</h1>
        <p>当前登录教师：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadRequests">刷新申请</el-button>
    </div>

    <el-empty v-if="!loading && requests.length === 0" description="暂无补考申请" />

    <el-table v-else v-loading="loading" :data="requests" border>
      <el-table-column prop="studentId" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="110" />
      <el-table-column prop="courseId" label="课程号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="finalScore" label="原成绩" width="90" />
      <el-table-column prop="requestReason" label="申请原因" min-width="180" />
      <el-table-column prop="requestedExamTime" label="学生申请时间" min-width="170" />
      <el-table-column label="教师状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.teacherStatus)">{{ row.teacherStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="管理员状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.adminStatus)">{{ row.adminStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="补考结果" width="110">
        <template #default="{ row }">
          <el-tag :type="resultTagType(row.resultStatus)">{{ row.resultStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <div v-if="row.requestStatus === '待教师确认' && row.teacherStatus === '待确认'" class="action-row">
            <el-button size="small" type="primary" @click="openReviewDialog(row, 'APPROVE')">确认时间</el-button>
            <el-button size="small" type="danger" plain @click="openReviewDialog(row, 'REJECT')">驳回</el-button>
          </div>
          <span v-else>已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="reviewAction === 'APPROVE' ? '确认补考时间' : '驳回补考申请'" width="520px">
      <el-form label-position="top">
        <el-form-item label="学生申请时间">
          <el-input :model-value="currentRequestedExamTime" disabled />
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input
            v-model.trim="reviewComment"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            :placeholder="reviewAction === 'APPROVE' ? '可填写确认说明' : '请填写驳回理由'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="reviewAction === 'APPROVE' ? 'primary' : 'danger'" @click="handleReview">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeacherMakeupRequests, reviewTeacherMakeupRequest } from '../../api/teacher'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const requests = ref([])
const dialogVisible = ref(false)
const currentRequestId = ref(null)
const currentRequestedExamTime = ref('')
const reviewAction = ref('APPROVE')
const reviewComment = ref('')

function statusTagType(status) {
  if (['已确认', '已安排', '已通过'].includes(status)) return 'success'
  if (status === '已驳回') return 'danger'
  if (status === '待安排') return 'info'
  return 'warning'
}

function resultTagType(status) {
  if (status === '已通过') return 'success'
  if (status === '未通过') return 'danger'
  if (status === '未安排') return 'info'
  return 'warning'
}

async function loadRequests() {
  loading.value = true
  try {
    const response = await getTeacherMakeupRequests()
    requests.value = response.data || []
  } finally {
    loading.value = false
  }
}

function openReviewDialog(row, action) {
  currentRequestId.value = row.requestId
  currentRequestedExamTime.value = row.requestedExamTime || ''
  reviewAction.value = action
  reviewComment.value = ''
  dialogVisible.value = true
}

async function handleReview() {
  if (reviewAction.value === 'REJECT' && !reviewComment.value) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  await reviewTeacherMakeupRequest(currentRequestId.value, {
    action: reviewAction.value,
    reviewComment: reviewComment.value
  })
  ElMessage.success(reviewAction.value === 'APPROVE' ? '补考时间已确认' : '补考申请已驳回')
  dialogVisible.value = false
  await loadRequests()
}

onMounted(() => {
  loadRequests()
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
  gap: 8px;
}
</style>
