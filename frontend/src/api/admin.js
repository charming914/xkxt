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

export function getAdminCourseSelectionStatistics() {
  return request({
    url: '/admin/statistics/course-selection',
    method: 'get'
  })
}

export function getCourseSelectionSetting() {
  return request({
    url: '/admin/course-selection-setting',
    method: 'get'
  })
}

export function updateCourseSelectionSetting(data) {
  return request({
    url: '/admin/course-selection-setting',
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
