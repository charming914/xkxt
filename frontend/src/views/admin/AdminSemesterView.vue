<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学期管理</h1>
        <p>管理员可以新建学期、切换当前学期，并在新建时从历史学期结转教学班。选课状态由学期选课时间自动判定。</p>
      </div>
      <div class="toolbar">
        <el-button @click="loadSemesters">刷新列表</el-button>
        <el-button type="primary" @click="openCreateDialog">新建学期</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && semesters.length === 0" description="暂无学期数据" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border>
        <el-table-column prop="semesterName" label="学期名称" min-width="160" />
        <el-table-column prop="schoolYear" label="学年" width="120" />
        <el-table-column prop="termCode" label="学期代码" width="100" />
        <el-table-column label="学期起止" min-width="200">
          <template #default="{ row }">
            {{ row.startDate }} ~ {{ row.endDate }}
          </template>
        </el-table-column>
        <el-table-column label="选课时间" min-width="320">
          <template #default="{ row }">
            {{ row.courseSelectStart }} ~ {{ row.courseSelectEnd }}
          </template>
        </el-table-column>
        <el-table-column label="选课状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(selectionStatus(row))">{{ selectionStatus(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前学期" width="100">
          <template #default="{ row }">
            <el-tag v-if="Number(row.isCurrent) === 1" type="success">当前</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="Number(row.isCurrent) === 1"
              @click="handleSetCurrent(row)"
            >设为当前</el-button>
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

    <el-dialog v-model="dialogVisible" title="新建学期" width="720px">
      <el-form :model="form" label-width="110px" class="grid-form">
        <el-form-item label="学期名称">
          <el-input v-model="form.semesterName" placeholder="例如：2026-2027-1" />
        </el-form-item>
        <el-form-item label="学年">
          <el-input v-model="form.schoolYear" placeholder="例如：2026-2027" />
        </el-form-item>
        <el-form-item label="学期代码">
          <el-select v-model="form.termCode">
            <el-option label="第一学期" value="1" />
            <el-option label="第二学期" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="学期开始日期"
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="学期结束日期"
          />
        </el-form-item>
        <el-form-item label="选课开始时间">
          <el-date-picker
            v-model="form.courseSelectStart"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选课开始时间"
          />
        </el-form-item>
        <el-form-item label="选课结束时间">
          <el-date-picker
            v-model="form.courseSelectEnd"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选课结束时间"
          />
        </el-form-item>
        <el-form-item label="结转教学班">
          <el-select v-model="form.carryOverFromSemesterId" clearable placeholder="不结转">
            <el-option
              v-for="item in semesters"
              :key="item.semesterId"
              :label="item.semesterName"
              :value="item.semesterId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设为当前学期">
          <el-switch v-model="form.setAsCurrent" />
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
  createSemester,
  getSemesterManagement,
  setCurrentSemester
} from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'

const loading = ref(false)
const semesters = ref([])
const dialogVisible = ref(false)
const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(semesters, 10)

const form = reactive({
  semesterName: '',
  schoolYear: '',
  termCode: '1',
  startDate: '',
  endDate: '',
  courseSelectStart: '',
  courseSelectEnd: '',
  carryOverFromSemesterId: null,
  setAsCurrent: false
})

function statusTagType(status) {
  if (status === '选课中') return 'success'
  if (status === '已结束') return 'info'
  return 'warning'
}

function selectionStatus(row) {
  if (!row.courseSelectStart || !row.courseSelectEnd) return '未设置'
  const now = Date.now()
  const start = new Date(String(row.courseSelectStart).replace(/-/g, '/')).getTime()
  const end = new Date(String(row.courseSelectEnd).replace(/-/g, '/')).getTime()
  if (now < start) return '未开始'
  if (now > end) return '已结束'
  return '选课中'
}

async function loadSemesters() {
  loading.value = true
  try {
    const response = await getSemesterManagement()
    semesters.value = response.data || []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    semesterName: '',
    schoolYear: '',
    termCode: '1',
    startDate: '',
    endDate: '',
    courseSelectStart: '',
    courseSelectEnd: '',
    carryOverFromSemesterId: null,
    setAsCurrent: false
  })
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.semesterName || !form.schoolYear || !form.termCode) {
    ElMessage.warning('请填写学期名称、学年和学期代码')
    return
  }
  if (!form.startDate || !form.endDate) {
    ElMessage.warning('请选择学期起止日期')
    return
  }
  if (!form.courseSelectStart || !form.courseSelectEnd) {
    ElMessage.warning('请选择选课起止时间')
    return
  }
  await createSemester(form)
  ElMessage.success('学期创建成功')
  dialogVisible.value = false
  await loadSemesters()
}

async function handleSetCurrent(row) {
  await ElMessageBox.confirm(
    `确定将「${row.semesterName}」设为当前学期吗？设置后其他学期将不再是当前学期。`,
    '提示',
    { type: 'warning' }
  )
  await setCurrentSemester(row.semesterId)
  ElMessage.success('当前学期切换成功')
  await loadSemesters()
}

onMounted(() => {
  loadSemesters()
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

.grid-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
