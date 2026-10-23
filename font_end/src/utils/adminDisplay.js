export const TX_TYPE_LABELS = {
  DEPOSIT: 'Nạp tiền',
  WITHDRAW: 'Rút tiền',
  TRANSFER: 'Chuyển khoản',
  PAYMENT: 'Thanh toán hóa đơn',
  REFUND: 'Hoàn tiền',
}

export const TX_TYPE_COLORS = {
  DEPOSIT: '#38bdf8',
  WITHDRAW: '#22c55e',
  TRANSFER: '#2563eb',
  PAYMENT: '#a855f7',
  REFUND: '#f97316',
}

const TX_STATUS_LABELS = {
  PENDING: 'Đang xử lý',
  SUCCESS: 'Thành công',
  FAILED: 'Thất bại',
  CANCELLED: 'Đã hủy',
}

const USER_STATUS_LABELS = {
  ACTIVE: 'Hoạt động',
  LOCKED: 'Khóa',
  PENDING: 'Chờ duyệt',
  DELETED: 'Đã xóa',
}

const ACCOUNT_STATUS_LABELS = {
  ACTIVE: 'Hoạt động',
  LOCKED: 'Khóa',
  CLOSED: 'Đã đóng',
  PENDING: 'Chờ duyệt',
}

export function formatDateTime(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  return d.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

export function formatChartDate(isoDate) {
  if (!isoDate) return ''
  const d = new Date(isoDate)
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}

export function transactionTypeLabel(type) {
  return TX_TYPE_LABELS[type] ?? type
}

export function transactionStatusLabel(status) {
  return TX_STATUS_LABELS[status] ?? status
}

export function userStatusLabel(status) {
  return USER_STATUS_LABELS[status] ?? status
}

export function accountStatusLabel(status) {
  return ACCOUNT_STATUS_LABELS[status] ?? status
}

export function statusBadgeClass(status) {
  if (status === 'SUCCESS' || status === 'ACTIVE') return 'success'
  if (status === 'FAILED' || status === 'LOCKED' || status === 'CANCELLED' || status === 'DELETED') {
    return 'failed'
  }
  return 'success'
}

export function txParticipantLabel(tx) {
  if (tx.fromUsername && tx.toUsername && tx.fromUsername !== tx.toUsername) {
    return `${tx.fromUsername} → ${tx.toUsername}`
  }
  return tx.fromUsername || tx.toUsername || '—'
}

export function getApiErrorMessage(err, fallback = 'Có lỗi xảy ra') {
  return err?.response?.data?.message || err?.message || fallback
}

export function formatTransactionDescription(description) {
  const trimmed = typeof description === 'string' ? description.trim() : ''
  return trimmed || '—'
}
