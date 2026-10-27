import { apiClient } from './client'

export async function fetchAdminOverview() {
  const { data } = await apiClient.get('/admin/dashboard/overview')
  return data
}

export async function fetchAdminTransactionsByType() {
  const { data } = await apiClient.get('/admin/dashboard/transactions-by-type')
  return data
}

export async function fetchAdminRecentTransactions(limit = 10) {
  const { data } = await apiClient.get('/admin/dashboard/recent-transactions', { params: { limit } })
  return data
}

export async function fetchAdminChart(days = 7, currency = 'VND') {
  const { data } = await apiClient.get('/admin/dashboard/chart', { params: { days, currency } })
  return data
}

export async function fetchAdminUsers({ status, keyword, page = 0, size = 20 } = {}) {
  const { data } = await apiClient.get('/admin/users', {
    params: { status, keyword, page, size },
  })
  return data
}

export async function lockAdminUser(id) {
  const { data } = await apiClient.put(`/admin/users/${id}/lock`)
  return data
}

export async function unlockAdminUser(id) {
  const { data } = await apiClient.put(`/admin/users/${id}/unlock`)
  return data
}

export async function fetchAdminAccounts({ status, type, currency, keyword, page = 0, size = 20 } = {}) {
  const { data } = await apiClient.get('/admin/accounts', {
    params: { status, type, currency, keyword, page, size },
  })
  return data
}

export async function lockAdminAccount(id) {
  const { data } = await apiClient.put(`/admin/accounts/${id}/lock`)
  return data
}

export async function unlockAdminAccount(id) {
  const { data } = await apiClient.put(`/admin/accounts/${id}/unlock`)
  return data
}

export async function fetchAdminTransactions({ type, status, keyword, page = 0, size = 20 } = {}) {
  const { data } = await apiClient.get('/admin/transactions', {
    params: { type, status, keyword, page, size },
  })
  return data
}

export async function fetchAdminAuditLogs({ keyword, page = 0, size = 20 } = {}) {
  const { data } = await apiClient.get('/admin/audit-logs', {
    params: { keyword, page, size },
  })
  return data
}
