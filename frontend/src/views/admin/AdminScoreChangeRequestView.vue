<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>成绩修改审核</h1>
        <p>审核教师对已提交成绩的修改申请；通过后成绩会退回为可修改状态。</p>
      </div>
      <el-button @click="loadRequests">刷新列表</el-button>
    </div>

    <el-empty v-if="!loading && requests.length === 0" description="暂无成绩修改申请" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border>
        <el-table-column prop="scoreChangeRequestId" label="申请编号" width="100" />
        <el-table-column prop="teacherName" label="教师" width="110" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="originalUsualScore" label="原平时" width="90" />
        <el-table-column prop="originalExamScore" label="原考试" width="90" />
        <el-table-column prop="originalFinalScore" label="原总评" width="90" />
        <el-table-column prop="requestReason" label="申请原因" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核备注" min-width="160" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" type="primary" :disabled="row.requestStatus !== '待审核'" @click="openReview(row, 'APPROVE')">通过</el-button>
              <el-button size="small" type="danger" plain :disabled="row.requestStatus !== '待审核'" @click="openReview(row, 'REJECT')">驳回</el-button>
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

    <el-dialog v-model="reviewVisible" title="成绩修改审核" width="460px">
      <el-form label-position="top">
        <el-form-item label="审核意见">
          <el-input v-model.trim="reviewComment" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="handleReview">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminScoreChangeRequests, reviewAdminScoreChangeRequest } from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'

const loading = ref(false)
const reviewing = ref(false)
const requests = ref([])
const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(requests, 10)
const currentRow = ref(null)
const reviewAction = ref('')
const reviewComment = ref('')
const reviewVisible = ref(false)

function statusType(status) {
  if (status === '已通过') return 'success'
  if (status === '已驳回') return 'danger'
  return 'warning'
}

async function loadRequests() {
  loading.value = true
  try {
    const response = await getAdminScoreChangeRequests()
    requests.value = response.data || []
  } finally {
    loading.value = false
  }
}

function openReview(row, action) {
  currentRow.value = row
  reviewAction.value = action
  reviewComment.value = ''
  reviewVisible.value = true
}

async function handleReview() {
  reviewing.value = true
  try {
    await reviewAdminScoreChangeRequest(currentRow.value.scoreChangeRequestId, {
      action: reviewAction.value,
      reviewComment: reviewComment.value
    })
    ElMessage.success('成绩修改申请处理成功')
    reviewVisible.value = false
    await loadRequests()
  } finally {
    reviewing.value = false
  }
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
