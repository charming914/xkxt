<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>退课</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadAll">刷新</el-button>
    </div>

    <el-alert
      title="新学期未设置开课时间或课程尚未开课时可直接退课；开课一周内可直接退课；开课三周内但超过一周需提交退课申请；超过三周不可退课。"
      type="info"
      :closable="false"
      class="tip-alert"
    />

    <div class="sub-header">
      <h2>当前已选课程</h2>
    </div>
    <el-empty v-if="!loading && courses.length === 0" description="当前暂无可退课程" />
    <el-table v-else v-loading="loading" :data="courses" border>
      <el-table-column prop="courseId" label="课程号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="courseType" label="课程种类" width="120" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="teacherName" label="任课教师" width="110" />
      <el-table-column label="上课时间" min-width="180">
        <template #default="{ row }">
          {{ row.scheduleText || '待排课' }}（{{ row.weekRange }}）
        </template>
      </el-table-column>
      <el-table-column prop="courseStartDate" label="开课日期" width="120" />
      <el-table-column label="退课状态" width="150">
        <template #default="{ row }">
          <el-tag :type="dropTagType(row)">
            {{ row.hasPendingDropRequest ? '申请审核中' : row.dropModeLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="Number(row.canDropDirectly) === 1"
            type="danger"
            size="small"
            :loading="submittingId === row.teachingClassId"
            @click="handleDrop(row)"
          >
            直接退课
          </el-button>
          <el-button
            v-else-if="Number(row.canRequestDrop) === 1"
            type="primary"
            size="small"
            :disabled="Number(row.hasPendingDropRequest) === 1"
            @click="openRequestDialog(row)"
          >
            申请退课
          </el-button>
          <el-button v-else size="small" disabled>不可退课</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="request-section">
      <div class="sub-header">
        <h2>我的退课申请</h2>
      </div>
      <el-empty v-if="!requestLoading && requests.length === 0" description="暂无退课申请记录" />
      <el-table v-else v-loading="requestLoading" :data="requests" border>
        <el-table-column prop="courseId" label="课程号" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="teacherName" label="任课教师" width="110" />
        <el-table-column prop="requestReason" label="退课原因" min-width="160" />
        <el-table-column label="申请状态" width="110">
          <template #default="{ row }">
            <el-tag :type="requestStatusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="教师审核" width="110">
          <template #default="{ row }">
            <el-tag :type="requestStatusType(row.teacherStatus)">{{ row.teacherStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="管理员审核" width="120">
          <template #default="{ row }">
            <el-tag :type="requestStatusType(row.adminStatus)">{{ row.adminStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" min-width="170" />
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="提交退课申请" width="520px">
      <el-form label-position="top">
        <el-form-item label="课程">
          <el-input :model-value="selectedCourseLabel" disabled />
        </el-form-item>
        <el-form-item label="退课原因">
          <el-input
            v-model="requestReason"
            type="textarea"
            :rows="4"
            placeholder="请说明退课原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateRequest">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCourseDropRequest,
  dropCourse,
  getCourseDropRequests,
  getDropCourses
} from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const requestLoading = ref(false)
const submittingId = ref(null)
const courses = ref([])
const requests = ref([])
const dialogVisible = ref(false)
const selectedCourse = ref(null)
const requestReason = ref('')

const selectedCourseLabel = computed(() => {
  if (!selectedCourse.value) return ''
  return `${selectedCourse.value.courseName}（教学班 ${selectedCourse.value.teachingClassId}）`
})

function weekLabel(weekDay) {
  const map = {
    1: '一',
    2: '二',
    3: '三',
    4: '四',
    5: '五',
    6: '六',
    7: '日'
  }
  return map[weekDay] || weekDay
}

function dropTagType(row) {
  if (Number(row.hasPendingDropRequest) === 1) return 'warning'
  if (row.dropMode === 'DIRECT') return 'success'
  if (row.dropMode === 'REQUEST') return 'warning'
  return 'danger'
}

function requestStatusType(status) {
  if (status === '已通过') return 'success'
  if (status === '已驳回') return 'danger'
  return 'warning'
}

async function loadCourses() {
  loading.value = true
  try {
    const response = await getDropCourses()
    courses.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadRequests() {
  requestLoading.value = true
  try {
    const response = await getCourseDropRequests()
    requests.value = response.data || []
  } finally {
    requestLoading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadCourses(), loadRequests()])
}

async function handleDrop(row) {
  try {
    await ElMessageBox.confirm(`确认退选 ${row.courseName} 吗？`, '确认退课', {
      type: 'warning',
      confirmButtonText: '确认退课',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  submittingId.value = row.teachingClassId
  try {
    const response = await dropCourse(row.teachingClassId)
    ElMessage.success(response.message || '退课成功')
    await loadAll()
  } finally {
    submittingId.value = null
  }
}

function openRequestDialog(row) {
  selectedCourse.value = row
  requestReason.value = ''
  dialogVisible.value = true
}

async function handleCreateRequest() {
  if (!selectedCourse.value) {
    return
  }
  if (!requestReason.value.trim()) {
    ElMessage.warning('请填写退课原因')
    return
  }
  const response = await createCourseDropRequest({
    teachingClassId: selectedCourse.value.teachingClassId,
    requestReason: requestReason.value.trim()
  })
  ElMessage.success(response.message || '退课申请提交成功')
  dialogVisible.value = false
  await loadAll()
}

onMounted(() => {
  loadAll()
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

.sub-header h2 {
  margin: 0;
}

.tip-alert {
  margin-bottom: 16px;
}

.request-section {
  margin-top: 28px;
}
</style>
