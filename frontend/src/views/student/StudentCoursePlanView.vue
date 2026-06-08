<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>选课计划</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <div class="toolbar">
        <el-select
          v-model="selectedSemesterId"
          placeholder="选择学期"
          class="semester-select"
          :disabled="semesters.length === 0"
        >
          <el-option
            v-for="semester in semesters"
            :key="semester.semesterId"
            :label="semester.semesterName"
            :value="semester.semesterId"
          />
        </el-select>
        <el-button @click="loadPlans">刷新计划</el-button>
        <el-button type="primary" :disabled="selectablePlanRows.length === 0" @click="handleSelectVisiblePlans">
          一键选课
        </el-button>
      </div>
    </div>

    <el-empty v-if="!loading && plans.length === 0" description="暂无选课计划，可在学生选课页加入计划" />
    <el-empty
      v-else-if="!loading && filteredPlans.length === 0"
      :description="`${selectedSemester?.semesterName || '当前学期'} 暂无选课计划`"
    />

    <template v-else>
      <el-table v-loading="loading" :data="filteredPlans" border>
        <el-table-column prop="semesterName" label="学期" min-width="120" />
        <el-table-column prop="courseId" label="课程号" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="160" />
        <el-table-column prop="courseType" label="课程类型" width="120" />
        <el-table-column prop="teacherName" label="教师" width="110" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column label="时间" min-width="180">
          <template #default="{ row }">
            周{{ weekLabel(row.weekDay) }} / {{ row.sectionDesc }} / {{ row.weekRange }}
          </template>
        </el-table-column>
        <el-table-column label="地点" min-width="130">
          <template #default="{ row }">
            {{ row.buildingName || '-' }} {{ row.roomName || '' }}
          </template>
        </el-table-column>
        <el-table-column label="容量" width="110">
          <template #default="{ row }">
            {{ row.selectedCount }}/{{ row.maxStudents }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="planStatusType(row)">{{ planStatus(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" type="primary" :disabled="Number(row.alreadySelected) === 1" @click="handleSelect(row)">
                选课
              </el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">移出</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <section class="plan-timetable-section">
        <div class="sub-header">
          <h2>{{ selectedSemester?.semesterName || '当前学期' }}计划课表</h2>
        </div>
        <div class="calendar-shell">
          <div class="calendar-head empty-cell" :style="gridStyle(1, 1)">节次</div>
          <div
            v-for="day in weekDays"
            :key="day.value"
            class="calendar-head"
            :style="gridStyle(day.value + 1, 1)"
          >
            周{{ day.label }}
          </div>

          <template v-for="(slot, slotIndex) in sectionSlots" :key="slot.key">
            <div class="section-label" :style="gridStyle(1, slotIndex + 2)">
              <strong>{{ slot.label }}</strong>
              <span>{{ slot.time }}</span>
            </div>
            <div
              v-for="day in weekDays"
              :key="`${day.value}-${slot.key}`"
              class="calendar-cell"
              :style="gridStyle(day.value + 1, slotIndex + 2)"
            />
          </template>

          <div
            v-for="course in filteredPlans"
            :key="`${course.planId}-${course.teachingClassId}`"
            class="course-card"
            :style="courseGridStyle(course)"
          >
            <strong>{{ course.courseName }}</strong>
            <span>{{ course.sectionDesc }}</span>
            <span>{{ course.teacherName }}</span>
            <span>{{ course.weekRange }}</span>
            <span>{{ course.buildingName || '-' }} {{ course.roomName || '' }}</span>
          </div>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCoursePlan, getCoursePlans, getStudentSemesters, selectCourse } from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const semesters = ref([])
const selectedSemesterId = ref(null)
const plans = ref([])

const weekDays = [
  { value: 1, label: '一' },
  { value: 2, label: '二' },
  { value: 3, label: '三' },
  { value: 4, label: '四' },
  { value: 5, label: '五' },
  { value: 6, label: '六' },
  { value: 7, label: '日' }
]

const sectionSlots = [
  { key: '1-2', label: '1-2节', time: '08:00-09:40', start: 1, end: 2 },
  { key: '3-4', label: '3-4节', time: '10:00-11:40', start: 3, end: 4 },
  { key: '5-6', label: '5-6节', time: '13:00-14:40', start: 5, end: 6 },
  { key: '7-8', label: '7-8节', time: '15:00-16:40', start: 7, end: 8 },
  { key: '9-10', label: '9-10节', time: '18:00-19:40', start: 9, end: 10 },
  { key: '11-12', label: '11-12节', time: '20:00-21:40', start: 11, end: 12 }
]

const selectedSemester = computed(() => {
  return semesters.value.find(item => Number(item.semesterId) === Number(selectedSemesterId.value)) || null
})

const filteredPlans = computed(() => {
  if (!selectedSemesterId.value) {
    return plans.value
  }
  return plans.value.filter(item => Number(item.semesterId) === Number(selectedSemesterId.value))
})

const selectablePlanRows = computed(() => {
  return filteredPlans.value.filter(item => Number(item.alreadySelected) !== 1)
})

function weekLabel(weekDay) {
  const match = weekDays.find(day => day.value === Number(weekDay))
  return match?.label || weekDay
}

function parseSectionRange(sectionDesc) {
  const matched = String(sectionDesc || '').match(/(\d+)\s*-\s*(\d+)/)
  if (!matched) {
    return { start: 5, end: 6 }
  }
  return {
    start: Number(matched[1]),
    end: Number(matched[2])
  }
}

function findSlotIndex(sectionNumber) {
  const index = sectionSlots.findIndex(slot => sectionNumber >= slot.start && sectionNumber <= slot.end)
  return index >= 0 ? index : 0
}

function gridStyle(column, row) {
  return {
    gridColumn: `${column} / ${column + 1}`,
    gridRow: `${row} / ${row + 1}`
  }
}

function courseGridStyle(course) {
  const range = parseSectionRange(course.sectionDesc)
  const startIndex = findSlotIndex(range.start)
  const endIndex = findSlotIndex(range.end)
  const column = Number(course.weekDay) + 1
  return {
    gridColumn: `${column} / ${column + 1}`,
    gridRow: `${startIndex + 2} / ${endIndex + 3}`
  }
}

function planStatus(row) {
  if (Number(row.alreadySelected) === 1) return '已选'
  if (Number(row.isFull) === 1) return '已满'
  return '可尝试'
}

function planStatusType(row) {
  if (Number(row.alreadySelected) === 1) return 'success'
  if (Number(row.isFull) === 1) return 'warning'
  return 'primary'
}

function defaultSemesterId() {
  const current = semesters.value.find(item => Number(item.isCurrent) === 1)
  return current?.semesterId ?? semesters.value[0]?.semesterId ?? null
}

async function loadSemesters() {
  const response = await getStudentSemesters()
  semesters.value = response.data || []
  if (!selectedSemesterId.value) {
    selectedSemesterId.value = defaultSemesterId()
  }
}

async function loadPlans() {
  loading.value = true
  try {
    const response = await getCoursePlans()
    plans.value = response.data || []
    if (selectedSemesterId.value && !semesters.value.some(item => Number(item.semesterId) === Number(selectedSemesterId.value))) {
      selectedSemesterId.value = defaultSemesterId()
    }
  } finally {
    loading.value = false
  }
}

async function handleSelect(row) {
  const response = await selectCourse({
    studentId: userStore.refId,
    teachingClassId: row.teachingClassId
  })
  ElMessage.success(response.message || '选课成功')
  await loadPlans()
}

async function handleSelectVisiblePlans() {
  const rows = selectablePlanRows.value
  if (rows.length === 0) {
    ElMessage.warning('当前学期没有可选的计划课程')
    return
  }
  try {
    await ElMessageBox.confirm(`确认对当前学期 ${rows.length} 门计划课程执行一键选课吗？`, '一键选课', {
      type: 'warning',
      confirmButtonText: '开始选课',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  let successCount = 0
  const failedMessages = []
  for (const row of rows) {
    try {
      await selectCourse({
        studentId: userStore.refId,
        teachingClassId: row.teachingClassId
      })
      successCount += 1
    } catch (error) {
      const message = error?.response?.data?.message || error?.message || '选课失败'
      failedMessages.push(`${row.courseName}：${message}`)
    }
  }
  await loadPlans()
  if (failedMessages.length > 0) {
    ElMessage.warning(`一键选课完成：成功 ${successCount} 门，失败 ${failedMessages.length} 门`)
    console.warn('一键选课失败明细', failedMessages)
    return
  }
  ElMessage.success(`一键选课成功，共 ${successCount} 门`)
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认将 ${row.courseName} 移出选课计划吗？`, '移出计划', {
    type: 'warning',
    confirmButtonText: '移出',
    cancelButtonText: '取消'
  })
  await deleteCoursePlan(row.planId)
  ElMessage.success('已移出选课计划')
  await loadPlans()
}

onMounted(async () => {
  await loadSemesters()
  await loadPlans()
})
</script>

<style scoped>
.page-header,
.sub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.semester-select {
  width: 220px;
}

.action-row {
  display: flex;
  gap: 8px;
}

.plan-timetable-section {
  margin-top: 28px;
}

.calendar-shell {
  position: relative;
  display: grid;
  grid-template-columns: 110px repeat(7, minmax(0, 1fr));
  grid-template-rows: 54px repeat(6, minmax(104px, auto));
  border: 1px solid #dce6df;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.calendar-head,
.section-label,
.calendar-cell {
  padding: 12px;
  border-right: 1px solid #e7efea;
  border-bottom: 1px solid #e7efea;
}

.calendar-head {
  background: var(--sky-100);
  font-weight: 600;
  text-align: center;
}

.empty-cell,
.section-label {
  background: #f8fbfd;
  font-weight: 600;
}

.section-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section-label span {
  color: var(--text-muted);
  font-size: 12px;
}

.calendar-cell {
  min-height: 104px;
  background: #fff;
}

.course-card {
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 6px;
  padding: 10px;
  border: 1px solid #d4e8d8;
  border-radius: 8px;
  background: linear-gradient(135deg, #e7f8ff 0%, #f8fdff 100%);
  box-shadow: 0 8px 20px rgba(2, 132, 199, 0.08);
  font-size: 13px;
}

.course-card strong {
  color: var(--sky-700);
}

@media (max-width: 960px) {
  .page-header,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .semester-select {
    width: 100%;
  }

  .calendar-shell {
    min-width: 920px;
  }

  .plan-timetable-section {
    overflow-x: auto;
  }
}
</style>
