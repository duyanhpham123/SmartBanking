import axios from 'axios'

const TOKEN_KEY = 'smartbanking_access_token'
const REFRESH_KEY = 'smartbanking_refresh_token'

// AuthContext nghe sự kiện này để đưa người dùng về trang đăng nhập
export const SESSION_EXPIRED_EVENT = 'smartbanking:session-expired'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081/api/v1'

// Token nằm ở localStorage nếu chọn "Ghi nhớ đăng nhập", ngược lại ở sessionStorage
function tokenStorage() {
  return localStorage.getItem(TOKEN_KEY) ? localStorage : sessionStorage
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
}

export function getStoredRefreshToken() {
  return localStorage.getItem(REFRESH_KEY) || sessionStorage.getItem(REFRESH_KEY)
}

export function setStoredToken(token, remember = false, refreshToken = null) {
  clearStoredToken()
  if (!token) return
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(TOKEN_KEY, token)
  if (refreshToken) storage.setItem(REFRESH_KEY, refreshToken)
}

export function clearStoredToken() {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Nhiều request cùng hết hạn một lúc chỉ gọi /auth/refresh một lần
let refreshPromise = null

async function refreshAccessToken() {
  const refreshToken = getStoredRefreshToken()
  if (!refreshToken) throw new Error('Không có refresh token')

  const storage = tokenStorage()
  // Dùng axios gốc để không đi qua interceptor này (tránh lặp vô hạn)
  const { data } = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
  storage.setItem(TOKEN_KEY, data.accessToken)
  storage.setItem(REFRESH_KEY, data.refreshToken)
  return data.accessToken
}

// Access token sống 15 phút: gặp 401 thì đổi token mới rồi gửi lại request một lần.
// Đổi không được (refresh token hết hạn / bị thu hồi) thì đăng xuất.
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    const isAuthCall = original?.url?.startsWith('/auth/')

    if (status !== 401 || !original || original._retry || isAuthCall) {
      return Promise.reject(error)
    }

    original._retry = true
    try {
      refreshPromise = refreshPromise ?? refreshAccessToken().finally(() => (refreshPromise = null))
      const newToken = await refreshPromise
      original.headers.Authorization = `Bearer ${newToken}`
      return apiClient(original)
    } catch {
      clearStoredToken()
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
      return Promise.reject(error)
    }
  },
)
