<template>
  <div class="login-page">
    <div class="login-card">
      <h1>教学事务管理系统</h1>
      <p>支持管理员、教师、学生三类角色登录</p>
      <el-form label-position="top" :model="form" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" class="full-width" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  username: '20240001',
  password: '123456'
})

function buildUser(data) {
  return {
    userId: data.userId,
    username: data.username,
    realName: data.realName,
    roleCode: data.roleCode,
    token: data.token,
    refId: data.roleCode === 'STUDENT' || data.roleCode === 'TEACHER' ? data.username : null
  }
}

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const response = await login(form)
    userStore.setUser(buildUser(response.data))
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at top left, rgba(69, 136, 110, 0.35), transparent 35%),
    linear-gradient(135deg, #f4f8f5 0%, #e2efe9 100%);
}

.login-card {
  width: 420px;
  padding: 32px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  box-shadow: 0 24px 60px rgba(26, 60, 50, 0.12);
}

.full-width {
  width: 100%;
}
</style>
