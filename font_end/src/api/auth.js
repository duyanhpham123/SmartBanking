import { apiClient, getStoredRefreshToken, setStoredToken } from './client'

export async function registerApi({ fullName, email, phone, password }) {
  const { data } = await apiClient.post('/auth/register', {
    fullName,
    email,
    phone,
    password,
  })
  return data
}

export async function loginApi({ identifier, password, remember = false }) {
  const { data } = await apiClient.post('/auth/login', {
    identifier,
    password,
    rememberMe: remember,
  })
  setStoredToken(data.accessToken, remember, data.refreshToken)
  return data
}

// Thu hồi refresh token trên server để token bị lộ cũng không dùng lại được.
// Lỗi mạng cũng bỏ qua: phía trình duyệt vẫn xóa token như bình thường.
export async function logoutApi() {
  const refreshToken = getStoredRefreshToken()
  if (!refreshToken) return
  try {
    await apiClient.post('/auth/logout', { refreshToken })
  } catch {
    // bỏ qua
  }
}

export async function logoutAllApi() {
  const { data } = await apiClient.post('/auth/logout-all')
  return data
}
