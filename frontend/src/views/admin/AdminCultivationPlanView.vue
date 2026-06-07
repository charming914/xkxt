<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>培养方案</h1>
        <p>导入各入学年份、专业与课程类型对应的毕业要求学分。</p>
      </div>
      <el-button @click="loadRequirements">刷新列表</el-button>
    </div>

    <div class="import-panel">
      <div>
        <h2>导入培养方案表</h2>
        <p>CSV 列顺序：enrollmentYear, majorId, courseType, requiredCredit, remark</p>
      </div>
      <div class="upload-actions">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :limit="1"
          accept=".csv"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
        >
          <el-button>选择 CSV</el-button>
        </el-upload>
        <el-button type="primary" :loading="importing" :disabled="!selectedFile" @click="handleImport">
          导入
        </el-button>
      </div>
    </div>

    <el-empty v-if="!loading && requirements.length === 0" description="暂无培养方案数据" />

    <el-table v-else v-loading="loading" :data="requirements" border>
      <el-table-column prop="enrollmentYear" label="入学年份" width="110" />
      <el-table-column prop="majorId" label="专业编号" width="120" />
      <el-table-column prop="majorName" label="专业名称" min-width="140" />
      <el-table-column prop="courseType" label="课程类型" min-width="140" />
      <el-table-column prop="requiredCredit" label="要求学分" width="110" />
      <el-table-column prop="remark" label="备注" min-width="180" />
      <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCultivationPlanRequirements, importCultivationPlan } from '../../api/admin'

const loading = ref(false)
const importing = ref(false)
const requirements = ref([])
const uploadRef = ref(null)
const selectedFile = ref(null)

function handleFileChange(file) {
  selectedFile.value = file.raw
}

function handleFileRemove() {
  selectedFile.value = null
}

async function loadRequirements() {
  loading.value = true
  try {
    const response = await getCultivationPlanRequirements()
    requirements.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function handleImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请选择 CSV 文件')
    return
  }
  importing.value = true
  try {
    const response = await importCultivationPlan(selectedFile.value)
    ElMessage.success(response.message || `培养方案导入成功，共导入 ${response.data || 0} 条`)
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    await loadRequirements()
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadRequirements()
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

.import-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
  padding: 16px;
  border: 1px solid #d8e9df;
  border-radius: 8px;
  background: #f8fbfd;
}

.import-panel h2 {
  margin: 0 0 6px;
  font-size: 16px;
}

.import-panel p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 720px) {
  .import-panel {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
