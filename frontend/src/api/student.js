import request from './request'

export function getStudentProfile() {
  return request({
    url: '/student/profile',
    method: 'get'
  })
}

export function updateStudentProfile(data) {
  return request({
    url: '/student/profile',
    method: 'put',
    data
  })
}

export function getStudentTodos() {
  return request({
    url: '/student/todos',
    method: 'get'
  })
}

export function getStudentCurrentSemester() {
  return request({
    url: '/student/current-semester',
    method: 'get'
  })
}

export function getStudentSemesters() {
  return request({
    url: '/student/semesters',
    method: 'get'
  })
}

export function getAvailableCourses(params) {
  return request({
    url: '/student/courses/available',
    method: 'get',
    params
  })
}

export function selectCourse(data) {
  return request({
    url: '/student/course-selections',
    method: 'post',
    data
  })
}

export function getCoursePlans() {
  return request({
    url: '/student/course-plans',
    method: 'get'
  })
}

export function createCoursePlan(data) {
  return request({
    url: '/student/course-plans',
    method: 'post',
    data
  })
}

export function deleteCoursePlan(planId) {
  return request({
    url: `/student/course-plans/${planId}`,
    method: 'delete'
  })
}

export function getStudentTimetable(params) {
  return request({
    url: '/student/timetable',
    method: 'get',
    params
  })
}

export function getStudentScores(params) {
  return request({
    url: '/student/scores',
    method: 'get',
    params
  })
}

export function createScoreReviewRequest(data) {
  return request({
    url: '/student/score-review-requests',
    method: 'post',
    data
  })
}

export function getScoreReviewRequests() {
  return request({
    url: '/student/score-review-requests',
    method: 'get'
  })
}

export function getAcademicRemediationScores() {
  return request({
    url: '/student/academic-remediation/scores',
    method: 'get'
  })
}

export function getMakeupRequests() {
  return request({
    url: '/student/makeup-requests',
    method: 'get'
  })
}

export function createMakeupRequest(data) {
  return request({
    url: '/student/makeup-requests',
    method: 'post',
    data
  })
}

export function getRetakeRequests() {
  return request({
    url: '/student/retake-requests',
    method: 'get'
  })
}

export function getRetakeClasses() {
  return request({
    url: '/student/retake-classes',
    method: 'get'
  })
}

export function createRetakeRequest(data) {
  return request({
    url: '/student/retake-requests',
    method: 'post',
    data
  })
}

export function getAcademicProgress() {
  return request({
    url: '/student/academic-progress',
    method: 'get'
  })
}

export function createCourseSelectionRequest(data) {
  return request({
    url: '/student/course-selection-requests',
    method: 'post',
    data
  })
}

export function getCourseSelectionRequests() {
  return request({
    url: '/student/course-selection-requests',
    method: 'get'
  })
}

export function getSelectedCourses(params) {
  return request({
    url: '/student/selected-courses',
    method: 'get',
    params
  })
}

export function getDropCourses() {
  return request({
    url: '/student/drop-courses',
    method: 'get'
  })
}

export function dropCourse(teachingClassId) {
  return request({
    url: `/student/course-selections/${teachingClassId}`,
    method: 'delete'
  })
}

export function createCourseDropRequest(data) {
  return request({
    url: '/student/drop-requests',
    method: 'post',
    data
  })
}

export function getCourseDropRequests() {
  return request({
    url: '/student/drop-requests',
    method: 'get'
  })
}

export function getStudentExams(params) {
  return request({
    url: '/student/exams',
    method: 'get',
    params
  })
}
