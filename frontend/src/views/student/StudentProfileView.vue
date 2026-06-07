<template>
  <section class="page-card profile-page">
    <div class="page-header">
      <div>
        <h1>学生个人信息</h1>
        <p>查看学籍资料，维护当前学生账号的联系方式。</p>
      </div>
      <el-button :loading="loading" @click="loadProfile">刷新资料</el-button>
    </div>

    <div v-loading="loading" class="profile-layout">
      <el-descriptions :column="2" border class="profile-summary">
        <el-descriptions-item label="学号">{{ profile.studentId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ profile.studentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ profile.gender || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ profile.birthDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="籍贯">{{ profile.hometown || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入学年份">{{ profile.enrollmentYear || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属院系">{{ profile.departmentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ profile.majorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ profile.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ profile.status || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form label-position="top" :model="form" class="contact-form">
        <el-form-item label="手机号">
          <el-input v-model.trim="form.phone" maxlength="20" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model.trim="form.email" maxlength="50" placeholder="请输入邮箱" />
        </el-form-item>
        <div class="action-row">
          <el-button @click="resetForm">恢复</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmit">保存联系方式</el-button>
        </div>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStudentProfile, updateStudentProfile } from '../../api/student'

const loading = ref(false)
const saving = ref(false)
const profile = ref({})
const form = reactive({
  phone: '',
  email: ''
})

function fillForm(data) {
  form.phone = data.phone || ''
  form.email = data.email || ''
}

async function loadProfile() {
  loading.value = true
  try {
    const response = await getStudentProfile()
    profile.value = response.data || {}
    fillForm(profile.value)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  fillForm(profile.value)
}

async function handleSubmit() {
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  saving.value = true
  try {
    await updateStudentProfile({
      phone: form.phone,
      email: form.email
    })
    ElMessage.success('个人信息保存成功')
    await loadProfile()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-page {
  max-width: 980px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.profile-layout {
  display: grid;
  gap: 22px;
}

.profile-summary {
  width: 100%;
}

.contact-form {
  max-width: 560px;
  padding: 24px;
  border-radius: 8px;
  border: 1px solid #c8ecff;
  background: linear-gradient(135deg, #eefaff 0%, #f8fdff 100%);
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

@media (max-width: 640px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
