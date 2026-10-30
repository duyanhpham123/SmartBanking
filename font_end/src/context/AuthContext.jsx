import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import { loginApi, logoutApi } from '../api/auth'
import { SESSION_EXPIRED_EVENT, clearStoredToken, getStoredToken } from '../api/client'

const USER_KEY = 'smartbanking_user'

// Không thao tác (chuột, phím, chạm, cuộn) quá lâu thì tự đăng xuất, như ứng dụng ngân hàng thật
const IDLE_TIMEOUT_MS = 10 * 60 * 1000
const ACTIVITY_EVENTS = ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll']

const AuthContext = createContext(null)

function readStoredUser() {
  const raw = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function persistUser(user, remember) {
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(USER_KEY)
  if (!user) return
  const payload = JSON.stringify(user)
  if (remember) {
    localStorage.setItem(USER_KEY, payload)
  } else {
    sessionStorage.setItem(USER_KEY, payload)
  }
}

function mapUserFromApi(apiUser) {
  const roles = apiUser.roles ?? []
  const isAdmin = roles.some((r) => r === 'ROLE_ADMIN')
  return {
    id: apiUser.id,
    name: apiUser.fullName || apiUser.username || 'Người dùng',
    role: isAdmin ? 'ADMIN' : 'USER',
    email: apiUser.email,
    phone: apiUser.phone,
    roles,
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (getStoredToken() ? readStoredUser() : null))

  const isAdmin = Boolean(user?.role === 'ADMIN')

  // Xóa phiên phía trình duyệt (không gọi server)
  const clearSession = useCallback(() => {
    setUser(null)
    clearStoredToken()
    localStorage.removeItem(USER_KEY)
    sessionStorage.removeItem(USER_KEY)
  }, [])

  const logout = useCallback(() => {
    logoutApi() // đọc refresh token ngay lập tức, nên xóa phiên ngay sau đó vẫn an toàn
    clearSession()
  }, [clearSession])

  // Refresh token hết hạn / bị thu hồi (vd: đổi mật khẩu ở thiết bị khác) -> về trang đăng nhập
  useEffect(() => {
    function onExpired() {
      clearSession()
      toast.error('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại')
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired)
  }, [clearSession])

  // Tự đăng xuất khi không thao tác
  useEffect(() => {
    if (!user) return undefined
    let timer
    const reset = () => {
      clearTimeout(timer)
      timer = setTimeout(() => {
        logout()
        toast('Bạn đã được đăng xuất do không thao tác trong 10 phút', { icon: '🔒' })
      }, IDLE_TIMEOUT_MS)
    }
    reset()
    ACTIVITY_EVENTS.forEach((e) => window.addEventListener(e, reset, { passive: true }))
    return () => {
      clearTimeout(timer)
      ACTIVITY_EVENTS.forEach((e) => window.removeEventListener(e, reset))
    }
  }, [user, logout])

  const login = useCallback(async (credentials, asAdmin = false) => {
    const data = await loginApi({
      identifier: credentials.identifier,
      password: credentials.password,
      remember: credentials.remember,
    })
    const mapped = mapUserFromApi(data.user)
    if (asAdmin && mapped.role !== 'ADMIN') {
      logoutApi()
      clearStoredToken()
      throw new Error('Tài khoản không có quyền quản trị')
    }
    setUser(mapped)
    persistUser(mapped, credentials.remember)
    return mapped
  }, [])

  // Đồng bộ lại thông tin hiển thị (tên, email...) sau khi người dùng sửa hồ sơ
  const updateUser = useCallback((apiUser) => {
    const mapped = mapUserFromApi(apiUser)
    setUser(mapped)
    persistUser(mapped, Boolean(localStorage.getItem(USER_KEY)))
  }, [])

  const value = useMemo(
    () => ({
      user,
      isAdmin,
      isAuthenticated: Boolean(user && getStoredToken()),
      login,
      logout,
      updateUser,
    }),
    [user, isAdmin, login, logout, updateUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
