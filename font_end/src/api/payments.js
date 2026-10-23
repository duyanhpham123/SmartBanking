import { apiClient } from './client'

export async function payBill(payload, idempotencyKey = crypto.randomUUID()) {
  const { data } = await apiClient.post('/payments', payload, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
  return data
}
