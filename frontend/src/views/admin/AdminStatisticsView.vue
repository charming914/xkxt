<template>
  <section class="page-card">
    <div class="setting-card">
      <div class="setting-header">
        <div>
          <h2>选课设置</h2>
          <p>管理员为当前学期设置选课时间窗口，也可以临时手动开启或强制关闭。</p>
        </div>
        <div class="toolbar">
          <el-button @click="loadSetting">刷新设置</el-button>
          <el-button type="primary" @click="handleSaveSetting">保存设置</el-button>
        </div>
      </div>

      <div class="setting-grid">
        <div class="setting-item">
          <span class="setting-label">设置学期</span>
          <el-select
            v-model="selectedSettingSemesterId"
            filterable
            placeholder="选择学期"
            :disabled="semesterOptions.length === 0"
            @change="loadSetting"
          >
            <el-option
              v-for="semester in semesterOptions"
              :key="semester.semesterId"
              :label="semester.semesterName"
              :value="semester.semesterId"
            >
              <span>{{ semester.semesterName }}</span>
              <el-tag v-if="Number(semester.isCurrent) === 1" size="small" type="success" class="inline-tag">当前</el-tag>
            </el-option>
          </el-select>
        </div>
        <div class="setting-item">
          <span class="setting-label">选课状态</span>
          <el-tag :type="courseSelectionStatusType">{{ courseSelectionStatus }}</el-tag>
        </div>
        <div class="setting-item setting-item-wide">
          <span class="setting-label">手动控制</span>
          <el-radio-group v-model="setting.courseSelectionManualMode">
            <el-radio-button label="AUTO">按默认时间</el-radio-button>
            <el-radio-button label="OPEN">手动开启</el-radio-button>
            <el-radio-button label="CLOSED">强制关闭</el-radio-button>
          </el-radio-group>
        </div>
        <div v-if="setting.courseSelectionManualMode === 'OPEN'" class="setting-item setting-item-wide">
          <span class="setting-label">手动开启截止时间</span>
          <div class="manual-end-row">
            <el-date-picker
              v-model="setting.courseSelectionManualEnd"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="不填则一直开启"
            />
            <el-button plain @click="clearManualEnd">长期开启</el-button>
          </div>
          <div class="duration-actions">
            <el-button size="small" plain @click="setManualDuration(1)">1小时</el-button>
            <el-button size="small" plain @click="setManualDuration(24)">1天</el-button>
            <el-button size="small" plain @click="setManualDuration(168)">1周</el-button>
          </div>
        </div>
        <div class="setting-item">
          <span class="setting-label">选课开始时间</span>
          <el-date-picker
            v-model="setting.courseSelectStart"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开始时间"
          />
        </div>
        <div class="setting-item">
          <span class="setting-label">选课结束时间</span>
          <el-date-picker
            v-model="setting.courseSelectEnd"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择结束时间"
          />
        </div>
        <div class="setting-item">
          <span class="setting-label">补考申请开始时间</span>
          <el-date-picker
            v-model="setting.makeupRequestStart"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开始时间"
          />
        </div>
        <div class="setting-item">
          <span class="setting-label">补考申请结束时间</span>
          <el-date-picker
            v-model="setting.makeupRequestEnd"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="开放不超过一周"
          />
        </div>
      </div>
    </div>

    <div class="setting-card">
      <div class="setting-header">
        <div>
          <h2>成绩提交设置</h2>
          <p>管理员设置当前学期教师提交成绩的时间窗口。</p>
        </div>
        <div class="toolbar">
          <el-button @click="loadScoreSetting">刷新设置</el-button>
          <el-button type="primary" @click="handleSaveScoreSetting">保存设置</el-button>
        </div>
      </div>

      <div class="setting-grid">
        <div class="setting-item">
          <span class="setting-label">当前学期</span>
          <strong>{{ scoreSetting.semesterName || '-' }}</strong>
        </div>
        <div class="setting-item">
          <span class="setting-label">成绩提交开始时间</span>
          <el-date-picker
            v-model="scoreSetting.scoreSubmitStart"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开始时间"
          />
        </div>
        <div class="setting-item">
          <span class="setting-label">成绩提交结束时间</span>
          <el-date-picker
            v-model="scoreSetting.scoreSubmitEnd"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择结束时间"
          />
        </div>
      </div>
    </div>

    <div class="page-header">
      <div>
        <h1>统计分析</h1>
        <p>管理员查看教学班选课情况和容量使用率。</p>
      </div>
      <el-button @click="loadStatistics">刷新统计</el-button>
    </div>

    <div class="summary-row">
      <div class="summary-card">
        <span class="summary-label">教学班数量</span>
        <strong>{{ statistics.length }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">总选课人数</span>
        <strong>{{ totalSelected }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">平均容量使用率</span>
        <strong>{{ averageUsage }}</strong>
      </div>
    </div>

    <el-empty v-if="!loading && statistics.length === 0" description="暂无统计数据" />

    <template v-else>
      <el-table v-loading="loading" :data="statPaged.pagedData.value" border>
      <el-table-column prop="semesterName" label="学期" min-width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="teachingClassId" label="教学班编号" width="120" />
      <el-table-column prop="teacherName" label="任课教师" width="120" />
      <el-table-column label="选课人数" width="100">
        <template #default="{ row }">
          {{ row.selectedCount }}
        </template>
      </el-table-column>
      <el-table-column label="容量上限" width="100">
        <template #default="{ row }">
          {{ row.maxStudents }}
        </template>
      </el-table-column>
      <el-table-column label="使用率" width="120">
        <template #default="{ row }">
          {{ usageRate(row) }}
        </template>
      </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="statPaged.currentPage.value"
          v-model:page-size="statPaged.pageSize.value"
          :total="statPaged.total.value"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="statPaged.handleSizeChange"
          @current-change="statPaged.handleCurrentChange"
        />
      </div>
    </template>

    <div class="page-header score-header">
      <div>
        <h1>已提交成绩</h1>
        <p>管理员可将已提交成绩退回为可修改状态。</p>
      </div>
      <div class="toolbar">
        <el-button type="warning" plain :disabled="selectedScores.length === 0" @click="handleBatchRollback">
          批量退回
        </el-button>
        <el-button @click="loadSubmittedScores">刷新成绩</el-button>
      </div>
    </div>

    <el-empty v-if="!scoreLoading && submittedScores.length === 0" description="暂无已提交成绩" />
    <template v-else>
      <el-table
        v-loading="scoreLoading"
        :data="scorePaged.pagedData.value"
        border
        @selection-change="selectedScores = $event"
      >
      <el-table-column type="selection" width="46" />
      <el-table-column prop="scoreId" label="成绩编号" width="100" />
      <el-table-column prop="semesterName" label="学期" width="130" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="teacherName" label="任课教师" width="110" />
      <el-table-column prop="studentId" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="100" />
      <el-table-column prop="usualScore" label="平时成绩" width="100" />
      <el-table-column prop="examScore" label="考试成绩" width="100" />
      <el-table-column prop="finalScore" label="总评成绩" width="100" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="warning" plain @click="handleRollback(row)">退回</el-button>
        </template>
      </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="scorePaged.currentPage.value"
          v-model:page-size="scorePaged.pageSize.value"
          :total="scorePaged.total.value"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="scorePaged.handleSizeChange"
          @current-change="scorePaged.handleCurrentChange"
        />
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAdminCourseSelectionStatistics,
  getScoreSubmitSetting,
  getSubmittedScores,
  getCourseSelectionSetting,
  getSemesterManagement,
  rollbackSubmittedScore,
  updateScoreSubmitSetting,
  updateCourseSelectionSetting
} from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'

const loading = ref(false)
const scoreLoading = ref(false)
const statistics = ref([])
const submittedScores = ref([])
const semesterOptions = ref([])
const selectedSettingSemesterId = ref(null)
const selectedScores = ref([])
const statPaged = useClientPagination(statistics, 10)
const scorePaged = useClientPagination(submittedScores, 10)
const setting = reactive({
  semesterName: '',
  courseSelectStart: '',
  courseSelectEnd: '',
  courseSelectionManualMode: 'AUTO',
  courseSelectionManualEnd: '',
  makeupRequestStart: '',
  makeupRequestEnd: ''
})
const scoreSetting = reactive({
  semesterName: '',
  scoreSubmitStart: '',
  scoreSubmitEnd: ''
})

const courseSelectionStatus = computed(() => {
  if (setting.courseSelectionManualMode === 'CLOSED') {
    return '强制关闭'
  }
  if (setting.courseSelectionManualMode === 'OPEN' && isManualOpenActive()) {
    return '手动开启'
  }
  if (!setting.courseSelectStart || !setting.courseSelectEnd) {
    return '未设置'
  }
  const now = Date.now()
  const start = parseTime(setting.courseSelectStart)
  const end = parseTime(setting.courseSelectEnd)
  if (now < start) return '未开始'
  if (now > end) return '已结束'
  return '选课中'
})

const courseSelectionStatusType = computed(() => {
  const status = courseSelectionStatus.value
  if (status === '选课中' || status === '手动开启') return 'success'
  if (status === '强制关闭') return 'danger'
  if (status === '已结束') return 'info'
  return 'warning'
})

const totalSelected = computed(() => {
  return statistics.value.reduce((sum, item) => sum + Number(item.selectedCount || 0), 0)
})

const averageUsage = computed(() => {
  if (!statistics.value.length) {
    return '0.00%'
  }
  const total = statistics.value.reduce((sum, item) => {
    const max = Number(item.maxStudents || 0)
    const selected = Number(item.selectedCount || 0)
    return sum + (max ? selected / max : 0)
  }, 0)
  return `${((total / statistics.value.length) * 100).toFixed(2)}%`
})

function usageRate(row) {
  const max = Number(row.maxStudents || 0)
  const selected = Number(row.selectedCount || 0)
  if (!max) {
    return '0.00%'
  }
  return `${((selected / max) * 100).toFixed(2)}%`
}

function parseTime(value) {
  if (!value) return NaN
  return new Date(String(value).replace('T', ' ').replace(/-/g, '/')).getTime()
}

function isManualOpenActive() {
  if (!setting.courseSelectionManualEnd) {
    return true
  }
  return Date.now() <= parseTime(setting.courseSelectionManualEnd)
}

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + ` ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function setManualDuration(hours) {
  const end = new Date()
  end.setHours(end.getHours() + hours)
  setting.courseSelectionManualMode = 'OPEN'
  setting.courseSelectionManualEnd = formatDateTime(end)
}

function clearManualEnd() {
  setting.courseSelectionManualEnd = ''
}

async function loadStatistics() {
  loading.value = true
  try {
    const response = await getAdminCourseSelectionStatistics()
    statistics.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadSemesters() {
  const response = await getSemesterManagement()
  semesterOptions.value = response.data || []
  if (!selectedSettingSemesterId.value) {
    const current = semesterOptions.value.find(item => Number(item.isCurrent) === 1)
    selectedSettingSemesterId.value = current?.semesterId ?? semesterOptions.value[0]?.semesterId ?? null
  }
}

async function loadSetting() {
  const params = selectedSettingSemesterId.value ? { semesterId: selectedSettingSemesterId.value } : undefined
  const response = await getCourseSelectionSetting(params)
  Object.assign(setting, {
    semesterName: '',
    courseSelectStart: '',
    courseSelectEnd: '',
    courseSelectionManualMode: 'AUTO',
    courseSelectionManualEnd: '',
    makeupRequestStart: '',
    makeupRequestEnd: '',
    ...(response.data || {})
  })
  setting.courseSelectionManualMode = setting.courseSelectionManualMode || 'AUTO'
  setting.courseSelectionManualEnd = setting.courseSelectionManualEnd || ''
}

async function loadScoreSetting() {
  const response = await getScoreSubmitSetting()
  Object.assign(scoreSetting, response.data || {})
}

async function loadSubmittedScores() {
  scoreLoading.value = true
  try {
    const response = await getSubmittedScores()
    submittedScores.value = response.data || []
  } finally {
    scoreLoading.value = false
  }
}

async function handleSaveSetting() {
  await updateCourseSelectionSetting({
    semesterId: selectedSettingSemesterId.value,
    courseSelectStart: setting.courseSelectStart,
    courseSelectEnd: setting.courseSelectEnd,
    courseSelectionManualMode: setting.courseSelectionManualMode || 'AUTO',
    courseSelectionManualEnd: setting.courseSelectionManualMode === 'OPEN' ? setting.courseSelectionManualEnd : '',
    makeupRequestStart: setting.makeupRequestStart,
    makeupRequestEnd: setting.makeupRequestEnd
  })
  ElMessage.success('选课设置更新成功')
  await loadSetting()
}

async function handleSaveScoreSetting() {
  await updateScoreSubmitSetting({
    scoreSubmitStart: scoreSetting.scoreSubmitStart,
    scoreSubmitEnd: scoreSetting.scoreSubmitEnd
  })
  ElMessage.success('成绩提交时间更新成功')
  await loadScoreSetting()
}

async function handleRollback(row) {
  await rollbackSubmittedScore(row.scoreId)
  ElMessage.success('成绩已退回')
  await loadSubmittedScores()
}

async function handleBatchRollback() {
  if (selectedScores.value.length === 0) {
    ElMessage.warning('请先选择要退回的成绩')
    return
  }
  const results = await Promise.allSettled(selectedScores.value.map(row => rollbackSubmittedScore(row.scoreId)))
  const successCount = results.filter(item => item.status === 'fulfilled').length
  const failedCount = results.length - successCount
  selectedScores.value = []
  await loadSubmittedScores()
  if (failedCount > 0) {
    ElMessage.warning(`批量退回完成：成功 ${successCount} 条，失败 ${failedCount} 条`)
    return
  }
  ElMessage.success(`批量退回成功，共 ${successCount} 条`)
}

onMounted(() => {
  loadSemesters().then(loadSetting)
  loadScoreSetting()
  loadStatistics()
  loadSubmittedScores()
})
</script>

<style scoped>
.setting-card {
  margin-bottom: 24px;
  padding: 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eefaff 0%, #f8fdff 100%);
  border: 1px solid #c8ecff;
}

.setting-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.setting-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.setting-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-item-wide {
  grid-column: span 2;
}

.setting-label {
  color: var(--text-muted);
  font-size: 13px;
}

.inline-tag {
  margin-left: 8px;
}

.manual-end-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.duration-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.score-header {
  margin-top: 24px;
}

.toolbar {
  display: flex;
  gap: 12px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 18px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #e7f8ff 0%, #f8fdff 100%);
  border: 1px solid #c8ecff;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: var(--sky-700);
}

.summary-label {
  color: var(--text-muted);
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 720px) {
  .setting-grid {
    grid-template-columns: 1fr;
  }

  .setting-item-wide {
    grid-column: auto;
  }

  .manual-end-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
