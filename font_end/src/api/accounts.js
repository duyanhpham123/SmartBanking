import { apiClient } from './client'

export async function fetchMyAccounts() {
  const { data } = await apiClient.get('/accounts')
  return data
}

export async function lookupAccount(accountNumber) {
  const { data } = await apiClient.get('/accounts/lookup', {
    params: { accountNumber },
  })
  return data
}

export async function fetchMyAccount(id) {
  const { data } = await apiClient.get(`/accounts/${id}`)
  return data
}

export async function fetchAccountTransactions(
  id,
  { type, status, from, to, keyword, page = 0, size = 10 } = {},
) {
  const { data } = await apiClient.get(`/accounts/${id}/transactions`, {
    params: { type, status, from, to, keyword, page, size },
  })
  return data
}

export async function createAccount(payload) {
  const { data } = await apiClient.post('/accounts', payload)
  return data
}

export async function renameAccount(id, accountName) {
  const { data } = await apiClient.put(`/accounts/${id}/name`, { accountName })
  return data
}
