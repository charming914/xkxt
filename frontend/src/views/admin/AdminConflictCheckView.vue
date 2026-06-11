<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>冲突检测中心</h1>
        <p>集中检测教师排课、教室占用、教学班容量和考试安排中的可疑冲突。</p>
      </div>
      <div class="toolbar">
        <el-button type="primary" :loading="loading" @click="loadData">重新检测</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="filters.semesterId" clearable filterable placeholder="按学期筛选">
        <el-option v-for="item in semesters" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.category" clearable filterable placeholder="问题类型">
        <el-option v-for="category in categoryOptions" :key="category" :label="category" :value="category" />
      </el-select>
      <el-input
        v-model.trim="filters.keyword"
        clearable
        placeholder="搜索课程 / 教师 / 教室 / 说明"
        style="width: 260px"
      />
    </div>

    <div class="metric-grid">
      <div class="metric-item">
        <span>问题总数</span>
        <strong>{{ issues.length }}</strong>
      </div>
      <div class="metric-item">
        <span>高优先级</span>
        <strong>{{ highCount }}</strong>
      </div>
      <div class="metric-item">
        <span>教学班问题</span>
        <strong>{{ teachingIssueCount }}</strong>
      </div>
      <div class="metric-item">
        <span>考试问题</span>
        <strong>{{ examIssueCount }}</strong>
      </div>
    </div>

    <el-empty v-if="!loading && filteredIssues.length === 0" description="暂未发现匹配问题" />

    <template v-else>
      <el-table v-loading="loading" :data="pagedData" border>
        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="severityType(row.severity)">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="问题类型" width="130" />
        <el-table-column prop="target" label="关联对象" min-width="170" />
        <el-table-column prop="detail" label="问题说明" min-width="260" show-overflow-tooltip />
        <el-table-column prop="evidence" label="冲突依据" min-width="260" show-overflow-tooltip />
        <el-table-column prop="suggestion" label="建议处理" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openResolveDialog(row)">处理</el-button>
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

    <el-dialog v-model="resolveDialog.visible" :title="resolveDialogTitle" width="760px">
      <div class="resolve-meta">
        <el-tag :type="severityType(resolveDialog.issue?.severity)" effect="plain">
          {{ resolveDialog.issue?.category || '-' }}
        </el-tag>
        <span>{{ resolveDialog.issue?.detail || '-' }}</span>
      </div>

      <el-form v-if="resolveDialog.scope === 'teaching'" label-position="top" :model="teachingForm">
        <el-form-item v-if="resolveTargets.length > 1" label="处理对象">
          <el-select v-model="resolveDialog.selectedKey" filterable @change="handleResolveTargetChange">
            <el-option
              v-for="item in resolveTargets"
              :key="item.key"
              :label="item.label"
              :value="item.key"
            />
          </el-select>
        </el-form-item>
        <div v-if="showRoomSuggestions" class="room-suggestions">
          <div class="room-suggestions-header">
            <strong>空闲教室建议</strong>
            <span v-if="availableRoomSlot">{{ availableRoomSlot }}</span>
          </div>
          <div v-if="availableRoomSuggestions.length" class="room-suggestions-list">
            <el-button
              v-for="room in availableRoomSuggestions"
              :key="room.value"
              size="small"
              type="success"
              plain
              @click="applySuggestedRoom(room)"
            >
              {{ room.label }}
            </el-button>
          </div>
          <el-alert
            v-else
            type="info"
            :closable="false"
            title="当前时段暂无空闲教室建议，可手动调整时间或教室。"
          />
        </div>
        <div class="grid-form">
          <el-form-item label="课程">
            <el-input v-model="teachingForm.courseName" disabled />
          </el-form-item>
          <el-form-item label="开课日期">
            <div class="date-field">
              <el-date-picker
                v-model="teachingForm.courseStartDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="可留空，保存时自动用学期开始日期"
                style="width: 100%"
              />
              <el-button size="small" plain @click="fillCourseStartDateFromSemester">用学期开始日期</el-button>
            </div>
          </el-form-item>
          <el-form-item label="任课教师">
            <el-select v-model="teachingForm.teacherId" filterable>
              <el-option v-for="item in teacherOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="教室">
            <el-select v-model="teachingForm.classroomId" filterable clearable placeholder="可留空">
              <el-option v-for="item in classrooms" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="周次">
            <el-input v-model.trim="teachingForm.weekRange" placeholder="例如：1-16周" />
          </el-form-item>
          <el-form-item label="容量">
            <el-input-number v-model="teachingForm.maxStudents" :min="1" />
            <span class="form-hint">已选 {{ teachingForm.selectedCount || 0 }} 人</span>
          </el-form-item>
          <el-form-item label="班型">
            <el-switch
              v-model="teachingForm.isRetakeClass"
              :active-value="1"
              :inactive-value="0"
              active-text="重修班"
              inactive-text="普通班"
            />
          </el-form-item>
        </div>
      </el-form>

      <el-form v-else label-position="top" :model="examForm">
        <el-form-item v-if="resolveTargets.length > 1" label="处理对象">
          <el-select v-model="resolveDialog.selectedKey" filterable @change="handleResolveTargetChange">
            <el-option
              v-for="item in resolveTargets"
              :key="item.key"
              :label="item.label"
              :value="item.key"
            />
          </el-select>
        </el-form-item>
        <div v-if="showRoomSuggestions" class="room-suggestions">
          <div class="room-suggestions-header">
            <strong>空闲教室建议</strong>
            <span v-if="availableRoomSlot">{{ availableRoomSlot }}</span>
          </div>
          <div v-if="availableRoomSuggestions.length" class="room-suggestions-list">
            <el-button
              v-for="room in availableRoomSuggestions"
              :key="room.value"
              size="small"
              type="success"
              plain
              @click="applySuggestedRoom(room)"
            >
              {{ room.label }}
            </el-button>
          </div>
          <el-alert
            v-else
            type="info"
            :closable="false"
            title="当前时段暂无空闲教室建议，可手动调整时间或教室。"
          />
        </div>
        <div class="grid-form">
          <el-form-item label="课程">
            <el-input v-model="examForm.courseName" disabled />
          </el-form-item>
          <el-form-item label="考试日期">
            <el-date-picker
              v-model="examForm.examDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择考试日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-time-picker
              v-model="examForm.startTime"
              value-format="HH:mm:ss"
              placeholder="开始时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-time-picker
              v-model="examForm.endTime"
              value-format="HH:mm:ss"
              placeholder="结束时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="考试教室">
            <el-select v-model="examForm.classroomId" filterable clearable placeholder="可留空">
              <el-option v-for="item in classrooms" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="resolveDialog.visible = false">取消</el-button>
        <el-button
          v-if="resolveDialog.scope === 'exam'"
          type="danger"
          plain
          :loading="resolveDialog.saving"
          @click="handleDeleteSelectedExam"
        >
          删除考试安排
        </el-button>
        <el-button type="primary" :loading="resolveDialog.saving" @click="handleSaveResolve">保存处理</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteAdminExam,
  getAdminExams,
  getAdminTeachingClasses,
  getSemesterOptions,
  getSemesterManagement,
  getClassroomOptions,
  getTeacherOptions,
  updateAdminExam,
  updateAdminTeachingClass
} from '../../api/admin'
import { useClientPagination } from '../../composables/useClientPagination'

const loading = ref(false)
const teachingClasses = ref([])
const exams = ref([])
const semesters = ref([])
const semesterDetails = ref([])
const classrooms = ref([])
const teacherOptions = ref([])
const resolveDialog = reactive({
  visible: false,
  saving: false,
  scope: 'teaching',
  selectedKey: '',
  issue: null
})
const teachingForm = reactive({
  teachingClassId: null,
  semesterId: null,
  courseId: '',
  courseName: '',
  courseStartDate: '',
  teacherId: '',
  classroomId: '',
  weekRange: '1-16周',
  isRetakeClass: 0,
  maxStudents: 60,
  selectedCount: 0
})
const examForm = reactive({
  examId: null,
  courseName: '',
  examDate: '',
  startTime: '',
  endTime: '',
  classroomId: ''
})
const filters = reactive({
  semesterId: '',
  category: '',
  keyword: ''
})

const checkedTeachingClasses = computed(() => {
  if (!filters.semesterId) {
    return teachingClasses.value
  }
  return teachingClasses.value.filter(row => String(row.semesterId) === String(filters.semesterId))
})

const selectedSemesterLabel = computed(() => {
  if (!filters.semesterId) {
    return ''
  }
  return semesters.value.find(item => String(item.value) === String(filters.semesterId))?.label || ''
})

const checkedExams = computed(() => {
  if (!selectedSemesterLabel.value) {
    return exams.value
  }
  return exams.value.filter(row => row.semesterName === selectedSemesterLabel.value)
})

const issues = computed(() => {
  return [
    ...detectTeachingClassIssues(checkedTeachingClasses.value),
    ...detectExamIssues(checkedExams.value)
  ].sort((a, b) => severityWeight(b.severity) - severityWeight(a.severity))
})

const categoryOptions = computed(() => {
  return [...new Set(issues.value.map(issue => issue.category))]
})

const filteredIssues = computed(() => {
  const keyword = filters.keyword.toLowerCase()
  return issues.value.filter(issue => {
    const matchCategory = !filters.category || issue.category === filters.category
    const haystack = [issue.target, issue.detail, issue.evidence, issue.suggestion, issue.category]
      .join(' ')
      .toLowerCase()
    const matchKeyword = !keyword || haystack.includes(keyword)
    return matchCategory && matchKeyword
  })
})

const highCount = computed(() => issues.value.filter(issue => issue.severity === '高').length)
const teachingIssueCount = computed(() => issues.value.filter(issue => issue.scope === 'teaching').length)
const examIssueCount = computed(() => issues.value.filter(issue => issue.scope === 'exam').length)
const resolveDialogTitle = computed(() => {
  return resolveDialog.scope === 'exam' ? '处理考试冲突' : '处理教学班冲突'
})
const resolveTargets = computed(() => {
  const records = resolveDialog.issue?.records || []
  return records.map(row => ({
    key: recordKey(resolveDialog.scope, row),
    label: resolveDialog.scope === 'exam' ? examLabel(row) : teachingClassLabel(row),
    row
  }))
})
const selectedResolveTarget = computed(() => {
  return resolveTargets.value.find(item => item.key === resolveDialog.selectedKey)?.row || null
})
const showRoomSuggestions = computed(() => {
  const category = resolveDialog.issue?.category || ''
  return ['教室占用冲突', '未安排教室', '考试教室冲突', '考试未安排教室'].includes(category) ||
    Boolean(resolveDialog.issue?.suggestions?.length)
})
const availableRoomSuggestions = computed(() => {
  const issue = resolveDialog.issue
  if (!issue || !showRoomSuggestions.value) {
    return []
  }
  const row = selectedResolveTarget.value
  if (!row) {
    return []
  }
  if (resolveDialog.scope === 'exam') {
    if (!examForm.examDate || !examForm.startTime || !examForm.endTime) {
      return []
    }
    return suggestFreeClassroomsForExam({
      ...row,
      examDate: examForm.examDate,
      startTime: examForm.startTime,
      endTime: examForm.endTime
    })
  }
  if (issue.suggestions?.length) {
    return issue.suggestions
  }
  return suggestFreeClassroomsForTeachingRecord(row)
})
const availableRoomSlot = computed(() => {
  if (resolveDialog.scope === 'exam') {
    return `${examForm.examDate || '-'} ${timeRange(examForm.startTime, examForm.endTime) || ''}`.trim()
  }
  if (resolveDialog.issue?.suggestionSlot) {
    return resolveDialog.issue.suggestionSlot
  }
  const row = selectedResolveTarget.value
  if (!row) {
    return ''
  }
  return [row.scheduleText, row.weekRange].filter(Boolean).join(' ')
})

const {
  currentPage,
  pageSize,
  total,
  pagedData,
  handleSizeChange,
  handleCurrentChange
} = useClientPagination(filteredIssues, 10)

async function loadData() {
  loading.value = true
  try {
    const [semesterRes, semesterDetailRes, teachingRes, examRes, classroomRes, teacherRes] = await Promise.all([
      getSemesterOptions(),
      getSemesterManagement(),
      getAdminTeachingClasses(),
      getAdminExams(),
      getClassroomOptions(),
      getTeacherOptions()
    ])
    semesters.value = semesterRes.data || []
    semesterDetails.value = semesterDetailRes.data || []
    teachingClasses.value = teachingRes.data || []
    exams.value = examRes.data || []
    classrooms.value = classroomRes.data || []
    teacherOptions.value = teacherRes.data || []
  } finally {
    loading.value = false
  }
}

function detectTeachingClassIssues(rows) {
  const result = []
  rows.forEach(row => {
    const maxStudents = Number(row.maxStudents || 0)
    const selectedCount = Number(row.selectedCount || 0)
    const target = teachingClassLabel(row)

    if (maxStudents <= 0) {
      result.push(makeIssue('teaching', '容量异常', '中', target, '教学班容量未设置或小于等于 0', '容量会影响选课人数校验', '在当前页面重新设置容量', '/admin/teaching-classes', { records: [row], defaultRecord: row }))
    } else if (selectedCount > maxStudents) {
      result.push(makeIssue('teaching', '容量超限', '高', target, `已选 ${selectedCount} 人，超过容量 ${maxStudents} 人`, '选课人数已经超过教学班容量', '在当前页面扩容或调整教学班', '/admin/teaching-classes', { records: [row], defaultRecord: row }))
    }

    if (!row.scheduleText) {
      result.push(makeIssue('teaching', '未排课', '低', target, '教学班暂无上课时间', '排课为空会影响课表和冲突校验', '在当前页面重新保存教学班，系统会重新排课', '/admin/teaching-classes', { records: [row], defaultRecord: row }))
    }

    if (!row.classroomId) {
      result.push(makeIssue('teaching', '未安排教室', '低', target, '教学班暂无上课教室', '教室为空会影响学生课表展示', '在当前页面指定教室并保存', '/admin/teaching-classes', { records: [row], defaultRecord: row }))
    }
  })

  const pairKeys = new Set()
  for (let index = 0; index < rows.length; index += 1) {
    for (let nextIndex = index + 1; nextIndex < rows.length; nextIndex += 1) {
      const current = rows[index]
      const next = rows[nextIndex]
      if (String(current.semesterId) !== String(next.semesterId)) {
        continue
      }
      const overlap = findScheduleOverlap(current, next)
      if (!overlap) {
        continue
      }
      if (current.teacherId && current.teacherId === next.teacherId) {
        addPairIssue(
          result,
          pairKeys,
          '教师时间冲突',
          '高',
          current,
          next,
          overlap,
          `${current.teacherName || current.teacherId} 同一时间被安排到多个教学班`,
          '调整其中一个教学班的上课时间'
        )
      }
      if (current.classroomId && current.classroomId === next.classroomId) {
        const overlapSession = findOverlapSession(current, next)
        const freeRooms = suggestFreeClassroomsForTeaching(overlapSession, current.semesterId)
        addPairIssue(
          result,
          pairKeys,
          '教室占用冲突',
          '高',
          current,
          next,
          overlap,
          `${classroomLabel(current)} 同一时间被多个教学班占用`,
          freeRooms.length
            ? `可改用 ${freeRooms.length} 间空闲教室，点击查看建议`
            : '调整其中一个教学班的教室或上课时间',
          {
            suggestions: freeRooms,
            suggestionSlot: overlapSession
              ? `周${overlapSession.weekDay} ${overlapSession.start}-${overlapSession.end}节 ${current.weekRange || ''}`.trim()
              : ''
          }
        )
      }
    }
  }
  return result
}

function detectExamIssues(rows) {
  const result = []
  rows.forEach(row => {
    const target = examLabel(row)
    if (!row.classroomId) {
      result.push(makeIssue('exam', '考试未安排教室', '低', target, '考试安排没有指定教室', '学生端会显示未指定考试地点', '在当前页面补充考试教室', '/admin/exams', { records: [row], defaultRecord: row }))
    }
    if (!isValidTimeRange(row.startTime, row.endTime)) {
      result.push(makeIssue('exam', '考试时间异常', '高', target, '考试开始时间不早于结束时间', `${timeRange(row.startTime, row.endTime) || '时间缺失'}`, '在当前页面修正考试时间', '/admin/exams', { records: [row], defaultRecord: row }))
    }
  })

  const pairKeys = new Set()
  for (let index = 0; index < rows.length; index += 1) {
    for (let nextIndex = index + 1; nextIndex < rows.length; nextIndex += 1) {
      const current = rows[index]
      const next = rows[nextIndex]
      if (!sameExamSlot(current, next)) {
        continue
      }
      if (current.classroomId && current.classroomId === next.classroomId) {
        const freeRooms = suggestFreeClassroomsForExam(current)
        addExamPairIssue(
          result,
          pairKeys,
          '考试教室冲突',
          '高',
          current,
          next,
          `${classroomLabel(current)} 同一考试时段被重复使用`,
          freeRooms.length
            ? `可改用 ${freeRooms.length} 间空闲教室，点击查看建议`
            : '调整其中一场考试的时间或教室',
          {
            suggestions: freeRooms,
            suggestionSlot: `${formatDate(current.examDate)} ${timeRange(current.startTime, current.endTime)}`
          }
        )
      }
      if (current.teacherId && current.teacherId === next.teacherId) {
        addExamPairIssue(
          result,
          pairKeys,
          '教师考试冲突',
          '中',
          current,
          next,
          `${current.teacherName || current.teacherId} 同一时段关联多场考试`,
          '核对考试监考或调整其中一场考试'
        )
      }
      if (current.teachingClassId && current.teachingClassId === next.teachingClassId) {
        addExamPairIssue(
          result,
          pairKeys,
          '重复考试安排',
          '中',
          current,
          next,
          `教学班 ${current.teachingClassId} 存在重复考试安排`,
          '删除重复考试或合并安排'
        )
      }
    }
  }
  return result
}

function makeIssue(scope, category, severity, target, detail, evidence, suggestion, route = '/admin/teaching-classes', extra = {}) {
  return {
    issueId: `${scope}-${category}-${target}-${detail}`,
    scope,
    category,
    severity,
    target,
    detail,
    evidence,
    suggestion,
    route,
    suggestions: extra.suggestions || null,
    suggestionSlot: extra.suggestionSlot || '',
    records: extra.records || [],
    defaultRecord: extra.defaultRecord || null
  }
}

function addPairIssue(result, keys, category, severity, current, next, overlap, detail, suggestion, extra = {}) {
  const key = [category, current.teachingClassId, next.teachingClassId].sort().join('|')
  if (keys.has(key)) {
    return
  }
  keys.add(key)
  result.push(makeIssue(
    'teaching',
    category,
    severity,
    `${teachingClassLabel(current)} / ${teachingClassLabel(next)}`,
    detail,
    `${overlap}；${current.courseName || '-'} 与 ${next.courseName || '-'}`,
    suggestion,
    '/admin/teaching-classes',
    {
      records: extra.records || [current, next],
      defaultRecord: extra.defaultRecord || next,
      ...extra
    }
  ))
}

function addExamPairIssue(result, keys, category, severity, current, next, detail, suggestion, extra = {}) {
  const key = [category, current.examId, next.examId].sort().join('|')
  if (keys.has(key)) {
    return
  }
  keys.add(key)
  result.push(makeIssue(
    'exam',
    category,
    severity,
    `${examLabel(current)} / ${examLabel(next)}`,
    detail,
    `${formatDate(current.examDate)} ${timeRange(current.startTime, current.endTime)}；${current.courseName || '-'} 与 ${next.courseName || '-'}`,
    suggestion,
    '/admin/exams',
    {
      records: extra.records || [current, next],
      defaultRecord: extra.defaultRecord || next,
      ...extra
    }
  ))
}

function findScheduleOverlap(current, next) {
  const currentSessions = parseScheduleSessions(current)
  const nextSessions = parseScheduleSessions(next)
  for (const currentSession of currentSessions) {
    for (const nextSession of nextSessions) {
      if (sessionsOverlap(currentSession, nextSession)) {
        return `周${currentSession.weekDay} ${currentSession.start}-${currentSession.end}节 ${current.weekRange || ''}`.trim()
      }
    }
  }
  return ''
}

function findOverlapSession(current, next) {
  const currentSessions = parseScheduleSessions(current)
  const nextSessions = parseScheduleSessions(next)
  for (const currentSession of currentSessions) {
    for (const nextSession of nextSessions) {
      if (sessionsOverlap(currentSession, nextSession)) {
        return currentSession
      }
    }
  }
  return null
}

// 建议在该时段空闲的教室（教学班维度）
function suggestFreeClassroomsForTeaching(session, semesterId) {
  if (!session) {
    return []
  }
  const occupied = new Set()
  teachingClasses.value.forEach(row => {
    if (String(row.semesterId) !== String(semesterId) || !row.classroomId) {
      return
    }
    const sessions = parseScheduleSessions(row)
    if (sessions.some(item => sessionsOverlap(item, session))) {
      occupied.add(String(row.classroomId))
    }
  })
  return classrooms.value.filter(room => !occupied.has(String(room.value)))
}

function suggestFreeClassroomsForTeachingRecord(targetRow) {
  const sessions = parseScheduleSessions(targetRow)
  if (!sessions.length) {
    return []
  }
  return classrooms.value.filter(room => {
    return sessions.every(session => isTeachingRoomFree(
      room.value,
      session,
      targetRow.semesterId,
      targetRow.teachingClassId
    ))
  })
}

function isTeachingRoomFree(classroomId, session, semesterId, ignoredTeachingClassId) {
  return !teachingClasses.value.some(row => {
    if (String(row.semesterId) !== String(semesterId) ||
        String(row.teachingClassId) === String(ignoredTeachingClassId) ||
        String(row.classroomId) !== String(classroomId)) {
      return false
    }
    return parseScheduleSessions(row).some(item => sessionsOverlap(item, session))
  })
}

// 建议在该考试时段空闲的教室（考试维度）
function suggestFreeClassroomsForExam(current) {
  const occupied = new Set()
  exams.value.forEach(row => {
    if (!row.classroomId) {
      return
    }
    if (String(row.examId) === String(current.examId)) {
      return
    }
    if (sameExamSlot(current, row)) {
      occupied.add(String(row.classroomId))
    }
  })
  return classrooms.value.filter(room => !occupied.has(String(room.value)))
}

function openResolveDialog(issue) {
  const records = issue.records || []
  if (records.length === 0) {
    ElMessage.warning('当前问题缺少可处理对象，请刷新后重试')
    return
  }
  resolveDialog.issue = issue
  resolveDialog.scope = issue.scope
  resolveDialog.selectedKey = recordKey(issue.scope, issue.defaultRecord || records[0])
  fillResolveForm(issue.defaultRecord || records[0])
  resolveDialog.visible = true
}

function handleResolveTargetChange() {
  const target = resolveTargets.value.find(item => item.key === resolveDialog.selectedKey)
  if (target) {
    fillResolveForm(target.row)
  }
}

function fillResolveForm(row) {
  if (resolveDialog.scope === 'exam') {
    Object.assign(examForm, {
      examId: row.examId,
      courseName: row.courseName || '',
      examDate: dateInputValue(row.examDate),
      startTime: timeInputValue(row.startTime),
      endTime: timeInputValue(row.endTime),
      classroomId: row.classroomId || ''
    })
    return
  }
  Object.assign(teachingForm, {
    teachingClassId: row.teachingClassId,
    semesterId: row.semesterId,
    courseId: row.courseId,
    courseName: row.courseName || '',
    courseStartDate: dateInputValue(row.courseStartDate) || semesterStartDate(row.semesterId),
    teacherId: row.teacherId || '',
    classroomId: row.classroomId || '',
    weekRange: row.weekRange || '1-16周',
    isRetakeClass: Number(row.isRetakeClass) === 1 ? 1 : 0,
    maxStudents: Number(row.maxStudents || 60),
    selectedCount: Number(row.selectedCount || 0)
  })
}

function applySuggestedRoom(room) {
  if (resolveDialog.scope === 'exam') {
    examForm.classroomId = room.value
    return
  }
  teachingForm.classroomId = room.value
}

async function handleSaveResolve() {
  if (resolveDialog.scope === 'exam') {
    await saveExamResolve()
    return
  }
  await saveTeachingResolve()
}

async function saveTeachingResolve() {
  const courseStartDate = teachingForm.courseStartDate || semesterStartDate(teachingForm.semesterId)
  if (!courseStartDate) {
    ElMessage.warning('请填写开课日期，或先在学期管理中维护该学期开始日期')
    return
  }
  if (!teachingForm.teacherId) {
    ElMessage.warning('请选择任课教师')
    return
  }
  if (Number(teachingForm.maxStudents) < Number(teachingForm.selectedCount)) {
    ElMessage.warning('容量不能小于已选人数')
    return
  }
  resolveDialog.saving = true
  try {
    await updateAdminTeachingClass(teachingForm.teachingClassId, {
      semesterId: teachingForm.semesterId,
      courseStartDate,
      courseId: teachingForm.courseId,
      teacherId: teachingForm.teacherId,
      classroomId: teachingForm.classroomId || '',
      weekRange: teachingForm.weekRange || '1-16周',
      isRetakeClass: teachingForm.isRetakeClass,
      maxStudents: teachingForm.maxStudents
    })
    ElMessage.success('教学班已更新，冲突检测已刷新')
    resolveDialog.visible = false
    await loadData()
  } finally {
    resolveDialog.saving = false
  }
}

function fillCourseStartDateFromSemester() {
  const startDate = semesterStartDate(teachingForm.semesterId)
  if (!startDate) {
    ElMessage.warning('当前学期没有开始日期')
    return
  }
  teachingForm.courseStartDate = startDate
}

async function saveExamResolve() {
  if (!examForm.examDate || !examForm.startTime || !examForm.endTime) {
    ElMessage.warning('请完整填写考试日期与时间')
    return
  }
  if (examForm.startTime >= examForm.endTime) {
    ElMessage.warning('开始时间必须早于结束时间')
    return
  }
  resolveDialog.saving = true
  try {
    await updateAdminExam(examForm.examId, {
      examDate: examForm.examDate,
      startTime: examForm.startTime,
      endTime: examForm.endTime,
      classroomId: examForm.classroomId || ''
    })
    ElMessage.success('考试安排已更新，冲突检测已刷新')
    resolveDialog.visible = false
    await loadData()
  } finally {
    resolveDialog.saving = false
  }
}

async function handleDeleteSelectedExam() {
  if (!examForm.examId) {
    return
  }
  await ElMessageBox.confirm(`确认删除「${examForm.courseName || examForm.examId}」的考试安排？`, '提示', {
    type: 'warning'
  })
  resolveDialog.saving = true
  try {
    await deleteAdminExam(examForm.examId)
    ElMessage.success('考试安排已删除，冲突检测已刷新')
    resolveDialog.visible = false
    await loadData()
  } finally {
    resolveDialog.saving = false
  }
}

function parseScheduleSessions(row) {
  return String(row.scheduleText || '')
    .split(/[；;]/)
    .map(text => {
      const match = text.match(/周([一二三四五六日天\d])\s*(\d+)\s*-\s*(\d+)节(?:\(([^)]+)\))?/)
      if (!match) {
        return null
      }
      return {
        weekDay: normalizeWeekDay(match[1]),
        start: Number(match[2]),
        end: Number(match[3]),
        parity: match[4] || '全周',
        weekRange: parseWeekRange(row.weekRange)
      }
    })
    .filter(Boolean)
}

function normalizeWeekDay(value) {
  const map = { 一: 1, 二: 2, 三: 3, 四: 4, 五: 5, 六: 6, 日: 7, 天: 7 }
  return map[value] || Number(value)
}

function parseWeekRange(value) {
  const match = String(value || '').match(/(\d+)\s*-\s*(\d+)/)
  if (!match) {
    return null
  }
  return {
    start: Number(match[1]),
    end: Number(match[2])
  }
}

function sessionsOverlap(current, next) {
  return current.weekDay === next.weekDay &&
    rangesOverlap(current.start, current.end, next.start, next.end) &&
    weekRangesOverlap(current.weekRange, next.weekRange) &&
    parityOverlap(current.parity, next.parity)
}

function rangesOverlap(start, end, nextStart, nextEnd) {
  return start <= nextEnd && nextStart <= end
}

function weekRangesOverlap(current, next) {
  if (!current || !next) {
    return true
  }
  return rangesOverlap(current.start, current.end, next.start, next.end)
}

function parityOverlap(current, next) {
  if (!current || !next || current === '全周' || next === '全周') {
    return true
  }
  return current === next
}

function sameExamSlot(current, next) {
  return sameExamSemester(current, next) &&
    formatDate(current.examDate) === formatDate(next.examDate) &&
    timeRangesOverlap(current.startTime, current.endTime, next.startTime, next.endTime)
}

function sameExamSemester(current, next) {
  const currentSemester = current.semesterId ?? current.semesterName
  const nextSemester = next.semesterId ?? next.semesterName
  if (!currentSemester || !nextSemester) {
    return true
  }
  return String(currentSemester) === String(nextSemester)
}

function timeRangesOverlap(start, end, nextStart, nextEnd) {
  const startMinutes = toMinutes(start)
  const endMinutes = toMinutes(end)
  const nextStartMinutes = toMinutes(nextStart)
  const nextEndMinutes = toMinutes(nextEnd)
  if ([startMinutes, endMinutes, nextStartMinutes, nextEndMinutes].some(Number.isNaN)) {
    return false
  }
  return startMinutes < nextEndMinutes && nextStartMinutes < endMinutes
}

function isValidTimeRange(start, end) {
  const startMinutes = toMinutes(start)
  const endMinutes = toMinutes(end)
  return !Number.isNaN(startMinutes) && !Number.isNaN(endMinutes) && startMinutes < endMinutes
}

function toMinutes(value) {
  const match = String(value || '').match(/^(\d{1,2}):(\d{2})/)
  if (!match) {
    return Number.NaN
  }
  return Number(match[1]) * 60 + Number(match[2])
}

function teachingClassLabel(row) {
  return `${row.courseName || '-'}（教学班 ${row.teachingClassId || '-'}）`
}

function examLabel(row) {
  return `${row.courseName || '-'}（考试 ${row.examId || '-'}）`
}

function classroomLabel(row) {
  return [row.buildingName, row.roomName].filter(Boolean).join(' ') || row.classroomId || '-'
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

function dateInputValue(value) {
  return value ? String(value).slice(0, 10) : ''
}

function semesterStartDate(semesterId) {
  const semester = semesterDetails.value.find(item => String(item.semesterId) === String(semesterId))
  return dateInputValue(semester?.startDate)
}

function timeInputValue(value) {
  return value ? String(value).slice(0, 8) : ''
}

function recordKey(scope, row) {
  return scope === 'exam' ? `exam-${row.examId}` : `teaching-${row.teachingClassId}`
}

function severityWeight(severity) {
  return { 高: 3, 中: 2, 低: 1 }[severity] || 0
}

function severityType(severity) {
  if (severity === '高') return 'danger'
  if (severity === '中') return 'warning'
  return 'info'
}

onMounted(() => {
  loadData()
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

.toolbar,
.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-bar {
  margin-bottom: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.metric-item {
  padding: 14px 16px;
  border: 1px solid #d7e8f5;
  border-radius: 8px;
  background: #f8fbfd;
}

.metric-item span {
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.room-suggestions {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #d8e9df;
  border-radius: 8px;
  background: #f8fbfd;
}

.room-suggestions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.room-suggestions-header strong {
  font-size: 14px;
}

.room-suggestions-header span {
  color: var(--text-muted);
  font-size: 12px;
}

.room-suggestions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 2px 0;
  max-height: 280px;
  overflow-y: auto;
}

.resolve-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  color: #475569;
  font-size: 14px;
}

.grid-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.form-hint {
  margin-left: 10px;
  color: var(--text-muted);
  font-size: 12px;
}

.date-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-select) {
  width: 100%;
}

@media (max-width: 920px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .grid-form {
    grid-template-columns: 1fr;
  }
}
</style>
