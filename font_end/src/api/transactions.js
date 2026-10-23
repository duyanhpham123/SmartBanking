import { apiClient } from './client'

export async function depositTransaction(payload) {
  const { data } = await apiClient.post('/transactions/deposit', payload)
  return data
}

export async function withdrawTransaction(payload) {
  const { data } = await apiClient.post('/transactions/withdraw', payload)
  return data
}

export async function transferTransaction(payload, idempotencyKey = crypto.randomUUID()) {
  const { data } = await apiClient.post('/transactions/transfer', payload, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
  return data
}

export async function fetchTransactionHistory({
  type,
  status,
  from,
  to,
  keyword,
  page = 0,
  size = 10,
} = {}) {
  const { data } = await apiClient.get('/transactions/history', {
    params: { type, status, from, to, keyword, page, size },
  })
  return data
}

export async function exportTransactionHistory({ type, status, from, to, keyword } = {}) {
  const { data } = await apiClient.get('/transactions/history/export', {
    params: { type, status, from, to, keyword },
    responseType: 'blob',
  })
  return data
}

export async function fetchTransactionById(id) {
  const { data } = await apiClient.get(`/transactions/${id}`)
  return data
}
