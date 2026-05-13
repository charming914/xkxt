<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>补选审核</h1>
        <p>管理员可以审核学生补选申请，并在无时间冲突时进行代选。</p>
      </div>
      <el-button @click="loadRequests">刷新申请</el-button>
    </div>

    <el-empty v-if="!loading && requests.length === 0" description="暂无补选申请" />

    <el-table v-else v-loading="loading" :data="requests" border>
      <el-table-column prop="studentId" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="110" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="teacherName" label="任课教师" width="110" />
      <el-table-column label="上课时间" min-width="180">
        <template #default="{ row }">
          周{{ weekLabel(row.weekDay) }} / {{ row.sectionDesc }} / {{ row.weekRange }}
        </template>
      </el-table-column>
      <el-table-column prop="requestReason" label="申请原因" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reviewComment" label="审核备注" min-width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="action-row" v-if="row.requestStatus === '待审核'">
            <el-button size="small" type="primary" @click="openReviewDialog(row, 'APPROVE')">通过</el-button>
            <el-button size="small" type="danger" @click="openReviewDialog(row, 'REJECT')">驳回</el-button>
          </div>
          <span v-else>已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="reviewAction === 'APPROVE' ? '通过补选申请' : '驳回补选申请'" width="520px">
      <el-form label-position="top">
        <el-form-item label="审核备注">
          <el-input v-model="reviewComment" type="textarea" :rows="4" placeholder="可填写审核意见" />
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
import { getAdminCourseSelectionRequests, reviewAdminCourseSelectionRequest } from '../../api/admin'

const loading = ref(false)
const requests = ref([])
const dialogVisible = ref(false)
const currentRequestId = ref(null)
const reviewAction = ref('APPROVE')
const reviewComment = ref('')

function weekLabel(weekDay) {
  const map = { 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六', 7: '日' }
  return map[weekDay] || weekDay
}

function statusTagType(status) {
  if (status === '已通过') return 'success'
  if (status === '已驳回') return 'danger'
  return 'warning'
}

async function loadRequests() {
  loading.value = true
  try {
    const response = await getAdminCourseSelectionRequests()
    requests.value = response.data || []
  } finally {
    loading.value = false
  }
}

function openReviewDialog(row, action) {
  currentRequestId.value = row.requestId
  reviewAction.value = action
  reviewComment.value = ''
  dialogVisible.value = true
}

async function handleReview() {
  await reviewAdminCourseSelectionRequest(currentRequestId.value, {
    action: reviewAction.value,
    reviewComment: reviewComment.value
  })
  ElMessage.success(reviewAction.value === 'APPROVE' ? '补选申请已通过' : '补选申请已驳回')
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
