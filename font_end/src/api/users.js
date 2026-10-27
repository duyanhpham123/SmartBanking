import { apiClient } from './client'

export async function fetchMyProfile() {
  const { data } = await apiClient.get('/users/me')
  return data
}

export async function updateMyProfile(payload) {
  const { data } = await apiClient.put('/users/me', payload)
  return data
}

export async function changeMyPassword(payload) {
  const { data } = await apiClient.put('/users/me/password', payload)
  return data
}
