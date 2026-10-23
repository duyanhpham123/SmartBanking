import { apiClient, setStoredToken } from './client'

export async function loginApi({ identifier, password, remember = false }) {
  const { data } = await apiClient.post('/auth/login', {
    identifier,
    password,
    rememberMe: remember,
  })
  setStoredToken(data.accessToken, remember)
  return data
}
