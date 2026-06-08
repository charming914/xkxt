<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>教学班管理</h1>
        <p>管理员可以新增教学班、指定任课教师和教室，并设置容量。上课节次由系统按课程学分自动排课（每节2学时，奇数学分排单/双周），选课状态由学期选课时间自动判定。</p>
      </div>
      <div class="toolbar">
        <el-button @click="loadTeachingClasses">刷新教学班</el-button>
        <el-button type="primary" @click="openCreateDialog">新增教学班</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="filters.semesterId" clearable placeholder="按学期筛选">
        <el-option v-for="item in semesters" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.courseId" clearable placeholder="按课程筛选">
        <el-option v-for="item in courses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.teacherId" clearable placeholder="按教师筛选">
        <el-option v-for="item in teachers" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.classroomId" clearable placeholder="按教室筛选">
        <el-option v-for="item in classrooms" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :disabled="filteredTeachingClasses.length === 0" @click="exportClassroomTimetable">导出教室课表</el-button>
      <el-button @click="resetFilters">重置筛选</el-button>
    </div>

    <el-empty v-if="!loading && filteredTeachingClasses.length === 0" description="暂无教学班数据" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border>
        <el-table-column prop="teachingClassId" label="教学班编号" width="120" />
        <el-table-column prop="semesterName" label="学期" min-width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column label="班型" width="100">
          <template #default="{ row }">
            <el-tag v-if="Number(row.isRetakeClass) === 1" type="warning">重修班</el-tag>
            <el-tag v-else type="info">普通班</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="courseStartDate" label="开课日期" width="120" />
        <el-table-column prop="teacherName" label="任课教师" width="110" />
        <el-table-column label="上课时间" min-width="200">
          <template #default="{ row }">
            {{ row.scheduleText || '待排课' }}
          </template>
        </el-table-column>
        <el-table-column label="教室" min-width="140">
          <template #default="{ row }">
            {{ row.buildingName || '-' }} {{ row.roomName || '' }}
          </template>
        </el-table-column>
        <el-table-column label="容量" width="90">
          <template #default="{ row }">
            {{ row.selectedCount }}/{{ row.maxStudents }}
          </template>
        </el-table-column>
        <el-table-column label="选课状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" type="primary" plain @click="openStudentDialog(row)">学生名单</el-button>
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑教学班' : '新增教学班'" width="760px">
      <el-form :model="form" label-width="90px" class="grid-form">
        <el-form-item label="学期">
          <el-select v-model="form.semesterId">
            <el-option v-for="item in semesters" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="form.courseId">
            <el-option v-for="item in courses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开课日期">
          <el-date-picker
            v-model="form.courseStartDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择开课日期"
          />
        </el-form-item>
        <el-form-item label="任课教师">
          <el-select v-model="form.teacherId">
            <el-option v-for="item in teachers" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="教室">
          <el-select v-model="form.classroomId" clearable>
            <el-option v-for="item in classrooms" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="星期">
          <el-input disabled placeholder="按学分自动排课" />
        </el-form-item>
        <el-form-item label="节次">
          <el-input disabled placeholder="按学分自动排课" />
        </el-form-item>
        <el-form-item label="周次">
          <el-input v-model="form.weekRange" placeholder="例如：1-16周" />
        </el-form-item>
        <el-form-item label="重修班">
          <el-switch
            v-model="form.isRetakeClass"
            :active-value="1"
            :inactive-value="0"
            active-text="是"
            inactive-text="否"
          />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.maxStudents" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="studentDialogVisible" title="已选学生名单" width="760px">
      <div class="student-dialog-header">
        <strong v-if="currentTeachingClassLabel">{{ currentTeachingClassLabel }}</strong>
      </div>
      <el-empty v-if="!studentLoading && selectedStudents.length === 0" description="当前教学班暂无已选学生" />
      <el-table v-else v-loading="studentLoading" :data="selectedStudents" border>
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="className" label="班级" min-width="150" />
        <el-table-column prop="phone" label="联系电话" min-width="140" />
        <el-table-column prop="selectionTime" label="选课时间" min-width="170" />
        <el-table-column prop="selectionStatus" label="状态" width="100" />
      </el-table>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminTeachingClass,
  deleteAdminTeachingClass,
  getAdminTeachingClassStudents,
  getAdminTeachingClasses,
  getClassroomOptions,
  getCourseOptions,
  getSemesterOptions,
  getTeacherOptions,
  updateAdminTeachingClass
} from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)
const teachingClasses = ref([])
const selectedStudents = ref([])
const semesters = ref([])
const courses = ref([])
const teachers = ref([])
const classrooms = ref([])
const studentDialogVisible = ref(false)
const studentLoading = ref(false)
const currentTeachingClassLabel = ref('')
const filters = reactive({
  semesterId: null,
  courseId: '',
  teacherId: '',
  classroomId: ''
})

const weekDayOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' }
]
const sectionOptions = ['1-2节', '1-4节', '3-4节', '5-6节', '5-8节', '7-8节', '9-10节', '9-12节']

const form = reactive({
  semesterId: null,
  courseStartDate: '',
  courseId: '',
  teacherId: '',
  classroomId: '',
  weekRange: '1-16周',
  isRetakeClass: 0,
  maxStudents: 60
})

const filteredTeachingClasses = computed(() => {
  return teachingClasses.value.filter(item => {
    const matchSemester = !filters.semesterId || item.semesterId === filters.semesterId
    const matchCourse = !filters.courseId || item.courseId === filters.courseId
    const matchTeacher = !filters.teacherId || item.teacherId === filters.teacherId
    const matchClassroom = !filters.classroomId || item.classroomId === filters.classroomId
    return matchSemester && matchCourse && matchTeacher && matchClassroom
  })
})

const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(filteredTeachingClasses, 10)

const selectedClassroomLabel = computed(() => {
  if (!filters.classroomId) {
    return '全部教室'
  }
  return classrooms.value.find(item => item.value === filters.classroomId)?.label || filters.classroomId
})

function weekLabel(weekDay) {
  const map = { 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六', 7: '日' }
  return map[weekDay] || weekDay
}

function statusTagType(status) {
  if (status === '选课中') return 'success'
  if (status === '已结束') return 'info'
  return 'warning'
}

async function loadTeachingClasses() {
  loading.value = true
  try {
    const response = await getAdminTeachingClasses()
    teachingClasses.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [semesterRes, courseRes, teacherRes, classroomRes] = await Promise.all([
    getSemesterOptions(),
    getCourseOptions(),
    getTeacherOptions(),
    getClassroomOptions()
  ])
  semesters.value = semesterRes.data || []
  courses.value = courseRes.data || []
  teachers.value = teacherRes.data || []
  classrooms.value = classroomRes.data || []
}

function resetForm() {
  Object.assign(form, {
    semesterId: semesters.value[0]?.value ?? null,
    courseStartDate: '',
    courseId: courses.value[0]?.value || '',
    teacherId: teachers.value[0]?.value || '',
    classroomId: '',
    weekRange: '1-16周',
    isRetakeClass: 0,
    maxStudents: 60
  })
}

function resetFilters() {
  filters.semesterId = null
  filters.courseId = ''
  filters.teacherId = ''
  filters.classroomId = ''
}

function classroomLabel(row) {
  return [row.buildingName, row.roomName].filter(Boolean).join(' ') || '-'
}

function sectionOrder(sectionDesc) {
  const matched = String(sectionDesc || '').match(/\d+/)
  return matched ? Number(matched[0]) : 999
}

function escapeCsv(value) {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

function exportClassroomTimetable() {
  if (filteredTeachingClasses.value.length === 0) {
    ElMessage.warning('当前没有可导出的教室课表')
    return
  }

  const headers = ['学期', '教室', '上课时间', '周次', '课程名称', '班型', '教学班编号', '任课教师', '开课日期', '容量', '状态']
  const rows = [...filteredTeachingClasses.value]
    .sort((a, b) => {
      return String(a.semesterName || '').localeCompare(String(b.semesterName || ''), 'zh-Hans') ||
        classroomLabel(a).localeCompare(classroomLabel(b), 'zh-Hans') ||
        String(a.courseStartDate || '').localeCompare(String(b.courseStartDate || '')) ||
        Number(a.teachingClassId || 0) - Number(b.teachingClassId || 0)
    })
    .map(row => [
      row.semesterName,
      classroomLabel(row),
      row.scheduleText || '待排课',
      row.weekRange,
      row.courseName,
      Number(row.isRetakeClass) === 1 ? '重修班' : '普通班',
      row.teachingClassId,
      row.teacherName,
      row.courseStartDate,
      `${row.selectedCount ?? 0}/${row.maxStudents ?? ''}`,
      row.status
    ])
  const content = [headers, ...rows].map(row => row.map(escapeCsv).join(',')).join('\n')
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `classroom-timetable-${selectedClassroomLabel.value}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

function openCreateDialog() {
  isEdit.value = false
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  currentId.value = row.teachingClassId
  Object.assign(form, {
    semesterId: row.semesterId,
    courseStartDate: row.courseStartDate || '',
    courseId: row.courseId,
    teacherId: row.teacherId,
    classroomId: row.classroomId || '',
    weekRange: row.weekRange,
    isRetakeClass: Number(row.isRetakeClass) === 1 ? 1 : 0,
    maxStudents: row.maxStudents
  })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.courseStartDate) {
    ElMessage.warning('请选择开课日期')
    return
  }

  if (isEdit.value) {
    await updateAdminTeachingClass(currentId.value, form)
    ElMessage.success('教学班修改成功')
  } else {
    await createAdminTeachingClass(form)
    ElMessage.success('教学班新增成功')
  }
  dialogVisible.value = false
  await loadTeachingClasses()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除教学班 ${row.teachingClassId} 吗？`, '提示', { type: 'warning' })
  await deleteAdminTeachingClass(row.teachingClassId)
  ElMessage.success('教学班删除成功')
  await loadTeachingClasses()
}

async function openStudentDialog(row) {
  currentTeachingClassLabel.value = `${row.courseName} / 教学班 ${row.teachingClassId}`
  studentDialogVisible.value = true
  studentLoading.value = true
  try {
    const response = await getAdminTeachingClassStudents(row.teachingClassId)
    selectedStudents.value = response.data || []
  } finally {
    studentLoading.value = false
  }
}

onMounted(async () => {
  await loadOptions()
  resetForm()
  loadTeachingClasses()
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.action-row {
  display: flex;
  gap: 8px;
}

.grid-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.student-dialog-header {
  margin-bottom: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
