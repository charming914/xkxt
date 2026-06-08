<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学期管理</h1>
        <p>每学期固定 16 教学周 + 2 考试周，考试周紧跟学期末，之后为假期。管理员只需选择学期开始日期，其余信息自动推导。</p>
      </div>
      <div class="toolbar">
        <el-button @click="loadSemesters">刷新列表</el-button>
        <el-button type="primary" @click="openCreateDialog">新建学期</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && semesters.length === 0" description="暂无学期数据" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border>
        <el-table-column prop="semesterName" label="学期名称" min-width="160" />
        <el-table-column prop="schoolYear" label="学年" width="120" />
        <el-table-column prop="termCode" label="学期代码" width="100" />
        <el-table-column label="教学周(16周)" min-width="200">
          <template #default="{ row }">
            {{ row.startDate }} ~ {{ row.endDate }}
          </template>
        </el-table-column>
        <el-table-column label="考试周(2周)" min-width="200">
          <template #default="{ row }">
            {{ examWeekRange(row) }}
          </template>
        </el-table-column>
        <el-table-column label="选课时间" min-width="320">
          <template #default="{ row }">
            {{ formatTime(row.courseSelectStart) }} ~ {{ formatTime(row.courseSelectEnd) }}
          </template>
        </el-table-column>
        <el-table-column label="选课状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(selectionStatus(row))">{{ selectionStatus(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前学期" width="100">
          <template #default="{ row }">
            <el-tag v-if="Number(row.isCurrent) === 1" type="success">当前</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="Number(row.isCurrent) === 1"
              @click="handleSetCurrent(row)"
            >设为当前</el-button>
            <el-button size="small" plain @click="openEditDialog(row)">编辑</el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :disabled="Number(row.isCurrent) === 1"
              @click="handleDelete(row)"
            >删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学期' : '新建学期'" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="学期开始日期">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择学期第一天（开学日）"
            style="width: 100%"
          />
        </el-form-item>

        <el-alert
          v-if="preview"
          type="info"
          :closable="false"
          show-icon
          class="preview-alert"
        >
          <template #title>
            <div class="preview-grid">
              <div><span class="label">学期名称</span>{{ preview.semesterName }}</div>
              <div><span class="label">学年 / 学期</span>{{ preview.schoolYear }} / 第{{ preview.termCode === '1' ? '一' : '二' }}学期</div>
              <div><span class="label">教学周(16周)</span>{{ preview.startDate }} ~ {{ preview.endDate }}</div>
              <div><span class="label">考试周(2周)</span>{{ preview.examStart }} ~ {{ preview.examEnd }}</div>
              <div><span class="label">选课时间</span>{{ preview.courseSelectStart }} ~ {{ preview.courseSelectEnd }}</div>
            </div>
          </template>
        </el-alert>

        <template v-if="!isEdit">
          <el-form-item label="结转教学班">
            <el-select v-model="form.carryOverFromSemesterId" clearable placeholder="不结转" style="width: 100%">
              <el-option
                v-for="item in semesters"
                :key="item.semesterId"
                :label="item.semesterName"
                :value="item.semesterId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="设为当前学期">
            <el-switch v-model="form.setAsCurrent" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createSemester,
  updateSemester,
  deleteSemester,
  getSemesterManagement,
  setCurrentSemester
} from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'

const TEACHING_WEEKS = 16
const EXAM_WEEKS = 2

const loading = ref(false)
const semesters = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(semesters, 10)

const form = reactive({
  startDate: '',
  carryOverFromSemesterId: null,
  setAsCurrent: false
})

function addDays(date, days) {
  const d = new Date(date)
  d.setDate(d.getDate() + days)
  return d
}

function fmtDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// 与后端 deriveSemesterFields 保持一致的推导逻辑，用于实时预览
const preview = computed(() => {
  if (!form.startDate) return null
  const start = new Date(`${form.startDate}T00:00:00`)
  if (Number.isNaN(start.getTime())) return null

  const endDate = addDays(start, TEACHING_WEEKS * 7 - 1)
  const examStart = addDays(endDate, 1)
  const examEnd = addDays(endDate, EXAM_WEEKS * 7)

  const year = start.getFullYear()
  let termCode
  let schoolYear
  if (start.getMonth() + 1 >= 7) {
    termCode = '1'
    schoolYear = `${year}-${year + 1}`
  } else {
    termCode = '2'
    schoolYear = `${year - 1}-${year}`
  }
  const semesterName = `${schoolYear}-${termCode}`

  // 选课时间：上学期考试周之前的两周（与后端一致）。从已加载列表中找出开始日早于本学期、最接近的上学期
  let selectStart
  let selectEnd
  const startStr = fmtDate(start)
  const prev = semesters.value
    .filter((s) => s.startDate && String(s.startDate).substring(0, 10) < startStr
      && (!isEdit.value || s.semesterId !== editingId.value))
    .sort((a, b) => String(b.startDate).localeCompare(String(a.startDate)))[0]
  if (prev && prev.endDate) {
    const prevEnd = new Date(`${String(prev.endDate).substring(0, 10)}T00:00:00`)
    const prevExamStart = addDays(prevEnd, 1)
    selectStart = addDays(prevExamStart, -EXAM_WEEKS * 7)
    selectEnd = addDays(prevExamStart, -1)
  } else {
    selectStart = addDays(start, -14)
    selectEnd = addDays(start, 13)
  }

  return {
    semesterName,
    schoolYear,
    termCode,
    startDate: fmtDate(start),
    endDate: fmtDate(endDate),
    examStart: fmtDate(examStart),
    examEnd: fmtDate(examEnd),
    courseSelectStart: `${fmtDate(selectStart)} 08:00:00`,
    courseSelectEnd: `${fmtDate(selectEnd)} 23:59:59`
  }
})

function examWeekRange(row) {
  if (!row.endDate) return '-'
  const end = new Date(`${String(row.endDate).substring(0, 10)}T00:00:00`)
  if (Number.isNaN(end.getTime())) return '-'
  return `${fmtDate(addDays(end, 1))} ~ ${fmtDate(addDays(end, EXAM_WEEKS * 7))}`
}

function statusTagType(status) {
  if (status === '选课中') return 'success'
  if (status === '已结束') return 'info'
  return 'warning'
}

function parseTime(str) {
  if (!str) return NaN
  return new Date(String(str).replace('T', ' ').replace(/-/g, '/')).getTime()
}

function formatTime(str) {
  if (!str) return '-'
  return String(str).replace('T', ' ')
}

function selectionStatus(row) {
  if (!row.courseSelectStart || !row.courseSelectEnd) return '未设置'
  const now = Date.now()
  const start = parseTime(row.courseSelectStart)
  const end = parseTime(row.courseSelectEnd)
  if (now < start) return '未开始'
  if (now > end) return '已结束'
  return '选课中'
}

async function loadSemesters() {
  loading.value = true
  try {
    const response = await getSemesterManagement()
    semesters.value = response.data || []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    startDate: '',
    carryOverFromSemesterId: null,
    setAsCurrent: false
  })
}

function openCreateDialog() {
  resetForm()
  isEdit.value = false
  editingId.value = null
  dialogVisible.value = true
}

function openEditDialog(row) {
  resetForm()
  isEdit.value = true
  editingId.value = row.semesterId
  form.startDate = row.startDate ? String(row.startDate).substring(0, 10) : ''
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.startDate) {
    ElMessage.warning('请选择学期开始日期')
    return
  }
  if (isEdit.value) {
    await updateSemester(editingId.value, { startDate: form.startDate })
    ElMessage.success('学期更新成功')
  } else {
    await createSemester({
      startDate: form.startDate,
      carryOverFromSemesterId: form.carryOverFromSemesterId,
      setAsCurrent: form.setAsCurrent
    })
    ElMessage.success('学期创建成功')
  }
  dialogVisible.value = false
  await loadSemesters()
}

async function handleSetCurrent(row) {
  await ElMessageBox.confirm(
    `确定将「${row.semesterName}」设为当前学期吗？设置后其他学期将不再是当前学期。`,
    '提示',
    { type: 'warning' }
  )
  await setCurrentSemester(row.semesterId)
  ElMessage.success('当前学期切换成功')
  await loadSemesters()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除学期「${row.semesterName}」吗？此操作不可恢复。`,
    '提示',
    { type: 'warning' }
  )
  await deleteSemester(row.semesterId)
  ElMessage.success('学期删除成功')
  await loadSemesters()
}

onMounted(() => {
  loadSemesters()
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

.toolbar {
  display: flex;
  gap: 12px;
}

.preview-alert {
  margin: 4px 0 18px 110px;
}

.preview-grid {
  display: grid;
  gap: 6px;
  font-size: 13px;
  line-height: 1.6;
  white-space: normal;
}

.preview-grid .label {
  display: inline-block;
  width: 96px;
  color: #909399;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
