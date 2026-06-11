<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>审批流看板</h1>
        <p>集中查看补选、退课、调课、考试、成绩和补考重修申请的处理状态。</p>
      </div>
      <div class="toolbar">
        <el-button type="primary" :loading="loading" @click="loadBoard">刷新看板</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <div class="metric-item">
        <span>待处理</span>
        <strong>{{ pendingCount }}</strong>
      </div>
      <div class="metric-item">
        <span>全部申请</span>
        <strong>{{ allItems.length }}</strong>
      </div>
      <div class="metric-item">
        <span>已处理</span>
        <strong>{{ handledCount }}</strong>
      </div>
      <div class="metric-item">
        <span>接口异常</span>
        <strong>{{ sourceErrors.length }}</strong>
      </div>
    </div>

    <div class="type-grid">
      <button
        v-for="item in typeSummaries"
        :key="item.key"
        class="type-item"
        type="button"
        :class="{ active: filters.type === item.key }"
        @click="filters.type = filters.type === item.key ? '' : item.key"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.pending }}/{{ item.total }}</strong>
      </button>
    </div>

    <el-alert
      v-if="sourceErrors.length > 0"
      class="source-alert"
      type="warning"
      :closable="false"
      :title="`有 ${sourceErrors.length} 类申请加载失败：${sourceErrors.join('、')}`"
    />

    <div class="filter-bar">
      <el-select v-model="filters.type" clearable filterable placeholder="申请类型">
        <el-option v-for="item in workflowConfigs" :key="item.key" :label="item.label" :value="item.key" />
      </el-select>
      <el-select v-model="filters.status" clearable filterable placeholder="处理状态">
        <el-option v-for="status in statusOptions" :key="status" :label="status" :value="status" />
      </el-select>
      <el-input
        v-model.trim="filters.keyword"
        clearable
        placeholder="搜索学生 / 教师 / 课程 / 摘要"
        style="width: 260px"
      />
    </div>

    <el-empty v-if="!loading && filteredItems.length === 0" description="暂无匹配申请" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.typeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestId" label="申请编号" width="110" />
        <el-table-column prop="applicant" label="申请人" min-width="130" />
        <el-table-column prop="courseName" label="关联课程" min-width="150" />
        <el-table-column prop="summary" label="申请摘要" min-width="280" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status, row.pending)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" min-width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openSource(row)">去处理</el-button>
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
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  getAdminCourseDropRequests,
  getAdminCourseSelectionRequests,
  getAdminExamChangeRequests,
  getAdminMakeupRequests,
  getAdminRetakeRequests,
  getAdminScheduleAdjustRequests,
  getAdminScoreChangeRequests,
  getAdminScoreReviewRequests
} from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'

const router = useRouter()
const loading = ref(false)
const requestBuckets = ref({})
const sourceErrors = ref([])
const filters = reactive({
  type: '',
  status: '',
  keyword: ''
})

const workflowConfigs = [
  {
    key: 'selection',
    label: '补选审核',
    route: '/admin/course-selection-requests',
    loader: getAdminCourseSelectionRequests,
    id: row => row.requestId,
    applicant: row => personLabel(row.studentName, row.studentId),
    summary: row => `${personLabel(row.studentName, row.studentId)} 申请补选 ${row.courseName || '-'}`
  },
  {
    key: 'drop',
    label: '退课审核',
    route: '/admin/course-drop-requests',
    loader: getAdminCourseDropRequests,
    id: row => row.dropRequestId,
    pending: row => row.requestStatus === '待审核' && row.adminStatus === '待审核',
    applicant: row => personLabel(row.studentName, row.studentId),
    summary: row => `${personLabel(row.studentName, row.studentId)} 申请退课 ${row.courseName || '-'}`
  },
  {
    key: 'schedule',
    label: '调课审核',
    route: '/admin/schedule-adjust-requests',
    loader: getAdminScheduleAdjustRequests,
    id: row => row.adjustRequestId,
    applicant: row => personLabel(row.teacherName, row.teacherId),
    summary: row => `${row.courseName || '-'}：${scheduleLabel(row, 'original')} 调整为 ${scheduleLabel(row, 'requested')}`
  },
  {
    key: 'exam',
    label: '考试修改',
    route: '/admin/exam-change-requests',
    loader: getAdminExamChangeRequests,
    id: row => row.examChangeRequestId,
    applicant: row => personLabel(row.teacherName, row.teacherId),
    summary: row => `${row.courseName || '-'}：${examLabel(row, 'original')} 调整为 ${examLabel(row, 'requested')}`
  },
  {
    key: 'scoreChange',
    label: '成绩修改',
    route: '/admin/score-change-requests',
    loader: getAdminScoreChangeRequests,
    id: row => row.scoreChangeRequestId,
    applicant: row => personLabel(row.teacherName, row.teacherId),
    summary: row => `${row.courseName || '-'} / ${personLabel(row.studentName, row.studentId)}：申请退回已提交成绩`
  },
  {
    key: 'scoreReview',
    label: '成绩复核',
    route: '/admin/score-review-requests',
    loader: getAdminScoreReviewRequests,
    id: row => row.requestId,
    applicant: row => personLabel(row.studentName, row.studentId),
    summary: row => `${row.courseName || '-'}：总评 ${row.finalScore ?? '-'}，原因 ${row.requestReason || '-'}`
  },
  {
    key: 'makeup',
    label: '补考申请',
    route: '/admin/remediation-requests',
    loader: getAdminMakeupRequests,
    id: row => row.requestId,
    pending: row => row.requestStatus === '待管理员排教室',
    applicant: row => personLabel(row.studentName, row.studentId),
    summary: row => `${row.courseName || '-'}：教师确认 ${row.teacherStatus || '-'}，补考时间 ${row.confirmedExamTime || '-'}`
  },
  {
    key: 'retake',
    label: '重修申请',
    route: '/admin/remediation-requests',
    loader: getAdminRetakeRequests,
    id: row => row.requestId,
    applicant: row => personLabel(row.studentName, row.studentId),
    summary: row => `${row.courseName || '-'}：原成绩 ${row.finalScore ?? '-'}，原因 ${row.requestReason || '-'}`
  }
]

const allItems = computed(() => {
  return workflowConfigs
    .flatMap(config => {
      const rows = requestBuckets.value[config.key] || []
      return rows.map(row => normalizeItem(config, row))
    })
    .sort((a, b) => {
      if (a.pending !== b.pending) {
        return a.pending ? -1 : 1
      }
      return sortableDate(b.createdAt) - sortableDate(a.createdAt)
    })
})

const pendingCount = computed(() => allItems.value.filter(item => item.pending).length)
const handledCount = computed(() => allItems.value.filter(item => !item.pending).length)

const typeSummaries = computed(() => {
  return workflowConfigs.map(config => {
    const items = allItems.value.filter(item => item.type === config.key)
    return {
      key: config.key,
      label: config.label,
      total: items.length,
      pending: items.filter(item => item.pending).length
    }
  })
})

const statusOptions = computed(() => {
  return [...new Set(allItems.value.map(item => item.status).filter(Boolean))]
})

const filteredItems = computed(() => {
  const keyword = filters.keyword.toLowerCase()
  return allItems.value.filter(item => {
    const matchType = !filters.type || item.type === filters.type
    const matchStatus = !filters.status || item.status === filters.status
    const haystack = [item.applicant, item.courseName, item.summary, item.requestId, item.typeLabel]
      .join(' ')
      .toLowerCase()
    const matchKeyword = !keyword || haystack.includes(keyword)
    return matchType && matchStatus && matchKeyword
  })
})

const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(filteredItems, 10)

function personLabel(name, id) {
  if (name && id) {
    return `${name}（${id}）`
  }
  return name || id || '-'
}

function requestStatus(config, row) {
  if (row.requestStatus) {
    return row.requestStatus
  }
  if (row.adminStatus) {
    return row.adminStatus
  }
  return '未知'
}

function isPending(config, row) {
  if (typeof config.pending === 'function') {
    return config.pending(row)
  }
  return requestStatus(config, row) === '待审核'
}

function normalizeItem(config, row) {
  return {
    type: config.key,
    typeLabel: config.label,
    route: config.route,
    requestId: config.id(row) || '-',
    applicant: config.applicant(row),
    courseName: row.courseName || '-',
    summary: config.summary(row),
    status: requestStatus(config, row),
    pending: isPending(config, row),
    createdAt: row.createdAt || row.requestTime || row.submittedAt || row.updatedAt || '',
    raw: row
  }
}

function formatWeekDay(value) {
  const map = { 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六', 7: '日' }
  return map[value] || value || '-'
}

function scheduleLabel(row, prefix) {
  const weekDay = row[`${prefix}WeekDay`]
  const section = row[`${prefix}SectionDesc`] || '-'
  const weekRange = row[`${prefix}WeekRange`] || ''
  const building = row[`${prefix}BuildingName`] || '-'
  const room = row[`${prefix}RoomName`] || ''
  return `周${formatWeekDay(weekDay)} ${section} ${weekRange} ${building}${room ? ` ${room}` : ''}`
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '-'
}

function timeRange(start, end) {
  if (!start || !end) {
    return ''
  }
  return `${String(start).slice(0, 5)}-${String(end).slice(0, 5)}`
}

function examLabel(row, prefix) {
  const date = formatDate(row[`${prefix}ExamDate`])
  const range = timeRange(row[`${prefix}StartTime`], row[`${prefix}EndTime`])
  const building = row[`${prefix}BuildingName`] || '-'
  const room = row[`${prefix}RoomName`] || ''
  return `${date} ${range} ${building}${room ? ` ${room}` : ''}`
}

function sortableDate(value) {
  const timestamp = Date.parse(value)
  return Number.isNaN(timestamp) ? 0 : timestamp
}

function statusType(status, pending) {
  if (pending) return 'warning'
  if (status === '已通过' || status === '已安排') return 'success'
  if (status === '已驳回') return 'danger'
  if (status === '已撤销') return 'info'
  return 'info'
}

async function loadBoard() {
  loading.value = true
  sourceErrors.value = []
  try {
    const results = await Promise.allSettled(
      workflowConfigs.map(async config => {
        const response = await config.loader()
        return {
          config,
          rows: response.data || []
        }
      })
    )
    const nextBuckets = {}
    const nextErrors = []
    results.forEach(result => {
      if (result.status === 'fulfilled') {
        nextBuckets[result.value.config.key] = result.value.rows
      } else {
        const index = results.indexOf(result)
        nextBuckets[workflowConfigs[index].key] = []
        nextErrors.push(workflowConfigs[index].label)
      }
    })
    requestBuckets.value = nextBuckets
    sourceErrors.value = nextErrors
    if (nextErrors.length > 0) {
      ElMessage.warning('部分审批数据加载失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

function openSource(row) {
  router.push(row.route)
}

onMounted(() => {
  loadBoard()
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
  gap: 10px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-item {
  padding: 14px 16px;
  border: 1px solid #d7e8f5;
  border-radius: 8px;
  background: #f8fbfd;
}

.metric-item span,
.type-item span {
  display: block;
  color: var(--text-muted);
  font-size: 13px;
}

.metric-item strong {
  display: block;
  margin-top: 6px;
  color: #075985;
  font-size: 28px;
  line-height: 1;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 54px;
  padding: 12px 14px;
  border: 1px solid #d8e9df;
  border-radius: 8px;
  color: var(--text-main);
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.type-item.active,
.type-item:hover {
  border-color: #0f91d2;
  box-shadow: 0 8px 22px rgba(3, 105, 161, 0.12);
}

.type-item strong {
  color: #075985;
  font-size: 18px;
}

.source-alert {
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 980px) {
  .metric-grid,
  .type-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid,
  .type-grid {
    grid-template-columns: 1fr;
  }
}
</style>
