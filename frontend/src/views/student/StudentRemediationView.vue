<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>补考重修</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="refreshAll">刷新数据</el-button>
    </div>

    <el-alert
      title="申请规则：补考申请需在开放后一周内提交，补考时间必须选择开放后两周内；重修不再提交申请，管理员开放重修班后可直接选课。"
      type="info"
      :closable="false"
      class="tip-alert"
    />

    <section class="section-block">
      <div class="sub-header">
        <h2>未通过课程</h2>
        <span class="muted-text">仅显示已提交且总评低于 60 分的成绩</span>
      </div>
      <el-empty v-if="!scoreLoading && failedScores.length === 0" description="暂无需要补考或重修的课程" />
      <el-table v-else v-loading="scoreLoading" :data="failedScores" border>
        <el-table-column prop="semesterName" label="学期" min-width="120" />
        <el-table-column prop="courseId" label="课程号" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="160" />
        <el-table-column prop="courseType" label="课程类型" width="120" />
        <el-table-column prop="teacherName" label="任课教师" width="110" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="finalScore" label="总评" width="80" />
        <el-table-column label="补考状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.makeupStatus" :type="statusType(row.makeupStatus)">{{ row.makeupStatus }}</el-tag>
            <span v-else>未申请</span>
          </template>
        </el-table-column>
        <el-table-column label="补考结果" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.makeupResultStatus" :type="makeupResultType(row.makeupResultStatus)">
              {{ row.makeupResultStatus }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" type="primary" :disabled="Number(row.canApplyMakeup) !== 1" @click="openRequest(row, 'MAKEUP')">
                申请补考
              </el-button>
            </div>
            <span class="reason-text">{{ remediationHint(row) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div class="record-grid">
      <section class="section-block">
        <div class="sub-header">
          <h2>我的补考申请</h2>
          <el-button size="small" @click="loadMakeupRequests">刷新</el-button>
        </div>
        <el-empty v-if="!makeupLoading && makeupRequests.length === 0" description="暂无补考申请记录" />
        <el-table v-else v-loading="makeupLoading" :data="makeupRequests" border>
          <el-table-column prop="semesterName" label="学期" min-width="120" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column prop="finalScore" label="原成绩" width="90" />
          <el-table-column prop="requestReason" label="申请原因" min-width="160" />
          <el-table-column prop="requestedExamTime" label="申请时间" min-width="170" />
          <el-table-column label="教师确认" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.teacherStatus)">{{ row.teacherStatus || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="confirmedExamTime" label="确认时间" min-width="170" />
          <el-table-column label="教室安排" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.adminStatus)">{{ row.adminStatus || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="补考教室" min-width="140">
            <template #default="{ row }">
              {{ classroomLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="补考结果" width="110">
            <template #default="{ row }">
              <el-tag :type="makeupResultType(row.resultStatus)">{{ row.resultStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="teacherComment" label="教师备注" min-width="150" />
          <el-table-column prop="adminComment" label="管理员备注" min-width="150" />
          <el-table-column prop="createdAt" label="提交时间" min-width="170" />
        </el-table>
      </section>

      <section class="section-block">
        <div class="sub-header">
          <h2>可选重修班</h2>
          <el-button size="small" @click="loadRetakeClasses">刷新</el-button>
        </div>
        <el-empty v-if="!retakeLoading && retakeClasses.length === 0" description="暂无开放的重修班" />
        <el-table v-else v-loading="retakeLoading" :data="retakeClasses" border>
          <el-table-column prop="semesterName" label="学期" min-width="120" />
          <el-table-column prop="courseId" label="课程号" width="120" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column prop="teacherName" label="任课教师" width="110" />
          <el-table-column label="上课时间" min-width="170">
            <template #default="{ row }">
              {{ row.scheduleText || '待排课' }}（{{ row.weekRange }}）
            </template>
          </el-table-column>
          <el-table-column label="教室" min-width="130">
            <template #default="{ row }">
              {{ classroomLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="容量" width="90">
            <template #default="{ row }">
              {{ row.selectedCount }}/{{ row.maxStudents }}
            </template>
          </el-table-column>
          <el-table-column prop="retakeActionReason" label="状态" min-width="150" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                size="small"
                type="warning"
                :disabled="Number(row.canSelectRetake) !== 1"
                @click="handleSelectRetake(row)"
              >
                选课
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form label-position="top">
        <el-form-item label="课程">
          <el-input :model-value="selectedCourseLabel" disabled />
        </el-form-item>
        <el-form-item label="补考时间">
          <el-date-picker
            v-model="requestedExamTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择补考时间"
          />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input
            v-model.trim="requestReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请说明申请原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createMakeupRequest,
  getAcademicRemediationScores,
  getMakeupRequests,
  getRetakeClasses,
  selectCourse
} from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const scoreLoading = ref(false)
const makeupLoading = ref(false)
const retakeLoading = ref(false)
const submitting = ref(false)
const failedScores = ref([])
const makeupRequests = ref([])
const retakeClasses = ref([])
const dialogVisible = ref(false)
const selectedScore = ref(null)
const requestType = ref('MAKEUP')
const requestReason = ref('')
const requestedExamTime = ref('')

const dialogTitle = computed(() => '提交补考申请')

const selectedCourseLabel = computed(() => {
  if (!selectedScore.value) return ''
  return `${selectedScore.value.courseName}（${selectedScore.value.courseId || '无课程号'}，总评 ${selectedScore.value.finalScore ?? '-'}）`
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

function remediationHint(row) {
  if (Number(row.canApplyMakeup) === 1) {
    return '可先申请补考'
  }
  if (row.makeupStatus === '待审核') {
    return '补考申请待审核'
  }
  if (row.makeupStatus === '待教师确认') {
    return '补考申请待教师确认'
  }
  if (row.makeupStatus === '待管理员排教室') {
    return '教师已确认，等待管理员安排教室'
  }
  if (row.makeupResultStatus === '未通过') {
    return '补考未通过，可在下方选择重修班'
  }
  if (row.makeupResultStatus === '已通过') {
    return '补考已通过，无需重修'
  }
  if (row.makeupStatus === '已安排') {
    return '补考已安排，等待考试结果'
  }
  if (row.makeupStatus === '已通过' && row.makeupResultStatus === '待考试') {
    return '补考已安排，等待考试结果'
  }
  if (row.makeupStatus === '已通过' && row.makeupResultStatus === '已通过') {
    return '补考已通过，无需重修'
  }
  if (row.makeupStatus === '已驳回') {
    return '补考申请被驳回，可重新申请'
  }
  return '需先完成补考流程'
}

function classroomLabel(row) {
  return [row.buildingName, row.roomName].filter(Boolean).join(' ') || '-'
}

function weekLabel(weekDay) {
  const map = { 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六', 7: '日' }
  return map[weekDay] || weekDay
}

async function loadFailedScores() {
  scoreLoading.value = true
  try {
    const response = await getAcademicRemediationScores()
    failedScores.value = response.data || []
  } finally {
    scoreLoading.value = false
  }
}

async function loadMakeupRequests() {
  makeupLoading.value = true
  try {
    const response = await getMakeupRequests()
    makeupRequests.value = response.data || []
  } finally {
    makeupLoading.value = false
  }
}

async function loadRetakeClasses() {
  retakeLoading.value = true
  try {
    const response = await getRetakeClasses()
    retakeClasses.value = response.data || []
  } finally {
    retakeLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadFailedScores(), loadMakeupRequests(), loadRetakeClasses()])
}

function openRequest(row, type) {
  selectedScore.value = row
  requestType.value = type
  requestReason.value = ''
  requestedExamTime.value = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!selectedScore.value) {
    return
  }
  if (!requestReason.value) {
    ElMessage.warning('请填写申请原因')
    return
  }
  if (!requestedExamTime.value) {
    ElMessage.warning('请选择补考时间')
    return
  }
  submitting.value = true
  try {
    const payload = {
      scoreId: selectedScore.value.scoreId,
      requestReason: requestReason.value,
      requestedExamTime: requestedExamTime.value
    }
    const response = await createMakeupRequest(payload)
    ElMessage.success(response.message || '申请提交成功')
    dialogVisible.value = false
    await refreshAll()
  } finally {
    submitting.value = false
  }
}

async function handleSelectRetake(row) {
  await selectCourse({ teachingClassId: row.teachingClassId })
  ElMessage.success('重修班选课成功')
  await refreshAll()
}

onMounted(() => {
  refreshAll()
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

.tip-alert {
  margin-bottom: 18px;
}

.section-block {
  margin-top: 22px;
}

.record-grid {
  display: grid;
  gap: 24px;
}

.muted-text,
.reason-text {
  color: var(--text-muted);
  font-size: 12px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

:deep(.el-date-editor) {
  width: 100%;
}
</style>
