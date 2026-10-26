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

export async function fetchMyKyc() {
  const { data } = await apiClient.get('/users/me/kyc')
  return data
}

export async function submitMyKyc(payload) {
  const { data } = await apiClient.put('/users/me/kyc', payload)
  return data
}
