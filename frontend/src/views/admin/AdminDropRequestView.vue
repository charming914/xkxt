<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>退课审核</h1>
        <p>管理员可以审核学生退课申请；教师和管理员均通过后，系统自动执行退课。</p>
      </div>
      <el-button @click="loadRequests">刷新申请</el-button>
    </div>

    <el-empty v-if="!loading && requests.length === 0" description="暂无退课申请" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border>
      <el-table-column prop="studentId" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="110" />
      <el-table-column prop="courseId" label="课程号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="teacherName" label="任课教师" width="110" />
      <el-table-column label="上课时间" min-width="180">
        <template #default="{ row }">
          周{{ weekLabel(row.weekDay) }} / {{ row.sectionDesc }} / {{ row.weekRange }}
        </template>
      </el-table-column>
      <el-table-column prop="requestReason" label="退课原因" min-width="180" />
      <el-table-column label="总状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="教师审核" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.teacherStatus)">{{ row.teacherStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="管理员审核" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.adminStatus)">{{ row.adminStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="action-row" v-if="row.requestStatus === '待审核' && row.adminStatus === '待审核'">
            <el-button size="small" type="primary" @click="openReviewDialog(row, 'APPROVE')">通过</el-button>
            <el-button size="small" type="danger" @click="openReviewDialog(row, 'REJECT')">驳回</el-button>
          </div>
          <span v-else>已处理</span>
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

    <el-dialog v-model="dialogVisible" :title="reviewAction === 'APPROVE' ? '通过退课申请' : '驳回退课申请'" width="520px">
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
import { getAdminCourseDropRequests, reviewAdminCourseDropRequest } from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'

const loading = ref(false)
const requests = ref([])
const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(requests, 10)
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
    const response = await getAdminCourseDropRequests()
    requests.value = response.data || []
  } finally {
    loading.value = false
  }
}

function openReviewDialog(row, action) {
  currentRequestId.value = row.dropRequestId
  reviewAction.value = action
  reviewComment.value = ''
  dialogVisible.value = true
}

async function handleReview() {
  await reviewAdminCourseDropRequest(currentRequestId.value, {
    action: reviewAction.value,
    reviewComment: reviewComment.value
  })
  ElMessage.success(reviewAction.value === 'APPROVE' ? '退课申请已通过' : '退课申请已驳回')
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>

