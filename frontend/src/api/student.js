import request from './request'

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

export function getStudentTimetable() {
  return request({
    url: '/student/timetable',
    method: 'get'
  })
}

export function getStudentScores() {
  return request({
    url: '/student/scores',
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

export function getSelectedCourses() {
  return request({
    url: '/student/selected-courses',
    method: 'get'
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
