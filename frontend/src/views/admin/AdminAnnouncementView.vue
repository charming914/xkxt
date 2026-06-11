<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>公告发布</h1>
        <p>面向全体师生或指定院系发布通知公告，发布后将实时送达到对应用户的站内消息中心。</p>
      </div>
      <el-button @click="loadAnnouncements">刷新</el-button>
    </div>

    <el-card class="publish-card" shadow="never">
      <template #header>
        <span class="card-title">发布新公告</span>
      </template>
      <el-form :model="form" label-width="92px" class="publish-form">
        <el-form-item label="公告标题" required>
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="投递范围" required>
          <el-radio-group v-model="form.targetScope">
            <el-radio value="ALL">全体用户</el-radio>
            <el-radio value="STUDENT">全体学生</el-radio>
            <el-radio value="TEACHER">全体教师</el-radio>
            <el-radio value="DEPARTMENT">指定院系</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.targetScope === 'DEPARTMENT'" label="目标院系" required>
          <el-select v-model="form.targetDepartmentId" filterable placeholder="请选择院系" style="width: 260px">
            <el-option
              v-for="dept in departments"
              :key="dept.value"
              :label="dept.label"
              :value="dept.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="公告内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="publishing" @click="handlePublish">发布公告</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <h2 class="section-title">历史公告</h2>
    <el-empty v-if="!loading && announcements.length === 0" description="暂无公告" />
    <el-table v-else v-loading="loading" :data="announcements" border>
      <el-table-column prop="announcementId" label="编号" width="80" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
      <el-table-column label="投递范围" width="150">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ scopeLabel(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="recipientCount" label="送达人数" width="100" />
      <el-table-column prop="publishedByName" label="发布人" width="120" />
      <el-table-column prop="createdAt" label="发布时间" width="190" />
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { publishAnnouncement, getAnnouncements, getDepartmentOptions } from '../../api/admin'

const loading = ref(false)
const publishing = ref(false)
const announcements = ref([])
const departments = ref([])
const form = reactive({
  title: '',
  content: '',
  targetScope: 'ALL',
  targetDepartmentId: ''
})

const scopeTextMap = {
  ALL: '全体用户',
  STUDENT: '全体学生',
  TEACHER: '全体教师',
  DEPARTMENT: '指定院系'
}

function scopeLabel(row) {
  if (row.targetScope === 'DEPARTMENT') {
    return `院系：${row.targetDepartmentName || row.targetDepartmentId || '-'}`
  }
  return scopeTextMap[row.targetScope] || row.targetScope
}

async function loadAnnouncements() {
  loading.value = true
  try {
    const response = await getAnnouncements()
    announcements.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadDepartments() {
  const response = await getDepartmentOptions()
  departments.value = response.data || []
}

function resetForm() {
  form.title = ''
  form.content = ''
  form.targetScope = 'ALL'
  form.targetDepartmentId = ''
}

async function handlePublish() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写公告标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写公告内容')
    return
  }
  if (form.targetScope === 'DEPARTMENT' && !form.targetDepartmentId) {
    ElMessage.warning('请选择目标院系')
    return
  }
  publishing.value = true
  try {
    const response = await publishAnnouncement({
      title: form.title.trim(),
      content: form.content.trim(),
      targetScope: form.targetScope,
      targetDepartmentId: form.targetScope === 'DEPARTMENT' ? form.targetDepartmentId : null
    })
    ElMessage.success(response.message || '公告发布成功')
    resetForm()
    loadAnnouncements()
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  loadDepartments()
  loadAnnouncements()
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

.publish-card {
  margin-bottom: 24px;
}

.card-title {
  font-weight: 600;
}

.publish-form {
  max-width: 680px;
}

.section-title {
  margin: 0 0 14px;
  font-size: 17px;
}
</style>
