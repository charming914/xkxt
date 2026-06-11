<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>操作日志</h1>
        <p>记录管理员对学生、教师、课程等数据的增删改与各类审批操作，便于追溯。</p>
      </div>
      <el-button @click="loadLogs">刷新</el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="按操作内容 / 操作人搜索"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select
        v-model="filters.operationType"
        placeholder="操作类型"
        clearable
        filterable
        style="width: 180px"
        @change="handleSearch"
      >
        <el-option v-for="type in operationTypes" :key="type" :label="type" :value="type" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <el-empty v-if="!loading && logs.length === 0" description="暂无操作日志" />

    <el-table v-else v-loading="loading" :data="logs" border>
      <el-table-column prop="logId" label="编号" width="90" />
      <el-table-column label="操作人" width="160">
        <template #default="{ row }">
          {{ row.realName || '系统' }}
          <span v-if="row.username" class="muted">（{{ row.username }}）</span>
        </template>
      </el-table-column>
      <el-table-column label="操作类型" width="140">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.operationType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operationContent" label="操作内容" min-width="240" show-overflow-tooltip />
      <el-table-column prop="operationTime" label="操作时间" width="200" />
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="loadLogs"
        @size-change="handleSizeChange"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getAdminOperationLogs, getAdminOperationLogTypes } from '../../api/admin'

const loading = ref(false)
const logs = ref([])
const operationTypes = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive({
  keyword: '',
  operationType: ''
})

async function loadLogs() {
  loading.value = true
  try {
    const response = await getAdminOperationLogs({
      keyword: filters.keyword || undefined,
      operationType: filters.operationType || undefined,
      page: page.value,
      size: size.value
    })
    logs.value = response.data?.records || []
    total.value = Number(response.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function loadTypes() {
  const response = await getAdminOperationLogTypes()
  operationTypes.value = response.data || []
}

function handleSearch() {
  page.value = 1
  loadLogs()
}

function handleSizeChange() {
  page.value = 1
  loadLogs()
}

onMounted(() => {
  loadTypes()
  loadLogs()
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.muted {
  color: var(--text-muted);
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
