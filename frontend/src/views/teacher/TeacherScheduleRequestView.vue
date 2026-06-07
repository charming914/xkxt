<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>调课申请</h1>
        <p>提交本人教学班的换时间、换教室申请，并查看审核状态。</p>
      </div>
      <el-button @click="loadData">刷新</el-button>
    </div>

    <el-form class="request-form" label-position="top" :model="form">
      <div class="form-grid">
        <el-form-item label="教学班">
          <el-select v-model="form.teachingClassId" placeholder="请选择教学班" filterable @change="handleClassChange">
            <el-option
              v-for="item in classOptions"
              :key="item.teachingClassId"
              :label="`${item.teachingClassId} - ${item.courseName}`"
              :value="item.teachingClassId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调整后星期">
          <el-select v-model="form.requestedWeekDay" placeholder="请选择星期" @change="loadAvailableClassrooms">
            <el-option v-for="item in weekDays" :key="item.value" :label="`周${item.label}`" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整后节次">
          <el-select v-model="form.requestedSectionDesc" placeholder="请选择节次" @change="loadAvailableClassrooms">
            <el-option
              v-for="item in sectionOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调整后周次">
          <el-input v-model="form.requestedWeekRange" disabled />
        </el-form-item>
        <el-form-item label="调整后教室">
          <el-select
            v-model="form.requestedClassroomId"
            clearable
            filterable
            :loading="classroomLoading"
            placeholder="请选择该时间段空教室"
          >
            <el-option
              v-for="item in classroomOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请原因" class="reason-field">
          <el-input v-model.trim="form.requestReason" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </div>
      <div class="action-row">
        <el-button @click="resetForm">重置</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">提交申请</el-button>
      </div>
    </el-form>

    <el-empty v-if="!loading && requests.length === 0" description="暂无调课申请" />
    <el-table v-else v-loading="loading" :data="requests" border>
      <el-table-column prop="teachingClassId" label="教学班" width="100" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column label="原安排" min-width="210">
        <template #default="{ row }">
          周{{ formatWeekDay(row.originalWeekDay) }} {{ row.originalSectionDesc }} {{ row.originalWeekRange }}
          {{ row.originalBuildingName || '-' }} {{ row.originalRoomName || '' }}
        </template>
      </el-table-column>
      <el-table-column label="申请调整为" min-width="220">
        <template #default="{ row }">
          周{{ formatWeekDay(row.requestedWeekDay) }} {{ row.requestedSectionDesc }} {{ row.requestedWeekRange }}
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
    </el-table>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createTeacherScheduleAdjustRequest,
  getTeacherAvailableClassroomOptions,
  getTeacherScheduleAdjustRequests,
  getTeachingClasses
} from '../../api/teacher'

const loading = ref(false)
const saving = ref(false)
const classroomLoading = ref(false)
const requests = ref([])
const classOptions = ref([])
const classroomOptions = ref([])
const weekDays = [
  { value: 1, label: '一' },
  { value: 2, label: '二' },
  { value: 3, label: '三' },
  { value: 4, label: '四' },
  { value: 5, label: '五' },
  { value: 6, label: '六' },
  { value: 7, label: '日' }
]
const form = reactive({
  teachingClassId: '',
  requestedWeekDay: '',
  requestedSectionDesc: '',
  requestedWeekRange: '',
  requestedClassroomId: '',
  requestReason: ''
})

const selectedClass = computed(() => {
  return classOptions.value.find(item => item.teachingClassId === form.teachingClassId)
})

const sectionOptions = computed(() => {
  const length = resolveSectionLength(selectedClass.value?.sectionDesc)
  return ['1-2节', '3-4节', '5-6节', '7-8节', '9-10节', '11-12节', '1-4节', '5-8节', '9-12节']
    .filter(item => resolveSectionLength(item) === length)
})

function resetForm() {
  Object.assign(form, {
    teachingClassId: '',
    requestedWeekDay: '',
    requestedSectionDesc: '',
    requestedWeekRange: '',
    requestedClassroomId: '',
    requestReason: ''
  })
  classroomOptions.value = []
}

function formatWeekDay(weekDay) {
  return weekDays.find(item => item.value === weekDay)?.label || weekDay
}

function statusType(status) {
  if (status === '已通过') {
    return 'success'
  }
  if (status === '已驳回') {
    return 'danger'
  }
  return 'warning'
}

function resolveSectionLength(sectionDesc) {
  if (!sectionDesc || !sectionDesc.includes('-')) {
    return 0
  }
  const [start, end] = sectionDesc.replace('节', '').split('-').map(item => Number(item))
  if (Number.isNaN(start) || Number.isNaN(end)) {
    return 0
  }
  return end - start + 1
}

async function handleClassChange() {
  const current = selectedClass.value
  if (!current) {
    resetForm()
    return
  }
  form.requestedWeekDay = current.weekDay
  form.requestedSectionDesc = current.sectionDesc
  form.requestedWeekRange = current.weekRange
  form.requestedClassroomId = ''
  await loadAvailableClassrooms()
}

async function loadAvailableClassrooms() {
  if (!form.teachingClassId || !form.requestedWeekDay || !form.requestedSectionDesc) {
    classroomOptions.value = []
    return
  }
  classroomLoading.value = true
  try {
    const response = await getTeacherAvailableClassroomOptions({
      teachingClassId: form.teachingClassId,
      weekDay: form.requestedWeekDay,
      sectionDesc: form.requestedSectionDesc
    })
    classroomOptions.value = response.data || []
  } finally {
    classroomLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const [classResponse, requestResponse] = await Promise.all([
      getTeachingClasses(),
      getTeacherScheduleAdjustRequests()
    ])
    classOptions.value = classResponse.data || []
    requests.value = requestResponse.data || []
    if (form.teachingClassId) {
      await loadAvailableClassrooms()
    }
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!form.teachingClassId || !form.requestedWeekDay || !form.requestedSectionDesc || !form.requestedWeekRange || !form.requestedClassroomId || !form.requestReason) {
    ElMessage.warning('请完整填写调课申请')
    return
  }
  saving.value = true
  try {
    await createTeacherScheduleAdjustRequest(form)
    ElMessage.success('调课申请提交成功')
    resetForm()
    await loadData()
  } finally {
    saving.value = false
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

.request-form {
  margin-bottom: 22px;
  padding: 20px;
  border-radius: 8px;
  background: #f8fbfd;
  border: 1px solid #c8ecff;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.reason-field {
  grid-column: 1 / -1;
}

.action-row {
  display: flex;
  gap: 12px;
}

@media (max-width: 720px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
