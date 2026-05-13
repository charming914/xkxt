import request from './request'

export function getAvailableCourses() {
  return request({
    url: '/student/courses/available',
    method: 'get'
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
