<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>课程管理</h1>
        <p>管理员查看课程学分、学时、考核方式和开课院系。</p>
      </div>
      <div class="toolbar">
        <el-button @click="loadCourses">刷新课程列表</el-button>
        <el-button type="primary" @click="openCreateDialog">新增课程</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model.trim="filters.keyword"
        clearable
        placeholder="搜索课程编号 / 名称 / 类型 / 院系"
        style="width: 320px"
      />
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-empty v-if="!loading && sortedCourses.length === 0" description="暂无匹配课程数据" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border @sort-change="handleSortChange">
        <el-table-column prop="courseId" label="课程编号" width="120" sortable="custom" />
        <el-table-column prop="courseName" label="课程名称" min-width="160" sortable="custom" />
        <el-table-column prop="courseType" label="课程类型" min-width="130" sortable="custom" />
        <el-table-column prop="credit" label="学分" width="80" sortable="custom" />
        <el-table-column prop="totalHours" label="学时" width="80" sortable="custom" />
        <el-table-column prop="assessmentMethod" label="考核方式" width="100" sortable="custom" />
        <el-table-column prop="usualRatio" label="平时:考试" width="110" sortable="custom">
          <template #default="{ row }">{{ row.usualRatio }} : {{ row.examRatio }}</template>
        </el-table-column>
        <el-table-column prop="departmentName" label="开课院系" min-width="150" sortable="custom" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑课程' : '新增课程'" width="720px">
      <el-form :model="form" label-width="90px" class="grid-form">
        <el-form-item label="课程编号">
          <el-input v-model="form.courseId" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="form.courseName" />
        </el-form-item>
        <el-form-item label="课程类型">
          <el-input v-model="form.courseType" />
        </el-form-item>
        <el-form-item label="学分">
          <el-input-number v-model="form.credit" :min="0" :step="0.5" />
        </el-form-item>
        <el-form-item label="总学时">
          <el-input-number v-model="form.totalHours" :min="0" />
        </el-form-item>
        <el-form-item label="考核方式">
          <el-select v-model="form.assessmentMethod">
            <el-option label="考试" value="考试" />
            <el-option label="考查" value="考查" />
          </el-select>
        </el-form-item>
        <el-form-item label="平时成绩占比">
          <el-input-number v-model="form.usualRatio" :min="0" :max="100" :step="5" @change="handleUsualRatioChange" />
          <span class="ratio-hint">考试占比 {{ form.examRatio }}%（平时+考试=100）</span>
        </el-form-item>
        <el-form-item label="开课院系">
          <el-select v-model="form.departmentId" filterable>
            <el-option v-for="item in departments" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否必修">
          <el-select v-model="form.isRequired">
            <el-option label="必修" :value="1" />
            <el-option label="选修" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程说明" class="span-2">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
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
import { ElMessage } from 'element-plus'
import {
  createAdminCourse,
  deleteAdminCourse,
  getAdminCourseDeleteCheck,
  getAdminCourses,
  getDepartmentOptions,
  updateAdminCourse
} from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'
import { runProtectedDelete } from '../../utils/deleteGuard'
import { includesKeyword, sortRows, updateSortState } from '../../utils/tableTools'

const loading = ref(false)
const courses = ref([])
const departments = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const filters = reactive({
  keyword: ''
})
const sortState = reactive({
  prop: '',
  order: ''
})
const form = reactive({
  courseId: '',
  courseName: '',
  courseType: '',
  credit: 4,
  totalHours: 48,
  assessmentMethod: '考试',
  usualRatio: 40,
  examRatio: 60,
  departmentId: '',
  isRequired: 1,
  description: ''
})

const filteredCourses = computed(() => {
  return courses.value.filter(row => includesKeyword(row, [
    'courseId',
    'courseName',
    'courseType',
    'assessmentMethod',
    'departmentName',
    'description'
  ], filters.keyword))
})

const sortedCourses = computed(() => sortRows(filteredCourses.value, sortState))

const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(sortedCourses, 10)

function handleUsualRatioChange(value) {
  const usual = Number.isFinite(value) ? value : 0
  form.usualRatio = usual
  form.examRatio = 100 - usual
}

async function loadCourses() {
  loading.value = true
  try {
    const response = await getAdminCourses()
    courses.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const response = await getDepartmentOptions()
  departments.value = response.data || []
}

function resetForm() {
  Object.assign(form, {
    courseId: '',
    courseName: '',
    courseType: '',
    credit: 4,
    totalHours: 48,
    assessmentMethod: '考试',
    usualRatio: 40,
    examRatio: 60,
    departmentId: departments.value[0]?.value || '',
    isRequired: 1,
    description: ''
  })
}

function resetFilters() {
  filters.keyword = ''
  sortState.prop = ''
  sortState.order = ''
}

function handleSortChange(sort) {
  updateSortState(sortState, sort)
}

function openCreateDialog() {
  isEdit.value = false
  currentId.value = ''
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  currentId.value = row.courseId
  Object.assign(form, {
    courseId: row.courseId,
    courseName: row.courseName,
    courseType: row.courseType,
    credit: Number(row.credit),
    totalHours: Number(row.totalHours),
    assessmentMethod: row.assessmentMethod,
    usualRatio: row.usualRatio ?? 40,
    examRatio: row.examRatio ?? 60,
    departmentId: row.departmentId || '',
    isRequired: row.isRequired ?? 1,
    description: row.description || ''
  })
  dialogVisible.value = true
}

async function handleSave() {
  if (Number(form.usualRatio) + Number(form.examRatio) !== 100) {
    ElMessage.warning('平时成绩占比与考试成绩占比之和必须为100')
    return
  }
  if (isEdit.value) {
    await updateAdminCourse(currentId.value, form)
    ElMessage.success('课程修改成功')
  } else {
    await createAdminCourse(form)
    ElMessage.success('课程新增成功')
  }
  dialogVisible.value = false
  await loadCourses()
}

async function handleDelete(row) {
  const deleted = await runProtectedDelete({
    entityName: '课程',
    targetLabel: `${row.courseName}（${row.courseId}）`,
    checkAction: () => getAdminCourseDeleteCheck(row.courseId),
    deleteAction: () => deleteAdminCourse(row.courseId),
    finalWarning: '删除后将移除课程基础信息，无法恢复。'
  })
  if (deleted) {
    ElMessage.success('课程删除成功')
    await loadCourses()
  }
}

onMounted(() => {
  loadOptions()
  loadCourses()
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.action-row {
  display: flex;
  gap: 8px;
}

.grid-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.span-2 {
  grid-column: 1 / -1;
}

.ratio-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
