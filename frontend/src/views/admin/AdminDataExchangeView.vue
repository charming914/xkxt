<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>数据导入导出中心</h1>
        <p>集中导出基础数据、教学安排、选课统计和操作日志，并批量导入学生、教师和培养方案 CSV。</p>
      </div>
      <div class="toolbar">
        <el-button :loading="loadingAll" @click="loadAllDatasets">刷新全部</el-button>
        <el-button type="primary" :loading="exportingAll" @click="exportAllDatasets">导出全部</el-button>
      </div>
    </div>

    <div class="dataset-grid">
      <div v-for="config in datasetConfigs" :key="config.key" class="dataset-item">
        <div class="dataset-main">
          <h2>{{ config.label }}</h2>
          <p>{{ config.description }}</p>
          <span v-if="datasetStates[config.key].loadedAt" class="loaded-at">
            最近加载：{{ datasetStates[config.key].loadedAt }}
          </span>
          <span v-if="datasetStates[config.key].error" class="error-text">
            {{ datasetStates[config.key].error }}
          </span>
        </div>
        <div class="dataset-count">
          <strong>{{ datasetStates[config.key].rows.length }}</strong>
          <span>条</span>
        </div>
        <div class="dataset-actions">
          <el-button size="small" :loading="datasetStates[config.key].loading" @click="loadDataset(config)">
            刷新
          </el-button>
          <el-button size="small" type="primary" plain @click="exportDataset(config)">
            导出 CSV
          </el-button>
        </div>
      </div>
    </div>

    <div class="import-section">
      <div v-for="config in importConfigs" :key="config.key" class="import-panel">
        <div class="import-info">
          <h2>{{ config.label }}</h2>
          <p>{{ config.hint }}</p>
          <div v-if="importStates[config.key].result" class="import-result">
            <span class="result-summary">
              成功 {{ importStates[config.key].result.success }} 条，失败
              {{ importStates[config.key].result.failed }} 条
            </span>
            <ul v-if="importStates[config.key].result.errors?.length" class="result-errors">
              <li v-for="(err, index) in importStates[config.key].result.errors" :key="index">
                {{ err }}
              </li>
            </ul>
          </div>
        </div>
        <div class="upload-actions">
          <el-button @click="config.downloadTemplate">下载模板</el-button>
          <el-upload
            :ref="el => setUploadRef(config.key, el)"
            :auto-upload="false"
            :limit="1"
            accept=".csv"
            :on-change="file => handleFileChange(config.key, file)"
            :on-remove="() => handleFileRemove(config.key)"
          >
            <el-button>选择 CSV</el-button>
          </el-upload>
          <el-button
            type="primary"
            :loading="importStates[config.key].importing"
            :disabled="!importStates[config.key].file"
            @click="handleImport(config)"
          >
            导入
          </el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAdminCourses,
  getAdminCourseSelectionStatistics,
  getAdminExams,
  getAdminOperationLogs,
  getAdminStudents,
  getAdminTeachers,
  getAdminTeachingClasses,
  getCultivationPlanRequirements,
  importCultivationPlan,
  importAdminStudents,
  importAdminTeachers
} from '../../api/admin'
import { downloadCsv } from '../../utils/exportCsv'

const loadingAll = ref(false)
const exportingAll = ref(false)
const uploadRefs = {}

function setUploadRef(key, el) {
  if (el) {
    uploadRefs[key] = el
  }
}

const datasetConfigs = [
  {
    key: 'students',
    label: '学生数据',
    description: '学号、姓名、院系、专业、班级和状态。',
    loader: getAdminStudents,
    columns: [
      { label: '学号', prop: 'studentId' },
      { label: '姓名', prop: 'studentName' },
      { label: '性别', prop: 'gender' },
      { label: '院系', prop: 'departmentName' },
      { label: '专业', prop: 'majorName' },
      { label: '班级', prop: 'className' },
      { label: '入学年份', prop: 'enrollmentYear' },
      { label: '联系电话', prop: 'phone' },
      { label: '邮箱', prop: 'email' },
      { label: '状态', prop: 'status' }
    ]
  },
  {
    key: 'teachers',
    label: '教师数据',
    description: '工号、姓名、职称、学历、院系和状态。',
    loader: getAdminTeachers,
    columns: [
      { label: '工号', prop: 'teacherId' },
      { label: '姓名', prop: 'teacherName' },
      { label: '性别', prop: 'gender' },
      { label: '职称', prop: 'titleName' },
      { label: '学历', prop: 'education' },
      { label: '所属院系', prop: 'departmentName' },
      { label: '联系电话', prop: 'phone' },
      { label: '邮箱', prop: 'email' },
      { label: '状态', prop: 'status' }
    ]
  },
  {
    key: 'courses',
    label: '课程数据',
    description: '课程编号、课程名称、类型、学分和考核方式。',
    loader: getAdminCourses,
    columns: [
      { label: '课程编号', prop: 'courseId' },
      { label: '课程名称', prop: 'courseName' },
      { label: '课程类型', prop: 'courseType' },
      { label: '学分', prop: 'credit' },
      { label: '总学时', prop: 'totalHours' },
      { label: '考核方式', prop: 'assessmentMethod' },
      { label: '平时占比', prop: 'usualRatio' },
      { label: '考试占比', prop: 'examRatio' },
      { label: '开课院系', prop: 'departmentName' },
      { label: '是否必修', value: row => Number(row.isRequired) === 1 ? '必修' : '选修' }
    ]
  },
  {
    key: 'teaching-classes',
    label: '教学班数据',
    description: '教学班、任课教师、上课时间、教室和容量。',
    loader: getAdminTeachingClasses,
    columns: [
      { label: '学期', prop: 'semesterName' },
      { label: '教学班编号', prop: 'teachingClassId' },
      { label: '课程编号', prop: 'courseId' },
      { label: '课程名称', prop: 'courseName' },
      { label: '任课教师', prop: 'teacherName' },
      { label: '开课日期', prop: 'courseStartDate' },
      { label: '上课时间', prop: 'scheduleText' },
      { label: '周次', prop: 'weekRange' },
      { label: '教室', value: classroomLabel },
      { label: '容量', prop: 'maxStudents' },
      { label: '已选人数', prop: 'selectedCount' },
      { label: '选课状态', prop: 'status' }
    ]
  },
  {
    key: 'exams',
    label: '考试安排',
    description: '考试日期、时间、地点、课程和教学班。',
    loader: getAdminExams,
    columns: [
      { label: '学期', prop: 'semesterName' },
      { label: '考试编号', prop: 'examId' },
      { label: '教学班编号', prop: 'teachingClassId' },
      { label: '课程编号', prop: 'courseId' },
      { label: '课程名称', prop: 'courseName' },
      { label: '任课教师', prop: 'teacherName' },
      { label: '考试日期', value: row => formatDate(row.examDate) },
      { label: '开始时间', prop: 'startTime' },
      { label: '结束时间', prop: 'endTime' },
      { label: '考试地点', value: classroomLabel },
      { label: '状态', prop: 'examStatus' }
    ]
  },
  {
    key: 'course-selection-statistics',
    label: '选课统计',
    description: '教学班选课人数、容量上限和容量使用率。',
    loader: getAdminCourseSelectionStatistics,
    columns: [
      { label: '学期', prop: 'semesterName' },
      { label: '教学班编号', prop: 'teachingClassId' },
      { label: '课程名称', prop: 'courseName' },
      { label: '任课教师', prop: 'teacherName' },
      { label: '选课人数', prop: 'selectedCount' },
      { label: '容量上限', prop: 'maxStudents' },
      { label: '容量使用率', value: usageRate }
    ]
  },
  {
    key: 'cultivation-plan',
    label: '培养方案',
    description: '入学年份、专业、课程类型和毕业要求学分。',
    loader: getCultivationPlanRequirements,
    columns: [
      { label: '入学年份', prop: 'enrollmentYear' },
      { label: '专业编号', prop: 'majorId' },
      { label: '专业名称', prop: 'majorName' },
      { label: '课程类型', prop: 'courseType' },
      { label: '要求学分', prop: 'requiredCredit' },
      { label: '备注', prop: 'remark' },
      { label: '更新时间', prop: 'updatedAt' }
    ]
  },
  {
    key: 'operation-logs',
    label: '操作日志',
    description: '最近 1000 条操作日志记录。',
    loader: () => getAdminOperationLogs({ page: 1, size: 1000 }),
    resolve: response => response.data?.records || [],
    columns: [
      { label: '日志编号', prop: 'logId' },
      { label: '操作人', prop: 'realName' },
      { label: '账号', prop: 'username' },
      { label: '操作类型', prop: 'operationType' },
      { label: '操作内容', prop: 'operationContent' },
      { label: '操作时间', prop: 'operationTime' }
    ]
  }
]

const datasetStates = reactive({})
datasetConfigs.forEach(config => {
  datasetStates[config.key] = {
    loading: false,
    rows: [],
    loadedAt: '',
    error: ''
  }
})

function rowsFromResponse(config, response) {
  if (typeof config.resolve === 'function') {
    return config.resolve(response)
  }
  return Array.isArray(response.data) ? response.data : []
}

async function loadDataset(config) {
  const state = datasetStates[config.key]
  state.loading = true
  state.error = ''
  try {
    const response = await config.loader()
    state.rows = rowsFromResponse(config, response)
    state.loadedAt = formatLoadedAt(new Date())
  } catch (error) {
    state.rows = []
    state.error = '加载失败'
    throw error
  } finally {
    state.loading = false
  }
}

async function loadAllDatasets() {
  loadingAll.value = true
  try {
    const results = await Promise.allSettled(datasetConfigs.map(config => loadDataset(config)))
    const failedCount = results.filter(result => result.status === 'rejected').length
    if (failedCount > 0) {
      ElMessage.warning(`有 ${failedCount} 类数据加载失败`)
    } else {
      ElMessage.success('全部数据已刷新')
    }
  } finally {
    loadingAll.value = false
  }
}

async function exportDataset(config) {
  const state = datasetStates[config.key]
  if (!state.loadedAt) {
    await loadDataset(config)
  }
  if (state.rows.length === 0) {
    ElMessage.warning(`${config.label}暂无可导出数据`)
    return false
  }
  downloadCsv(`${config.key}-${todayText()}.csv`, config.columns, state.rows)
  return true
}

async function exportAllDatasets() {
  exportingAll.value = true
  let exportedCount = 0
  try {
    for (const config of datasetConfigs) {
      const state = datasetStates[config.key]
      if (!state.loadedAt) {
        await loadDataset(config)
      }
      if (state.rows.length > 0) {
        downloadCsv(`${config.key}-${todayText()}.csv`, config.columns, state.rows)
        exportedCount += 1
      }
    }
    ElMessage.success(`已导出 ${exportedCount} 个数据文件`)
  } finally {
    exportingAll.value = false
  }
}

function handleFileChange(key, file) {
  importStates[key].file = file.raw
}

function handleFileRemove(key) {
  importStates[key].file = null
}

async function handleImport(config) {
  const state = importStates[config.key]
  if (!state.file) {
    ElMessage.warning('请选择 CSV 文件')
    return
  }
  state.importing = true
  try {
    const response = await config.importer(state.file)
    ElMessage.success(response.message || '导入成功')
    state.result = normalizeImportResult(config, response)
    uploadRefs[config.key]?.clearFiles()
    state.file = null
    if (config.refreshKey) {
      await loadDataset(datasetConfigs.find(item => item.key === config.refreshKey))
    }
  } finally {
    state.importing = false
  }
}

function normalizeImportResult(config, response) {
  // 培养方案返回的是数字；学生/教师返回 ImportResult 对象
  if (typeof response.data === 'number') {
    return { success: response.data, failed: 0, errors: [] }
  }
  if (response.data && typeof response.data === 'object') {
    return {
      success: response.data.success ?? 0,
      failed: response.data.failed ?? 0,
      errors: response.data.errors ?? []
    }
  }
  return { success: 0, failed: 0, errors: [] }
}

const importConfigs = [
  {
    key: 'students',
    label: '学生批量导入',
    hint: 'CSV 列顺序：学号,姓名,性别,入学年份,院系编号,专业编号,班级编号,状态[,出生日期,籍贯,手机号,邮箱]',
    importer: importAdminStudents,
    refreshKey: 'students',
    downloadTemplate: () => downloadCsv(
      `students-template-${todayText()}.csv`,
      ['学号', '姓名', '性别', '入学年份', '院系编号', '专业编号', '班级编号', '状态', '出生日期', '籍贯', '手机号', '邮箱'],
      [{ 学号: '2024010101', 姓名: '张三', 性别: '男', 入学年份: '2024', 院系编号: 'D001', 专业编号: 'M001', 班级编号: 'C001', 状态: '正常', 出生日期: '2006-01-01', 籍贯: '北京', 手机号: '13800000000', 邮箱: 'zs@example.com' }]
    )
  },
  {
    key: 'teachers',
    label: '教师批量导入',
    hint: 'CSV 列顺序：工号,姓名,性别,职称,院系编号,状态[,学历,手机号,邮箱,出生日期]',
    importer: importAdminTeachers,
    refreshKey: 'teachers',
    downloadTemplate: () => downloadCsv(
      `teachers-template-${todayText()}.csv`,
      ['工号', '姓名', '性别', '职称', '院系编号', '状态', '学历', '手机号', '邮箱', '出生日期'],
      [{ 工号: 'T0001', 姓名: '李四', 性别: '女', 职称: '副教授', 院系编号: 'D001', 状态: '在职', 学历: '博士', 手机号: '13900000000', 邮箱: 'ls@example.com', 出生日期: '1985-05-05' }]
    )
  },
  {
    key: 'cultivation-plan',
    label: '培养方案导入',
    hint: 'CSV 列顺序：enrollmentYear, majorId, courseType, requiredCredit, remark',
    importer: importCultivationPlan,
    refreshKey: 'cultivation-plan',
    downloadTemplate: () => downloadCsv(
      `cultivation-plan-template-${todayText()}.csv`,
      [
        { label: 'enrollmentYear', prop: 'enrollmentYear' },
        { label: 'majorId', prop: 'majorId' },
        { label: 'courseType', prop: 'courseType' },
        { label: 'requiredCredit', prop: 'requiredCredit' },
        { label: 'remark', prop: 'remark' }
      ],
      [{ enrollmentYear: 2024, majorId: 'M001', courseType: '专业必修', requiredCredit: 60, remark: '示例' }]
    )
  }
]

const importStates = reactive({})
importConfigs.forEach(config => {
  importStates[config.key] = {
    file: null,
    importing: false,
    result: null
  }
})

function classroomLabel(row) {
  return [row.buildingName, row.roomName].filter(Boolean).join(' ') || row.classroomId || ''
}

function usageRate(row) {
  const max = Number(row.maxStudents || 0)
  const selected = Number(row.selectedCount || 0)
  if (!max) {
    return '0.00%'
  }
  return `${((selected / max) * 100).toFixed(2)}%`
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : ''
}

function formatLoadedAt(value) {
  const pad = number => String(number).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ${pad(value.getHours())}:${pad(value.getMinutes())}`
}

function todayText() {
  return new Date().toISOString().slice(0, 10)
}

onMounted(() => {
  loadAllDatasets()
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

.dataset-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 22px;
}

.dataset-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  padding: 16px;
  border: 1px solid #d7e8f5;
  border-radius: 8px;
  background: #f8fbfd;
}

.dataset-main h2,
.import-panel h2 {
  margin: 0 0 6px;
  font-size: 16px;
}

.dataset-main p,
.import-panel p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.loaded-at,
.error-text {
  display: block;
  margin-top: 8px;
  font-size: 12px;
}

.loaded-at {
  color: var(--text-muted);
}

.error-text {
  color: #c2410c;
}

.dataset-count {
  min-width: 70px;
  text-align: right;
}

.dataset-count strong {
  color: #075985;
  font-size: 26px;
  line-height: 1;
}

.dataset-count span {
  margin-left: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

.dataset-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  grid-column: 1 / -1;
}

.import-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.import-panel {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 16px;
  border: 1px solid #d8e9df;
  border-radius: 8px;
  background: #fff;
}

.import-info {
  flex: 1;
  min-width: 0;
}

.import-result {
  margin-top: 10px;
  font-size: 12px;
}

.result-summary {
  font-weight: 600;
  color: #075985;
}

.result-errors {
  margin: 6px 0 0;
  padding-left: 18px;
  max-height: 160px;
  overflow-y: auto;
  color: #c2410c;
}

.result-errors li {
  line-height: 1.6;
}

.upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 920px) {
  .dataset-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .page-header,
  .import-panel {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
