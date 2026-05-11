<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>教师管理</h1>
        <p>管理员查看教师基础信息、职称和所属院系。</p>
      </div>
      <div class="toolbar">
        <el-button @click="loadTeachers">刷新教师列表</el-button>
        <el-button type="primary" @click="openCreateDialog">新增教师</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && teachers.length === 0" description="暂无教师数据" />

    <el-table v-else v-loading="loading" :data="teachers" border>
      <el-table-column prop="teacherId" label="工号" width="120" />
      <el-table-column prop="teacherName" label="姓名" width="120" />
      <el-table-column prop="titleName" label="职称" width="120" />
      <el-table-column prop="education" label="学历" width="100" />
      <el-table-column prop="departmentName" label="所属院系" min-width="150" />
      <el-table-column prop="phone" label="联系电话" min-width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === '在职' ? 'success' : 'info'">{{ row.status }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑教师' : '新增教师'" width="720px">
      <el-form :model="form" label-width="90px" class="grid-form">
        <el-form-item label="工号">
          <el-input v-model="form.teacherId" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.teacherName" />
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
        <el-form-item label="职称">
          <el-input v-model="form.titleName" />
        </el-form-item>
        <el-form-item label="学历">
          <el-input v-model="form.education" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="院系">
          <el-select v-model="form.departmentId">
            <el-option v-for="item in departments" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="在职" value="在职" />
            <el-option label="离职" value="离职" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminTeacher,
  deleteAdminTeacher,
  getAdminTeachers,
  getDepartmentOptions,
  updateAdminTeacher
} from '../../api/admin'

const loading = ref(false)
const teachers = ref([])
const departments = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const form = reactive({
  teacherId: '',
  teacherName: '',
  gender: '男',
  birthDate: '',
  titleName: '',
  education: '',
  phone: '',
  email: '',
  departmentId: '',
  status: '在职'
})

async function loadTeachers() {
  loading.value = true
  try {
    const response = await getAdminTeachers()
    teachers.value = response.data || []
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
    teacherId: '',
    teacherName: '',
    gender: '男',
    birthDate: '',
    titleName: '',
    education: '',
    phone: '',
    email: '',
    departmentId: departments.value[0]?.value || '',
    status: '在职'
  })
}

function openCreateDialog() {
  isEdit.value = false
  currentId.value = ''
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  currentId.value = row.teacherId
  Object.assign(form, {
    teacherId: row.teacherId,
    teacherName: row.teacherName,
    gender: row.gender || '男',
    birthDate: row.birthDate || '',
    titleName: row.titleName,
    education: row.education || '',
    phone: row.phone || '',
    email: row.email || '',
    departmentId: row.departmentId || '',
    status: row.status
  })
  dialogVisible.value = true
}

async function handleSave() {
  if (isEdit.value) {
    await updateAdminTeacher(currentId.value, form)
    ElMessage.success('教师信息修改成功')
  } else {
    await createAdminTeacher(form)
    ElMessage.success('教师新增成功，默认密码为123456')
  }
  dialogVisible.value = false
  await loadTeachers()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除教师 ${row.teacherName} 吗？`, '提示', { type: 'warning' })
  await deleteAdminTeacher(row.teacherId)
  ElMessage.success('教师删除成功')
  await loadTeachers()
}

onMounted(() => {
  loadOptions()
  loadTeachers()
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

.action-row {
  display: flex;
  gap: 8px;
}

.grid-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}
</style>
