<template>
  <section class="password-page-shell">
    <div class="page-card password-page">
      <div class="page-header">
        <div>
          <h1>修改密码</h1>
          <p>当前账号：{{ userStore.username }}，修改后请使用新密码重新登录。</p>
        </div>
      </div>

      <div class="password-panel">
        <el-form label-position="top" :model="form">
          <el-form-item label="旧密码">
            <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入旧密码" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>
          <div class="action-row">
            <el-button @click="resetForm">重置</el-button>
            <el-button type="primary" :loading="loading" @click="handleSubmit">保存密码</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { updatePassword } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

function resetForm() {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}

async function handleSubmit() {
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    ElMessage.warning('请完整填写密码信息')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (form.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }
  loading.value = true
  try {
    await updatePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clearUser()
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.password-page-shell {
  min-height: calc(100vh - 140px);
  display: grid;
  place-items: center;
}

.password-page {
  width: min(520px, 100%);
}

.page-header {
  margin-bottom: 20px;
}

.password-panel {
  padding: 24px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eefaff 0%, #f8fdff 100%);
  border: 1px solid #c8ecff;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

@media (max-width: 640px) {
  .password-page-shell {
    min-height: auto;
    display: block;
  }

  .action-row {
    justify-content: stretch;
  }

  .action-row :deep(.el-button) {
    flex: 1;
  }
}
</style>
