<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>学业进度</h1>
        <p>当前登录学生：{{ userStore.realName }}（{{ userStore.refId }}）</p>
      </div>
      <el-button @click="loadProgress">刷新进度</el-button>
    </div>

    <el-empty v-if="!loading && progressRows.length === 0" description="暂无培养方案，请管理员导入培养方案表" />

    <template v-else>
      <div class="summary-row">
        <div class="summary-card">
          <span class="summary-label">要求总学分</span>
          <strong>{{ totalRequired }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">已获学分</span>
          <strong>{{ totalEarned }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">剩余学分</span>
          <strong>{{ totalRemaining }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">总体完成度</span>
          <strong>{{ overallPercent }}%</strong>
        </div>
      </div>

      <el-table v-loading="loading" :data="progressRows" border>
        <el-table-column prop="enrollmentYear" label="入学年份" width="110" />
        <el-table-column prop="majorName" label="专业" min-width="140" />
        <el-table-column prop="courseType" label="课程类型" min-width="140" />
        <el-table-column prop="requiredCredit" label="要求学分" width="110" />
        <el-table-column prop="earnedCredit" label="已获学分" width="110" />
        <el-table-column prop="remainingCredit" label="剩余学分" width="110" />
        <el-table-column label="完成度" min-width="220">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress :percentage="progressPercent(row)" :stroke-width="12" />
              <span>{{ progressPercent(row) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" />
      </el-table>

      <section class="selected-course-section">
        <div class="sub-header">
          <h2>已选课程总览</h2>
        </div>
        <el-empty v-if="!selectedLoading && semesterGroups.length === 0" description="当前暂无已选课程" />
        <template v-else>
          <el-table v-loading="selectedLoading" :data="overallRows" border row-key="overallKey">
            <el-table-column type="expand" width="54">
              <template #default="{ row }">
                <div class="course-detail">
                  <el-table :data="row.courses" border size="small">
                    <el-table-column prop="courseId" label="课程号" width="120" />
                    <el-table-column prop="courseName" label="课程名称" min-width="150" />
                    <el-table-column prop="semesterName" label="学期" min-width="130" />
                    <el-table-column prop="teacherName" label="任课教师" width="110" />
                    <el-table-column prop="credit" label="学分" width="80" />
                    <el-table-column prop="finalScore" label="总评" width="90" />
                    <el-table-column prop="gpaPoint" label="绩点" width="90" />
                    <el-table-column label="课程状态" width="110">
                      <template #default="{ row: course }">
                        <el-tag :type="courseStatusType(course)">
                          {{ courseStatus(course) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="majorName" label="专业" min-width="150" />
            <el-table-column prop="courseType" label="课程类型" min-width="140" />
            <el-table-column prop="courseCount" label="课程数" width="100" />
            <el-table-column prop="totalCredits" label="总学分" width="100" />
            <el-table-column label="课程状态" min-width="260">
              <template #default="{ row }">
                <div class="status-tags">
                  <el-tag type="success">通过 {{ row.passedCount }}</el-tag>
                  <el-tag type="danger">未通过 {{ row.failedCount }}</el-tag>
                  <el-tag type="primary">未修 {{ row.unstudiedCount }}</el-tag>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="sub-header semester-course-header">
            <h2>按学期查看</h2>
          </div>
          <el-table v-loading="selectedLoading" :data="semesterGroups" border row-key="semesterKey">
            <el-table-column type="expand" width="54">
              <template #default="{ row }">
                <div class="course-detail">
                  <el-table :data="row.courses" border size="small">
                    <el-table-column prop="courseId" label="课程号" width="120" />
                    <el-table-column prop="courseName" label="课程名称" min-width="150" />
                    <el-table-column prop="courseType" label="课程类型" width="120" />
                    <el-table-column prop="teacherName" label="任课教师" width="110" />
                    <el-table-column prop="credit" label="学分" width="80" />
                    <el-table-column label="上课时间" min-width="180">
                      <template #default="{ row: course }">
                        {{ formatSchedule(course) }}
                      </template>
                    </el-table-column>
                    <el-table-column label="上课地点" min-width="140">
                      <template #default="{ row: course }">
                        {{ formatLocation(course) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="usualScore" label="平时成绩" width="100" />
                    <el-table-column prop="examScore" label="考试成绩" width="100" />
                    <el-table-column prop="finalScore" label="总评" width="90" />
                    <el-table-column prop="gpaPoint" label="绩点" width="90" />
                    <el-table-column label="课程状态" width="110">
                      <template #default="{ row: course }">
                        <el-tag :type="courseStatusType(course)">
                          {{ courseStatus(course) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="选课时间" min-width="170">
                      <template #default="{ row: course }">
                        {{ course.selectionTime || '-' }}
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="semesterName" label="学期" min-width="150" />
            <el-table-column prop="courseCount" label="课程数" width="100" />
            <el-table-column prop="totalCredits" label="总学分" width="100" />
            <el-table-column prop="averageScore" label="平均总评" width="110" />
            <el-table-column label="课程状态" min-width="260">
              <template #default="{ row }">
                <div class="status-tags">
                  <el-tag type="success">通过 {{ row.passedCount }}</el-tag>
                  <el-tag type="danger">未通过 {{ row.failedCount }}</el-tag>
                  <el-tag type="primary">未修 {{ row.unstudiedCount }}</el-tag>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  getAcademicProgress,
  getSelectedCourses,
  getStudentProfile,
  getStudentScores,
  getStudentSemesters
} from '../../api/student'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const selectedLoading = ref(false)
const progressRows = ref([])
const profile = ref({})
const selectedCourses = ref([])
const scores = ref([])

const totalRequiredNumber = computed(() => sumBy(progressRows.value, 'requiredCredit'))
const totalEarnedNumber = computed(() => sumBy(progressRows.value, 'earnedCredit'))
const totalRemainingNumber = computed(() => sumBy(progressRows.value, 'remainingCredit'))

const totalRequired = computed(() => totalRequiredNumber.value.toFixed(1))
const totalEarned = computed(() => totalEarnedNumber.value.toFixed(1))
const totalRemaining = computed(() => totalRemainingNumber.value.toFixed(1))
const overallPercent = computed(() => {
  if (!totalRequiredNumber.value) {
    return 0
  }
  return Math.min(100, Math.round(totalEarnedNumber.value / totalRequiredNumber.value * 100))
})

const courseRows = computed(() => {
  const scoreMap = new Map()
  scores.value.forEach((score) => {
    scoreMap.set(courseKey(score), score)
  })

  return selectedCourses.value.map((course) => ({
    ...course,
    ...scoreMap.get(courseKey(course))
  })).sort((a, b) => {
    const semesterCompare = String(b.semesterName || '').localeCompare(String(a.semesterName || ''), 'zh-CN')
    if (semesterCompare !== 0) return semesterCompare
    return String(a.courseName || '').localeCompare(String(b.courseName || ''), 'zh-CN')
  })
})

const semesterGroups = computed(() => {
  const groups = new Map()
  courseRows.value.forEach((course) => {
    const semesterKey = String(course.semesterId ?? course.semesterName ?? 'unknown')
    if (!groups.has(semesterKey)) {
      groups.set(semesterKey, {
        semesterKey,
        semesterName: course.semesterName || '未分学期',
        courses: []
      })
    }
    groups.get(semesterKey).courses.push(course)
  })

  return [...groups.values()].map((group) => {
    const passedCount = group.courses.filter(course => courseStatus(course) === '通过').length
    const failedCount = group.courses.filter(course => courseStatus(course) === '未通过').length
    const unstudiedCount = group.courses.filter(course => courseStatus(course) === '未修').length
    const scoreRows = group.courses.filter(course => hasScore(course))
    const totalScore = scoreRows.reduce((sum, course) => sum + Number(course.finalScore || 0), 0)
    const totalCredits = group.courses.reduce((sum, course) => sum + Number(course.credit || 0), 0)
    return {
      ...group,
      courseCount: group.courses.length,
      totalCredits: totalCredits.toFixed(1),
      averageScore: scoreRows.length ? (totalScore / scoreRows.length).toFixed(2) : '-',
      passedCount,
      failedCount,
      unstudiedCount
    }
  })
})

const overallRows = computed(() => {
  const majorName = profile.value.majorName || progressRows.value[0]?.majorName || '未注明专业'
  const groups = new Map()
  courseRows.value.forEach((course) => {
    const courseType = course.courseType || '未分类课程'
    const key = `${majorName}-${courseType}`
    if (!groups.has(key)) {
      groups.set(key, {
        overallKey: key,
        majorName,
        courseType,
        courses: []
      })
    }
    groups.get(key).courses.push(course)
  })
  return [...groups.values()].map((group) => {
    const totalCredits = group.courses.reduce((sum, course) => sum + Number(course.credit || 0), 0)
    return {
      ...group,
      courseCount: group.courses.length,
      totalCredits: totalCredits.toFixed(1),
      passedCount: group.courses.filter(course => courseStatus(course) === '通过').length,
      failedCount: group.courses.filter(course => courseStatus(course) === '未通过').length,
      unstudiedCount: group.courses.filter(course => courseStatus(course) === '未修').length
    }
  })
})

function sumBy(rows, key) {
  return rows.reduce((sum, row) => sum + Number(row[key] || 0), 0)
}

function progressPercent(row) {
  const required = Number(row.requiredCredit || 0)
  if (!required) {
    return 0
  }
  return Math.min(100, Math.round(Number(row.earnedCredit || 0) / required * 100))
}

function courseKey(course) {
  if (course.teachingClassId !== null && course.teachingClassId !== undefined) {
    return `class-${course.teachingClassId}`
  }
  return `course-${course.semesterId || course.semesterName || ''}-${course.courseId || course.courseName || ''}`
}

function hasScore(course) {
  return hasScoreValue(course.finalScore)
}

function courseStatus(course) {
  if (!hasScore(course)) return '未修'
  return Number(course.finalScore) >= 60 ? '通过' : '未通过'
}

function courseStatusType(course) {
  if (!hasScore(course)) return 'primary'
  return Number(course.finalScore) >= 60 ? 'success' : 'danger'
}

function hasScoreValue(value) {
  return value !== null && value !== undefined && value !== ''
}

function formatSchedule(course) {
  if (!course.scheduleText && !course.weekRange) {
    return '待排课'
  }
  if (!course.scheduleText) {
    return course.weekRange || '待排课'
  }
  return course.weekRange ? `${course.scheduleText}（${course.weekRange}）` : course.scheduleText
}

function formatLocation(course) {
  const location = `${course.buildingName || ''} ${course.roomName || ''}`.trim()
  return location || '-'
}

async function loadProgress() {
  loading.value = true
  try {
    const response = await getAcademicProgress()
    progressRows.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function loadSelectedCourses() {
  selectedLoading.value = true
  try {
    const [profileResponse, semesterResponse, scoreResponse] = await Promise.all([
      getStudentProfile(),
      getStudentSemesters(),
      getStudentScores()
    ])
    profile.value = profileResponse.data || {}
    const semesterRows = semesterResponse.data || []
    const selectedResponses = semesterRows.length
      ? await Promise.all(semesterRows.map(async (semester) => {
          try {
            return await getSelectedCourses({ semesterId: semester.semesterId })
          } catch {
            return { data: [] }
          }
        }))
      : [await getSelectedCourses()]
    selectedCourses.value = selectedResponses.flatMap(response => response.data || [])
    scores.value = scoreResponse.data || []
  } finally {
    selectedLoading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadProgress(), loadSelectedCourses()])
}

onMounted(() => {
  loadAll()
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

.sub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 28px 0 16px;
}

.sub-header h2 {
  margin: 0;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 18px 20px;
  border: 1px solid #d8e9df;
  border-radius: 8px;
  background: #f8fdff;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  color: var(--sky-700);
  font-size: 26px;
}

.summary-label {
  color: var(--text-muted);
  font-size: 13px;
}

.progress-cell {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 44px;
  align-items: center;
  gap: 10px;
}

.selected-course-section {
  margin-top: 28px;
}

.semester-course-header {
  margin-top: 24px;
}

.course-detail {
  padding: 12px 16px 16px 48px;
  background: #f8fbfd;
}

.status-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 960px) {
  .summary-row {
    grid-template-columns: 1fr;
  }

  .page-header,
  .sub-header {
    align-items: stretch;
    flex-direction: column;
  }

  .course-detail {
    padding-left: 12px;
    overflow-x: auto;
  }
}
</style>
