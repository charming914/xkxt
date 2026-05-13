<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学生选课</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadCourses">刷新课程</el-button>
    </div>

    <el-alert
      title="系统已启用时间冲突检查，若课程与已选课表冲突，将无法直接选课。"
      type="info"
      :closable="false"
      class="tip-alert"
    />

    <el-alert
      v-if="selectionHint"
      :title="selectionHint"
      type="warning"
      :closable="false"
      class="tip-alert"
    />

    <el-table v-loading="loading" :data="courses" border>
      <el-table-column prop="teachingClassId" label="教学班编号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="teacherName" label="任课教师" width="120" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="totalHours" label="学时" width="80" />
      <el-table-column label="上课时间" min-width="180">
        <template #default="{ row }">
          周{{ weekLabel(row.weekDay) }} / {{ row.sectionDesc }} / {{ row.weekRange }}
        </template>
      </el-table-column>
      <el-table-column label="上课地点" min-width="150">
        <template #default="{ row }">
          {{ row.buildingName || '-' }} {{ row.roomName || '' }}
        </template>
      </el-table-column>
      <el-table-column label="人数" width="120">
        <template #default="{ row }">
          {{ row.selectedCount }}/{{ row.maxStudents }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="action-row">
            <el-button
              type="primary"
              size="small"
              :loading="submittingId === row.teachingClassId"
              @click="handleSelect(row)"
            >
              选课
            </el-button>
            <el-button size="small" @click="openRequestDialog(row)">申请补选</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="request-section">
      <div class="sub-header">
        <h2>我的补选申请</h2>
        <el-button @click="loadRequests">刷新申请记录</el-button>
      </div>
      <el-empty v-if="!requestLoading && requests.length === 0" description="暂无补选申请记录" />
      <el-table v-else v-loading="requestLoading" :data="requests" border>
        <el-table-column prop="semesterName" label="学期" min-width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="teacherName" label="任课教师" width="110" />
        <el-table-column prop="requestReason" label="申请原因" min-width="160" />
        <el-table-column label="申请状态" width="110">
          <template #default="{ row }">
            <el-tag :type="requestStatusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核备注" min-width="160" />
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="提交补选申请" width="520px">
      <el-form label-position="top">
        <el-form-item label="课程">
          <el-input :model-value="selectedCourseLabel" disabled />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input
            v-model="requestReason"
            type="textarea"
            :rows="4"
            placeholder="请说明补选该课程的原因"
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
import { ElMessage } from 'element-plus'
import {
  createCourseSelectionRequest,
  getAvailableCourses,
  getCourseSelectionRequests,
  selectCourse
} from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submittingId = ref(null)
const courses = ref([])
const selectionHint = ref('')
const requests = ref([])
const requestLoading = ref(false)
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

async function loadCourses() {
  loading.value = true
  try {
    const response = await getAvailableCourses()
    courses.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadRequests() {
  requestLoading.value = true
  try {
    const response = await getCourseSelectionRequests()
    requests.value = response.data || []
  } finally {
    requestLoading.value = false
  }
}

function requestStatusType(status) {
  if (status === '已通过') return 'success'
  if (status === '已驳回') return 'danger'
  return 'warning'
}

async function handleSelect(row) {
  if (!userStore.refId) {
    ElMessage.warning('当前未识别学生身份')
    return
  }
  submittingId.value = row.teachingClassId
  selectionHint.value = ''
  try {
    const response = await selectCourse({
      studentId: userStore.refId,
      teachingClassId: row.teachingClassId
    })
    ElMessage.success(response.message || '选课成功')
    await loadCourses()
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || ''
    if (message.includes('时间冲突')) {
      selectionHint.value = '选课失败：该课程与已选课表存在时间冲突，请先查看课表后再选择。'
    } else if (message.includes('人数已满')) {
      selectionHint.value = '选课失败：该教学班人数已满。你可以改为提交补选申请，由管理员审核处理。'
    }
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
    ElMessage.warning('请填写补选申请原因')
    return
  }
  const response = await createCourseSelectionRequest({
    studentId: userStore.refId,
    teachingClassId: selectedCourse.value.teachingClassId,
    requestReason: requestReason.value.trim()
  })
  ElMessage.success(response.message || '补选申请提交成功')
  dialogVisible.value = false
  await loadRequests()
}

onMounted(() => {
  loadCourses()
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

.tip-alert {
  margin-bottom: 16px;
}

.action-row {
  display: flex;
  gap: 8px;
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
