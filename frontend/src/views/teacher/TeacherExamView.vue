<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>考试安排</h1>
        <p>查看本学期本人监考课程的考试安排，可提出时间或地点修改申请，经管理员审核后生效。</p>
      </div>
      <el-button @click="loadData">刷新</el-button>
    </div>

    <el-empty v-if="!loading && exams.length === 0" description="本学期暂无考试安排" />
    <el-table v-else v-loading="loading" :data="exams" border>
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="teachingClassId" label="教学班" width="90" />
      <el-table-column label="考试时间" min-width="210">
        <template #default="{ row }">
          {{ formatDate(row.examDate) }} {{ formatTimeRange(row.startTime, row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="考试地点" min-width="160">
        <template #default="{ row }">
          {{ row.buildingName ? `${row.buildingName} ${row.roomName || ''}` : '未指定' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.examStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openRequest(row)">申请修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <h2 class="section-title">我的考试修改申请</h2>
    <el-empty v-if="!loading && requests.length === 0" description="暂无考试修改申请" />
    <el-table v-else v-loading="loading" :data="requests" border>
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="teachingClassId" label="教学班" width="90" />
      <el-table-column label="原安排" min-width="210">
        <template #default="{ row }">
          {{ formatDate(row.originalExamDate) }} {{ formatTimeRange(row.originalStartTime, row.originalEndTime) }}
          {{ row.originalBuildingName || '-' }} {{ row.originalRoomName || '' }}
        </template>
      </el-table-column>
      <el-table-column label="申请调整为" min-width="220">
        <template #default="{ row }">
          {{ formatDate(row.requestedExamDate) }} {{ formatTimeRange(row.requestedStartTime, row.requestedEndTime) }}
          {{ row.requestedBuildingName || '-' }} {{ row.requestedRoomName || '' }}
        </template>
      </el-table-column>
      <el-table-column prop="requestReason" label="申请原因" min-width="180" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.requestStatus)">{{ row.requestStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reviewComment" label="审核备注" min-width="160" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="danger"
            plain
            :disabled="row.requestStatus !== '待审核'"
            :loading="cancelingId === row.examChangeRequestId"
            @click="handleCancel(row)"
          >
            撤销
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="requestVisible" title="申请修改考试安排" width="460px">
      <el-form label-position="top" :model="form">
        <el-form-item label="课程">
          <el-input :model-value="currentExam?.courseName" disabled />
        </el-form-item>
        <el-form-item label="考试日期">
          <el-date-picker
            v-model="form.requestedExamDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择考试日期"
            style="width: 100%"
            @change="loadAvailableExamClassrooms"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker
            v-model="form.requestedStartTime"
            value-format="HH:mm:ss"
            placeholder="开始时间"
            style="width: 100%"
            @change="handleStartTimeChange"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker
            v-model="form.requestedEndTime"
            value-format="HH:mm:ss"
            placeholder="结束时间"
            style="width: 100%"
            @change="loadAvailableExamClassrooms"
          />
        </el-form-item>
        <el-form-item label="考试教室">
          <el-select
            v-model="form.requestedClassroomId"
            clearable
            filterable
            :loading="classroomLoading"
            placeholder="请选择当前时间可用教室（可留空）"
            no-data-text="当前时间无可用教室"
          >
            <el-option
              v-for="item in classroomOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model.trim="form.requestReason" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="requestVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelTeacherExamChangeRequest,
  createTeacherExamChangeRequest,
  getTeacherAvailableExamClassroomOptions,
  getTeacherExamChangeRequests,
  getTeacherExams
} from '../../api/teacher'

const loading = ref(false)
const saving = ref(false)
const classroomLoading = ref(false)
const cancelingId = ref(null)
const exams = ref([])
const requests = ref([])
const classroomOptions = ref([])
const requestVisible = ref(false)
const currentExam = ref(null)
const form = reactive({
  examId: null,
  requestedExamDate: '',
  requestedStartTime: '',
  requestedEndTime: '',
  requestedClassroomId: '',
  requestReason: ''
})

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function formatTimeRange(start, end) {
  if (!start || !end) return ''
  return `${String(start).slice(0, 5)}-${String(end).slice(0, 5)}`
}

function statusType(status) {
  if (status === '已通过') {
    return 'success'
  }
  if (status === '已驳回') {
    return 'danger'
  }
  if (status === '已撤销') {
    return 'info'
  }
  return 'warning'
}

async function loadData() {
  loading.value = true
  try {
    const [examResponse, requestResponse] = await Promise.all([
      getTeacherExams(),
      getTeacherExamChangeRequests()
    ])
    exams.value = examResponse.data || []
    requests.value = requestResponse.data || []
  } finally {
    loading.value = false
  }
}

async function loadAvailableExamClassrooms() {
  if (!form.examId || !form.requestedExamDate || !form.requestedStartTime || !form.requestedEndTime) {
    classroomOptions.value = []
    form.requestedClassroomId = ''
    return
  }
  if (form.requestedStartTime >= form.requestedEndTime) {
    classroomOptions.value = []
    form.requestedClassroomId = ''
    return
  }
  classroomLoading.value = true
  try {
    const response = await getTeacherAvailableExamClassroomOptions({
      examId: form.examId,
      examDate: form.requestedExamDate,
      startTime: form.requestedStartTime,
      endTime: form.requestedEndTime
    })
    classroomOptions.value = response.data || []
    if (form.requestedClassroomId && !classroomOptions.value.some(item => item.value === form.requestedClassroomId)) {
      form.requestedClassroomId = ''
    }
  } catch {
    classroomOptions.value = []
    form.requestedClassroomId = ''
  } finally {
    classroomLoading.value = false
  }
}

function addHoursToTime(time, hours) {
  if (!time) {
    return ''
  }
  const parts = String(time).split(':').map(item => Number(item))
  if (parts.length < 2 || parts.some(item => Number.isNaN(item))) {
    return ''
  }
  const totalMinutes = (parts[0] * 60 + parts[1] + hours * 60) % (24 * 60)
  const nextHour = String(Math.floor(totalMinutes / 60)).padStart(2, '0')
  const nextMinute = String(totalMinutes % 60).padStart(2, '0')
  const second = String(parts[2] || 0).padStart(2, '0')
  return `${nextHour}:${nextMinute}:${second}`
}

async function handleStartTimeChange() {
  if (form.requestedStartTime) {
    form.requestedEndTime = addHoursToTime(form.requestedStartTime, 2)
  }
  await loadAvailableExamClassrooms()
}

async function openRequest(row) {
  currentExam.value = row
  form.examId = row.examId
  form.requestedExamDate = formatDate(row.examDate)
  form.requestedStartTime = row.startTime ? String(row.startTime).slice(0, 8) : ''
  form.requestedEndTime = row.endTime ? String(row.endTime).slice(0, 8) : ''
  form.requestedClassroomId = row.classroomId || ''
  form.requestReason = ''
  requestVisible.value = true
  await loadAvailableExamClassrooms()
}

async function handleSubmit() {
  if (!form.requestedExamDate || !form.requestedStartTime || !form.requestedEndTime || !form.requestReason) {
    ElMessage.warning('请完整填写考试日期、时间与申请原因')
    return
  }
  if (form.requestedStartTime >= form.requestedEndTime) {
    ElMessage.warning('开始时间必须早于结束时间')
    return
  }
  saving.value = true
  try {
    await createTeacherExamChangeRequest({
      examId: form.examId,
      requestedExamDate: form.requestedExamDate,
      requestedStartTime: form.requestedStartTime,
      requestedEndTime: form.requestedEndTime,
      requestedClassroomId: form.requestedClassroomId || '',
      requestReason: form.requestReason
    })
    ElMessage.success('考试修改申请提交成功')
    requestVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function handleCancel(row) {
  if (row.requestStatus !== '待审核') {
    return
  }
  await ElMessageBox.confirm('确认撤销该考试修改申请？', '撤销确认', {
    confirmButtonText: '撤销',
    cancelButtonText: '取消',
    type: 'warning'
  })
  cancelingId.value = row.examChangeRequestId
  try {
    await cancelTeacherExamChangeRequest(row.examChangeRequestId)
    ElMessage.success('考试修改申请已撤销')
    await loadData()
  } finally {
    cancelingId.value = null
  }
}

onMounted(() => {
  loadData()
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

.section-title {
  margin: 26px 0 14px;
  font-size: 16px;
  font-weight: 600;
}
</style>
