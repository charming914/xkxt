import request from './request'

export function getNotifications(params) {
  return request({
    url: '/notifications',
    method: 'get',
    params
  })
}

export function getUnreadCount() {
  return request({
    url: '/notifications/unread-count',
    method: 'get'
  })
}

export function markNotificationRead(notificationId) {
  return request({
    url: `/notifications/${notificationId}/read`,
    method: 'put'
  })
}

export function markAllNotificationsRead() {
  return request({
    url: '/notifications/read-all',
    method: 'put'
  })
}
