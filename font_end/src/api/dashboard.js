import { apiClient } from './client'

export async function fetchDashboardSummary() {
  const { data } = await apiClient.get('/dashboard/summary')
  return data
}

export async function fetchDashboardChart(days = 7, currency = 'VND') {
  const { data } = await apiClient.get('/dashboard/chart', { params: { days, currency } })
  return data
}

export async function fetchDashboardRecentTransactions(limit = 5) {
  const { data } = await apiClient.get('/dashboard/recent-transactions', { params: { limit } })
  return data
}
