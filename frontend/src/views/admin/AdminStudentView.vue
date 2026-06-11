<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学生管理</h1>
        <p>管理员查看学生基础信息、专业和班级归属。</p>
      </div>
      <div class="toolbar">
        <el-button @click="loadStudents">刷新学生列表</el-button>
        <el-button type="primary" @click="openCreateDialog">新增学生</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model.trim="filters.keyword"
        clearable
        placeholder="搜索学号 / 姓名 / 院系 / 专业 / 班级 / 手机号"
        style="width: 320px"
      />
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-empty v-if="!loading && sortedStudents.length === 0" description="暂无匹配学生数据" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border @sort-change="handleSortChange">
      <el-table-column prop="studentId" label="学号" width="120" sortable="custom" />
      <el-table-column prop="studentName" label="姓名" width="120" sortable="custom" />
      <el-table-column prop="gender" label="性别" width="80" sortable="custom" />
      <el-table-column prop="departmentName" label="院系" min-width="140" sortable="custom" />
      <el-table-column prop="majorName" label="专业" min-width="180" sortable="custom" />
      <el-table-column prop="className" label="班级" min-width="140" sortable="custom" />
      <el-table-column prop="phone" label="联系电话" min-width="140" sortable="custom" />
      <el-table-column prop="status" label="状态" width="100" sortable="custom">
        <template #default="{ row }">
          <el-tag :type="row.status === '正常' ? 'success' : 'warning'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学生' : '新增学生'" width="720px">
      <el-form :model="form" label-width="90px" class="grid-form">
        <el-form-item label="学号">
          <el-input v-model="form.studentId" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.studentName" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-input v-model="form.birthDate" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="籍贯">
          <el-input v-model="form.hometown" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="入学年份">
          <el-input v-model="form.enrollmentYear" />
        </el-form-item>
        <el-form-item label="院系">
          <el-select v-model="form.departmentId" filterable>
            <el-option v-for="item in departments" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="form.majorId" filterable>
            <el-option v-for="item in filteredMajors" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="form.classId" filterable>
            <el-option v-for="item in filteredClasses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="正常" value="正常" />
            <el-option label="休学" value="休学" />
            <el-option label="毕业" value="毕业" />
          </el-select>
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
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  createAdminStudent,
  deleteAdminStudent,
  getAdminStudents,
  getClassOptions,
  getDepartmentOptions,
  getMajorOptions,
  updateAdminStudent
} from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'
import { includesKeyword, sortRows, updateSortState } from '../../utils/tableTools'

const loading = ref(false)
const students = ref([])
const departments = ref([])
const majors = ref([])
const classes = ref([])
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
  studentId: '',
  studentName: '',
  gender: '男',
  birthDate: '',
  hometown: '',
  phone: '',
  email: '',
  enrollmentYear: '2024',
  departmentId: '',
  majorId: '',
  classId: '',
  status: '正常'
})

const filteredMajors = computed(() => {
  return majors.value.filter(item => !form.departmentId || item.departmentId === form.departmentId)
})

const filteredClasses = computed(() => {
  return classes.value.filter(item => !form.majorId || item.majorId === form.majorId)
})

const filteredStudents = computed(() => {
  return students.value.filter(row => includesKeyword(row, [
    'studentId',
    'studentName',
    'gender',
    'departmentName',
    'majorName',
    'className',
    'phone',
    'email',
    'status'
  ], filters.keyword))
})

const sortedStudents = computed(() => sortRows(filteredStudents.value, sortState))

const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(sortedStudents, 10)

async function loadStudents() {
  loading.value = true
  try {
    const response = await getAdminStudents()
    students.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [departmentRes, majorRes, classRes] = await Promise.all([
    getDepartmentOptions(),
    getMajorOptions(),
    getClassOptions()
  ])
  departments.value = departmentRes.data || []
  majors.value = majorRes.data || []
  classes.value = classRes.data || []
}

function resetForm() {
  Object.assign(form, {
    studentId: '',
    studentName: '',
    gender: '男',
    birthDate: '',
    hometown: '',
    phone: '',
    email: '',
    enrollmentYear: '2024',
    departmentId: departments.value[0]?.value || '',
    majorId: '',
    classId: '',
    status: '正常'
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
  currentId.value = row.studentId
  Object.assign(form, {
    studentId: row.studentId,
    studentName: row.studentName,
    gender: row.gender,
    birthDate: row.birthDate || '',
    hometown: row.hometown || '',
    phone: row.phone || '',
    email: row.email || '',
    enrollmentYear: row.enrollmentYear || '2024',
    departmentId: row.departmentId || '',
    majorId: row.majorId || '',
    classId: row.classId || '',
    status: row.status
  })
  dialogVisible.value = true
}

async function handleSave() {
  if (isEdit.value) {
    await updateAdminStudent(currentId.value, form)
    ElMessage.success('学生信息修改成功')
  } else {
    await createAdminStudent(form)
    ElMessage.success('学生新增成功，默认密码为123456')
  }
  dialogVisible.value = false
  await loadStudents()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除学生 ${row.studentName} 吗？`, '提示', { type: 'warning' })
  await deleteAdminStudent(row.studentId)
  ElMessage.success('学生删除成功')
  await loadStudents()
}

onMounted(() => {
  loadOptions()
  loadStudents()
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
