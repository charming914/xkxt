<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>考试管理</h1>
        <p>考试周为本学期结束后两周的工作日。可一键生成本学期考试安排，并直接修改或删除单场考试。</p>
      </div>
      <div class="action-row">
        <el-button type="primary" :loading="generating" @click="handleGenerate">生成本学期考试安排</el-button>
        <el-button @click="loadExams">刷新列表</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model.trim="filters.keyword"
        clearable
        placeholder="搜索课程 / 教师 / 教学班 / 教室 / 状态"
        style="width: 320px"
      />
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-empty v-if="!loading && sortedExams.length === 0" description="暂无匹配考试安排" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border @sort-change="handleSortChange">
        <el-table-column prop="examId" label="编号" width="80" sortable="custom" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" sortable="custom" />
        <el-table-column prop="teacherName" label="任课教师" width="110" sortable="custom" />
        <el-table-column prop="teachingClassId" label="教学班" width="90" sortable="custom" />
        <el-table-column prop="examDate" label="考试时间" min-width="220" sortable="custom">
          <template #default="{ row }">
            {{ formatDate(row.examDate) }} {{ formatTimeRange(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="classroomLabel" label="考试地点" min-width="160" sortable="custom">
          <template #default="{ row }">
            {{ row.buildingName ? `${row.buildingName} ${row.roomName || ''}` : '未指定' }}
          </template>
        </el-table-column>
        <el-table-column prop="examStatus" label="状态" width="100" sortable="custom">
          <template #default="{ row }">
            <el-tag>{{ row.examStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" type="primary" @click="openEdit(row)">修改</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="editVisible" title="修改考试安排" width="460px">
      <el-form label-position="top" :model="form">
        <el-form-item label="考试日期">
          <el-date-picker
            v-model="form.examDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择考试日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker
            v-model="form.startTime"
            value-format="HH:mm:ss"
            placeholder="开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker
            v-model="form.endTime"
            value-format="HH:mm:ss"
            placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="考试教室">
          <el-select v-model="form.classroomId" clearable filterable placeholder="请选择教室（可留空）">
            <el-option
              v-for="item in classroomOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  deleteAdminExam,
  generateAdminExams,
  getAdminExamDeleteCheck,
  getAdminExams,
  getClassroomOptions,
  updateAdminExam
} from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'
import { runProtectedDelete } from '../../utils/deleteGuard'
import { includesKeyword, sortRows, updateSortState } from '../../utils/tableTools'

const loading = ref(false)
const generating = ref(false)
const saving = ref(false)
const exams = ref([])
const classroomOptions = ref([])
const editVisible = ref(false)
const currentRow = ref(null)
const filters = reactive({
  keyword: ''
})
const sortState = reactive({
  prop: '',
  order: ''
})
const form = reactive({
  examDate: '',
  startTime: '',
  endTime: '',
  classroomId: ''
})

const filteredExams = computed(() => {
  return exams.value.filter(row => includesKeyword(row, [
    'examId',
    'teachingClassId',
    'semesterName',
    'courseName',
    'teacherName',
    classroomLabel,
    'examStatus'
  ], filters.keyword))
})

const sortedExams = computed(() => {
  return sortRows(filteredExams.value, sortState, {
    classroomLabel,
    examDate: row => `${formatDate(row.examDate)} ${formatTimeRange(row.startTime, row.endTime)}`
  })
})
const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(sortedExams, 10)

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function formatTimeRange(start, end) {
  if (!start || !end) return ''
  return `${String(start).slice(0, 5)}-${String(end).slice(0, 5)}`
}

function classroomLabel(row) {
  return [row.buildingName, row.roomName].filter(Boolean).join(' ') || row.classroomId || ''
}

function resetFilters() {
  filters.keyword = ''
  sortState.prop = ''
  sortState.order = ''
}

function handleSortChange(sort) {
  updateSortState(sortState, sort)
}

async function loadExams() {
  loading.value = true
  try {
    const response = await getAdminExams()
    exams.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadClassrooms() {
  const response = await getClassroomOptions()
  classroomOptions.value = response.data || []
}

async function handleGenerate() {
  generating.value = true
  try {
    const response = await generateAdminExams()
    ElMessage.success(`已生成 ${response.data} 场考试安排`)
    await loadExams()
  } finally {
    generating.value = false
  }
}

function openEdit(row) {
  currentRow.value = row
  form.examDate = formatDate(row.examDate)
  form.startTime = row.startTime ? String(row.startTime).slice(0, 8) : ''
  form.endTime = row.endTime ? String(row.endTime).slice(0, 8) : ''
  form.classroomId = row.classroomId || ''
  editVisible.value = true
}

async function handleSave() {
  if (!form.examDate || !form.startTime || !form.endTime) {
    ElMessage.warning('请完整填写考试日期与时间')
    return
  }
  if (form.startTime >= form.endTime) {
    ElMessage.warning('开始时间必须早于结束时间')
    return
  }
  saving.value = true
  try {
    await updateAdminExam(currentRow.value.examId, {
      examDate: form.examDate,
      startTime: form.startTime,
      endTime: form.endTime,
      classroomId: form.classroomId || ''
    })
    ElMessage.success('考试安排修改成功')
    editVisible.value = false
    await loadExams()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  const deleted = await runProtectedDelete({
    entityName: '考试安排',
    targetLabel: `${row.courseName || row.examId}（${row.examId}）`,
    checkAction: () => getAdminExamDeleteCheck(row.examId),
    deleteAction: () => deleteAdminExam(row.examId),
    finalWarning: '删除后将移除该考试安排，无法恢复。'
  })
  if (deleted) {
    ElMessage.success('考试安排已删除')
    await loadExams()
  }
}

onMounted(() => {
  loadExams()
  loadClassrooms()
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
