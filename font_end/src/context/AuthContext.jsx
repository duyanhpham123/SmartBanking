import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { loginApi } from '../api/auth'
import { clearStoredToken, getStoredToken } from '../api/client'

const USER_KEY = 'smartbanking_user'

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

  const logout = useCallback(() => {
    setUser(null)
    clearStoredToken()
    localStorage.removeItem(USER_KEY)
    sessionStorage.removeItem(USER_KEY)
  }, [])

  const login = useCallback(async (credentials, asAdmin = false) => {
    const data = await loginApi({
      identifier: credentials.identifier,
      password: credentials.password,
      remember: credentials.remember,
    })
    const mapped = mapUserFromApi(data.user)
    if (asAdmin && mapped.role !== 'ADMIN') {
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
