import request from './request'

export function getAdminStudents() {
  return request({
    url: '/admin/students',
    method: 'get'
  })
}

export function createAdminStudent(data) {
  return request({
    url: '/admin/students',
    method: 'post',
    data
  })
}

export function updateAdminStudent(studentId, data) {
  return request({
    url: `/admin/students/${studentId}`,
    method: 'put',
    data
  })
}

export function deleteAdminStudent(studentId) {
  return request({
    url: `/admin/students/${studentId}`,
    method: 'delete'
  })
}

export function getAdminTeachers() {
  return request({
    url: '/admin/teachers',
    method: 'get'
  })
}

export function createAdminTeacher(data) {
  return request({
    url: '/admin/teachers',
    method: 'post',
    data
  })
}

export function updateAdminTeacher(teacherId, data) {
  return request({
    url: `/admin/teachers/${teacherId}`,
    method: 'put',
    data
  })
}

export function deleteAdminTeacher(teacherId) {
  return request({
    url: `/admin/teachers/${teacherId}`,
    method: 'delete'
  })
}

export function getAdminCourses() {
  return request({
    url: '/admin/courses',
    method: 'get'
  })
}

export function getAdminTeachingClasses() {
  return request({
    url: '/admin/teaching-classes',
    method: 'get'
  })
}

export function getAdminTeachingClassStudents(teachingClassId) {
  return request({
    url: `/admin/teaching-classes/${teachingClassId}/students`,
    method: 'get'
  })
}

export function createAdminCourse(data) {
  return request({
    url: '/admin/courses',
    method: 'post',
    data
  })
}

export function updateAdminCourse(courseId, data) {
  return request({
    url: `/admin/courses/${courseId}`,
    method: 'put',
    data
  })
}

export function deleteAdminCourse(courseId) {
  return request({
    url: `/admin/courses/${courseId}`,
    method: 'delete'
  })
}

export function createAdminTeachingClass(data) {
  return request({
    url: '/admin/teaching-classes',
    method: 'post',
    data
  })
}

export function updateAdminTeachingClass(teachingClassId, data) {
  return request({
    url: `/admin/teaching-classes/${teachingClassId}`,
    method: 'put',
    data
  })
}

export function deleteAdminTeachingClass(teachingClassId) {
  return request({
    url: `/admin/teaching-classes/${teachingClassId}`,
    method: 'delete'
  })
}

export function getAdminCourseSelectionStatistics() {
  return request({
    url: '/admin/statistics/course-selection',
    method: 'get'
  })
}

export function getAdminDashboardOverview() {
  return request({
    url: '/admin/dashboard/overview',
    method: 'get'
  })
}

export function getAdminScoredClasses() {
  return request({
    url: '/admin/statistics/scored-classes',
    method: 'get'
  })
}

export function getAdminScoreDistribution(teachingClassId) {
  return request({
    url: `/admin/statistics/score-distribution/${teachingClassId}`,
    method: 'get'
  })
}

export function getCultivationPlanRequirements() {
  return request({
    url: '/admin/cultivation-plan',
    method: 'get'
  })
}

export function importCultivationPlan(file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: '/admin/cultivation-plan/import',
    method: 'post',
    data
  })
}

export function getAdminOperationLogs(params) {
  return request({
    url: '/admin/operation-logs',
    method: 'get',
    params
  })
}

export function getAdminOperationLogTypes() {
  return request({
    url: '/admin/operation-logs/types',
    method: 'get'
  })
}

export function getCourseSelectionSetting(params) {
  return request({
    url: '/admin/course-selection-setting',
    method: 'get',
    params
  })
}

export function updateCourseSelectionSetting(data) {
  return request({
    url: '/admin/course-selection-setting',
    method: 'put',
    data
  })
}

export function getScoreSubmitSetting() {
  return request({
    url: '/admin/score-submit-setting',
    method: 'get'
  })
}

export function updateScoreSubmitSetting(data) {
  return request({
    url: '/admin/score-submit-setting',
    method: 'put',
    data
  })
}

export function getSubmittedScores() {
  return request({
    url: '/admin/scores/submitted',
    method: 'get'
  })
}

export function rollbackSubmittedScore(scoreId) {
  return request({
    url: `/admin/scores/${scoreId}/rollback`,
    method: 'put'
  })
}

export function getAdminScoreChangeRequests() {
  return request({
    url: '/admin/score-change-requests',
    method: 'get'
  })
}

export function reviewAdminScoreChangeRequest(scoreChangeRequestId, data) {
  return request({
    url: `/admin/score-change-requests/${scoreChangeRequestId}/review`,
    method: 'put',
    data
  })
}

export function getAdminScoreReviewRequests() {
  return request({
    url: '/admin/score-review-requests',
    method: 'get'
  })
}

export function reviewAdminScoreReviewRequest(requestId, data) {
  return request({
    url: `/admin/score-review-requests/${requestId}/review`,
    method: 'put',
    data
  })
}

export function getAdminMakeupRequests() {
  return request({
    url: '/admin/makeup-requests',
    method: 'get'
  })
}

export function reviewAdminMakeupRequest(requestId, data) {
  return request({
    url: `/admin/makeup-requests/${requestId}/review`,
    method: 'put',
    data
  })
}

export function updateAdminMakeupResult(requestId, data) {
  return request({
    url: `/admin/makeup-requests/${requestId}/result`,
    method: 'put',
    data
  })
}

export function getAdminRetakeRequests() {
  return request({
    url: '/admin/retake-requests',
    method: 'get'
  })
}

export function reviewAdminRetakeRequest(requestId, data) {
  return request({
    url: `/admin/retake-requests/${requestId}/review`,
    method: 'put',
    data
  })
}

export function getDepartmentOptions() {
  return request({
    url: '/admin/departments',
    method: 'get'
  })
}

export function getSemesterOptions() {
  return request({
    url: '/admin/semesters',
    method: 'get'
  })
}

export function getSemesterManagement() {
  return request({
    url: '/admin/semester-management',
    method: 'get'
  })
}

export function createSemester(data) {
  return request({
    url: '/admin/semesters',
    method: 'post',
    data
  })
}

export function updateSemester(semesterId, data) {
  return request({
    url: `/admin/semesters/${semesterId}`,
    method: 'put',
    data
  })
}

export function deleteSemester(semesterId) {
  return request({
    url: `/admin/semesters/${semesterId}`,
    method: 'delete'
  })
}

export function setCurrentSemester(semesterId) {
  return request({
    url: `/admin/semesters/${semesterId}/current`,
    method: 'put'
  })
}

export function getTeacherOptions() {
  return request({
    url: '/admin/teacher-options',
    method: 'get'
  })
}

export function getClassroomOptions() {
  return request({
    url: '/admin/classrooms',
    method: 'get'
  })
}

export function getCourseOptions() {
  return request({
    url: '/admin/course-options',
    method: 'get'
  })
}

export function getMajorOptions() {
  return request({
    url: '/admin/majors',
    method: 'get'
  })
}

export function getClassOptions() {
  return request({
    url: '/admin/classes',
    method: 'get'
  })
}

export function getAdminCourseSelectionRequests() {
  return request({
    url: '/admin/course-selection-requests',
    method: 'get'
  })
}

export function reviewAdminCourseSelectionRequest(requestId, data) {
  return request({
    url: `/admin/course-selection-requests/${requestId}/review`,
    method: 'put',
    data
  })
}

export function getAdminCourseDropRequests() {
  return request({
    url: '/admin/course-drop-requests',
    method: 'get'
  })
}

export function reviewAdminCourseDropRequest(dropRequestId, data) {
  return request({
    url: `/admin/course-drop-requests/${dropRequestId}/review`,
    method: 'put',
    data
  })
}

export function getAdminScheduleAdjustRequests() {
  return request({
    url: '/admin/schedule-adjust-requests',
    method: 'get'
  })
}

export function reviewAdminScheduleAdjustRequest(adjustRequestId, data) {
  return request({
    url: `/admin/schedule-adjust-requests/${adjustRequestId}/review`,
    method: 'put',
    data
  })
}

export function getAdminExams() {
  return request({
    url: '/admin/exams',
    method: 'get'
  })
}

export function generateAdminExams() {
  return request({
    url: '/admin/exams/generate',
    method: 'post'
  })
}

export function updateAdminExam(examId, data) {
  return request({
    url: `/admin/exams/${examId}`,
    method: 'put',
    data
  })
}

export function deleteAdminExam(examId) {
  return request({
    url: `/admin/exams/${examId}`,
    method: 'delete'
  })
}

export function getAdminExamChangeRequests() {
  return request({
    url: '/admin/exam-change-requests',
    method: 'get'
  })
}

export function reviewAdminExamChangeRequest(examChangeRequestId, data) {
  return request({
    url: `/admin/exam-change-requests/${examChangeRequestId}/review`,
    method: 'put',
    data
  })
}
