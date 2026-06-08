import request from './request'

export function getTeacherProfile() {
  return request({
    url: '/teacher/profile',
    method: 'get'
  })
}

export function updateTeacherProfile(data) {
  return request({
    url: '/teacher/profile',
    method: 'put',
    data
  })
}

export function getTeacherTodos() {
  return request({
    url: '/teacher/todos',
    method: 'get'
  })
}

export function getTeacherCurrentSemester() {
  return request({
    url: '/teacher/current-semester',
    method: 'get'
  })
}

export function getTeachingClasses(params) {
  return request({
    url: '/teacher/teaching-classes',
    method: 'get',
    params
  })
}

export function getTeachingClassDetail(teachingClassId) {
  return request({
    url: `/teacher/teaching-classes/${teachingClassId}`,
    method: 'get'
  })
}

export function getTeachingClassStudents(teachingClassId) {
  return request({
    url: `/teacher/teaching-classes/${teachingClassId}/students`,
    method: 'get'
  })
}

export function getTeacherScores() {
  return request({
    url: '/teacher/scores',
    method: 'get'
  })
}

export function getScoreSubmitWindow(teachingClassId) {
  return request({
    url: `/teacher/scores/submit-window/${teachingClassId}`,
    method: 'get'
  })
}

export function saveTeacherScore(data) {
  return request({
    url: '/teacher/scores',
    method: 'post',
    data
  })
}

export function saveTeacherScores(scores) {
  return request({
    url: '/teacher/scores/batch',
    method: 'post',
    data: { scores }
  })
}

export function getTeacherScoreChangeRequests() {
  return request({
    url: '/teacher/score-change-requests',
    method: 'get'
  })
}

export function createTeacherScoreChangeRequest(data) {
  return request({
    url: '/teacher/score-change-requests',
    method: 'post',
    data
  })
}

export function getTeacherClassroomOptions() {
  return request({
    url: '/teacher/classroom-options',
    method: 'get'
  })
}

export function getTeacherAvailableClassroomOptions(params) {
  return request({
    url: '/teacher/available-classroom-options',
    method: 'get',
    params
  })
}

export function getTeacherAvailableExamClassroomOptions(params) {
  return request({
    url: '/teacher/exam-available-classroom-options',
    method: 'get',
    params
  })
}

export function getTeacherScheduleAdjustRequests() {
  return request({
    url: '/teacher/schedule-adjust-requests',
    method: 'get'
  })
}

export function createTeacherScheduleAdjustRequest(data) {
  return request({
    url: '/teacher/schedule-adjust-requests',
    method: 'post',
    data
  })
}

export function cancelTeacherScheduleAdjustRequest(adjustRequestId) {
  return request({
    url: `/teacher/schedule-adjust-requests/${adjustRequestId}/cancel`,
    method: 'put'
  })
}

export function getTeacherCourseDropRequests() {
  return request({
    url: '/teacher/course-drop-requests',
    method: 'get'
  })
}

export function reviewTeacherCourseDropRequest(dropRequestId, data) {
  return request({
    url: `/teacher/course-drop-requests/${dropRequestId}/review`,
    method: 'put',
    data
  })
}

export function getTeacherMakeupRequests() {
  return request({
    url: '/teacher/makeup-requests',
    method: 'get'
  })
}

export function reviewTeacherMakeupRequest(requestId, data) {
  return request({
    url: `/teacher/makeup-requests/${requestId}/review`,
    method: 'put',
    data
  })
}

export function getTeacherExams() {
  return request({
    url: '/teacher/exams',
    method: 'get'
  })
}

export function getTeacherExamChangeRequests() {
  return request({
    url: '/teacher/exam-change-requests',
    method: 'get'
  })
}

export function createTeacherExamChangeRequest(data) {
  return request({
    url: '/teacher/exam-change-requests',
    method: 'post',
    data
  })
}

export function cancelTeacherExamChangeRequest(examChangeRequestId) {
  return request({
    url: `/teacher/exam-change-requests/${examChangeRequestId}/cancel`,
    method: 'put'
  })
}
