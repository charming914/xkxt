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

    <el-form :model="queryForm" label-width="88px" class="query-panel">
      <div class="query-grid">
        <el-form-item label="学期">
          <el-select v-model="queryForm.semesterId" clearable placeholder="选择学期">
            <el-option
              v-for="semester in semesters"
              :key="semester.semesterId"
              :label="semester.semesterName"
              :value="semester.semesterId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="老师名称">
          <el-input v-model="queryForm.teacherName" clearable placeholder="按老师名称查询" />
        </el-form-item>
        <el-form-item label="课程名">
          <el-input v-model="queryForm.courseName" clearable placeholder="按课程名查询" />
        </el-form-item>
        <el-form-item label="课程号">
          <el-input v-model="queryForm.courseId" clearable placeholder="按课程号查询" />
        </el-form-item>
        <el-form-item label="课程种类">
          <el-select v-model="queryForm.courseType" clearable placeholder="选择课程种类">
            <el-option v-for="item in courseTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="学分数">
          <el-select v-model="queryForm.credit" clearable placeholder="选择学分">
            <el-option v-for="item in creditOptions" :key="item" :label="`${item} 学分`" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="星期">
          <el-select v-model="queryForm.weekDay" clearable placeholder="选择星期">
            <el-option v-for="item in weekDayOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="节次">
          <el-select v-model="queryForm.sectionDesc" clearable placeholder="选择节次">
            <el-option v-for="item in sectionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </div>
      <div class="query-actions">
        <el-button type="primary" @click="loadCourses">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </el-form>

    <el-empty v-if="!loading && courses.length === 0" :description="courseEmptyDescription" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border>
        <el-table-column prop="teachingClassId" label="教学班编号" width="120" />
        <el-table-column prop="courseId" label="课程号" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="160" />
        <el-table-column prop="courseType" label="课程种类" width="120" />
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
        <el-table-column label="操作" width="390" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <div class="action-row">
                <el-button
                  type="primary"
                  size="small"
                  :loading="submittingId === row.teachingClassId"
                  :disabled="!canSelect(row)"
                  @click="handleSelect(row)"
                >
                  选课
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  :loading="droppingId === row.teachingClassId"
                  :disabled="!canDrop(row)"
                  @click="handleDrop(row)"
                >
                  退课
                </el-button>
                <el-button size="small" :disabled="!canRequestSelection(row)" @click="openRequestDialog(row)">
                  申请补选
                </el-button>
                <el-button size="small" :disabled="!canAddPlan(row)" @click="handleCreatePlan(row)">
                  加入计划
                </el-button>
              </div>
              <span class="reason-text" :class="reasonClass(row)">{{ selectionReason(row) }}</span>
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

    <div class="request-section">
      <div class="sub-header">
        <h2>我的补选申请</h2>
        <el-button @click="loadRequests">刷新申请记录</el-button>
      </div>
      <el-empty
        v-if="!requestLoading && requests.length === 0"
        description="暂无补选申请记录；课程人数已满或不在选课时间时，可在上方提交补选申请"
      />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCoursePlan,
  createCourseSelectionRequest,
  dropCourse,
  getAvailableCourses,
  getCourseSelectionRequests,
  getStudentSemesters,
  selectCourse
} from '../../api/student'
import { useClientPagination } from '../../composables/useClientPagination'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submittingId = ref(null)
const droppingId = ref(null)
const courses = ref([])
const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(courses, 10)
const selectionHint = ref('')
const semesters = ref([])
const requests = ref([])
const requestLoading = ref(false)
const dialogVisible = ref(false)
const selectedCourse = ref(null)
const requestReason = ref('')
const queryForm = reactive({
  semesterId: '',
  teacherName: '',
  courseName: '',
  courseId: '',
  courseType: '',
  credit: '',
  weekDay: '',
  sectionDesc: ''
})

const courseTypeOptions = ['专业基础课', '专业核心课', '专业选修课']
const creditOptions = [1, 2, 3, 4, 5]
const weekDayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]
const sectionOptions = [
  { label: '1-2节 08:00-09:40', value: '1-2节', time: '08:00-09:40' },
  { label: '3-4节 10:00-11:40', value: '3-4节', time: '10:00-11:40' },
  { label: '5-6节 13:00-14:40', value: '5-6节', time: '13:00-14:40' },
  { label: '7-8节 15:00-16:40', value: '7-8节', time: '15:00-16:40' },
  { label: '9-10节 18:00-19:40', value: '9-10节', time: '18:00-19:40' },
  { label: '11-12节 20:00-21:40', value: '11-12节', time: '20:00-21:40' }
]

const selectedCourseLabel = computed(() => {
  if (!selectedCourse.value) return ''
  return `${selectedCourse.value.courseName}（教学班 ${selectedCourse.value.teachingClassId}）`
})

const courseEmptyDescription = computed(() => {
  if (queryForm.teacherName && (queryForm.courseName || queryForm.courseId)) {
    return '未找到匹配的教师或课程，请检查教师名称、课程名称或课程号'
  }
  if (queryForm.teacherName) {
    return '未找到该教师的可选课程，请检查教师名称是否正确'
  }
  if (queryForm.courseName || queryForm.courseId) {
    return '未找到该课程的可选教学班，请检查课程名称或课程号'
  }
  if (queryForm.semesterId || queryForm.courseType || queryForm.credit || queryForm.weekDay || queryForm.sectionDesc) {
    return '暂无符合筛选条件的可选课程，请调整学期、课程种类、学分、星期或节次'
  }
  return '当前暂无可选课程，可能选课时间未到、已结束，或当前学期没有开放教学班'
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
    const response = await getAvailableCourses(buildQueryParams())
    courses.value = response.data || []
  } finally {
    loading.value = false
  }
}

function buildQueryParams() {
  return Object.fromEntries(
    Object.entries(queryForm).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )
}

function resetQuery() {
  Object.assign(queryForm, {
    semesterId: defaultSemesterId(),
    teacherName: '',
    courseName: '',
    courseId: '',
    courseType: '',
    credit: '',
    weekDay: '',
    sectionDesc: ''
  })
  loadCourses()
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

function canSelect(row) {
  return Boolean(row.canSelectDirectly) && !row.alreadySelected
}

function canRequestSelection(row) {
  return Boolean(row.canRequestSelection) && !row.alreadySelected && !row.hasPendingSelectionRequest
}

function canAddPlan(row) {
  return !row.alreadySelected && Number(row.hasCoursePlan) !== 1
}

function canDrop(row) {
  return Boolean(row.alreadySelected)
}

function selectionReason(row) {
  if (row.selectionActionReason) {
    return row.selectionActionReason
  }
  if (row.alreadySelected) {
    return '已选该教学班'
  }
  if (row.hasPendingSelectionRequest) {
    return '补选申请待审核'
  }
  if (canSelect(row)) {
    return '可直接选课'
  }
  if (canRequestSelection(row)) {
    return '可提交补选申请'
  }
  return '当前不可选'
}

function reasonClass(row) {
  if (canSelect(row)) {
    return 'reason-success'
  }
  if (canRequestSelection(row)) {
    return 'reason-warning'
  }
  return 'reason-muted'
}

async function handleSelect(row) {
  if (!canSelect(row)) {
    ElMessage.warning('该课程当前不能直接选课，可在符合条件时提交补选申请')
    return
  }
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

async function handleCreatePlan(row) {
  if (!canAddPlan(row)) {
    ElMessage.warning('该课程已选或已在选课计划中')
    return
  }
  const response = await createCoursePlan({
    studentId: userStore.refId,
    teachingClassId: row.teachingClassId
  })
  ElMessage.success(response.message || '已加入选课计划')
  await loadCourses()
}

async function handleDrop(row) {
  if (!canDrop(row)) {
    ElMessage.warning('未选该课程，不能退课')
    return
  }
  try {
    await ElMessageBox.confirm(`确认退选 ${row.courseName} 吗？`, '确认退课', {
      type: 'warning',
      confirmButtonText: '确认退课',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  droppingId.value = row.teachingClassId
  selectionHint.value = ''
  try {
    const response = await dropCourse(row.teachingClassId)
    ElMessage.success(response.message || '退课成功')
    await loadCourses()
  } finally {
    droppingId.value = null
  }
}

function openRequestDialog(row) {
  if (!canRequestSelection(row)) {
    ElMessage.warning('该课程当前不需要或不能提交补选申请')
    return
  }
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

function defaultSemesterId() {
  const current = semesters.value.find(item => Number(item.isCurrent) === 1)
  return current?.semesterId ?? ''
}

async function loadSemesters() {
  const response = await getStudentSemesters()
  semesters.value = response.data || []
  if (!queryForm.semesterId) {
    queryForm.semesterId = defaultSemesterId()
  }
}

onMounted(async () => {
  await loadSemesters()
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

.query-panel {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid #dce6df;
  border-radius: 8px;
  background: #f8fbfd;
}

.query-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px 14px;
}

.query-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.query-panel :deep(.el-form-item) {
  margin-bottom: 14px;
}

.query-panel :deep(.el-select) {
  width: 100%;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-cell {
  display: grid;
  gap: 6px;
}

.reason-text {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.35;
}

.reason-success {
  color: #047857;
}

.reason-warning {
  color: #b45309;
}

.reason-muted {
  color: var(--text-muted);
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
