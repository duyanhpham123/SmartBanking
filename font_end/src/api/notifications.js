import { apiClient } from './client'

export async function fetchMyNotifications({ read, type, page = 0, size = 10 } = {}) {
  const { data } = await apiClient.get('/notifications', {
    params: { read, type, page, size },
  })
  return data
}

export async function fetchUnreadNotificationCount() {
  const { data } = await apiClient.get('/notifications/unread-count')
  return data
}

export async function markNotificationAsRead(id) {
  const { data } = await apiClient.put(`/notifications/${id}/read`)
  return data
}

export async function markAllNotificationsAsRead() {
  const { data } = await apiClient.put('/notifications/read-all')
  return data
}
