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

export function getSemesterOptions() {
  return request({
    url: '/admin/semesters',
    method: 'get'
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
