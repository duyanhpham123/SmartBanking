import { apiClient } from './client'

export async function sendOtp(purpose) {
  const { data } = await apiClient.post('/otp/send', { purpose })
  return data
}

export async function verifyOtp(purpose, code) {
  const { data } = await apiClient.post('/otp/verify', { purpose, code })
  return data
}
