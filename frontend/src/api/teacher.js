import request from './request'

export function getTeachingClasses() {
  return request({
    url: '/teacher/teaching-classes',
    method: 'get'
  })
}

export function getTeacherScores() {
  return request({
    url: '/teacher/scores',
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
