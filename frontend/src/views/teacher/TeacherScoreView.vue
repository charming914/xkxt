<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>教师成绩录入</h1>
        <p>当前登录教师：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadScores">刷新成绩列表</el-button>
    </div>

    <el-empty v-if="!loading && scores.length === 0" description="当前暂无成绩录入数据" />

    <el-table v-else v-loading="loading" :data="scores" border>
      <el-table-column prop="teachingClassId" label="教学班编号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="studentId" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="110" />
      <el-table-column label="平时成绩" width="130">
        <template #default="{ row }">
          <el-input-number v-model="row.usualScore" :min="0" :max="100" :controls="false" />
        </template>
      </el-table-column>
      <el-table-column label="考试成绩" width="130">
        <template #default="{ row }">
          <el-input-number v-model="row.examScore" :min="0" :max="100" :controls="false" />
        </template>
      </el-table-column>
      <el-table-column prop="finalScore" label="总评成绩" width="110" />
      <el-table-column prop="gpaPoint" label="绩点" width="90" />
      <el-table-column label="提交状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.isSubmitted ? 'success' : 'info'">
            {{ row.isSubmitted ? '已提交' : '未提交' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <div class="action-row">
            <el-button size="small" @click="handleSave(row, 0)">暂存</el-button>
            <el-button type="primary" size="small" @click="handleSave(row, 1)">提交</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeacherScores, saveTeacherScore } from '../../api/teacher'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const scores = ref([])

async function loadScores() {
  loading.value = true
  try {
    const response = await getTeacherScores()
    scores.value = (response.data || []).map(item => ({
      ...item,
      usualScore: item.usualScore ?? 0,
      examScore: item.examScore ?? 0
    }))
  } finally {
    loading.value = false
  }
}

async function handleSave(row, isSubmitted) {
  await saveTeacherScore({
    studentId: row.studentId,
    teachingClassId: row.teachingClassId,
    usualScore: Number(row.usualScore),
    examScore: Number(row.examScore),
    isSubmitted
  })
  ElMessage.success(isSubmitted ? '成绩提交成功' : '成绩暂存成功')
  await loadScores()
}

onMounted(() => {
  loadScores()
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
</style>
