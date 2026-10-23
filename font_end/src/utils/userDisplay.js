export const PAYMENT_METHOD_TO_API = {
  bank: 'BANK_TRANSFER',
  atm: 'ATM_CARD',
  ewallet: 'E_WALLET',
}

export const BILL_PROVIDER_OPTIONS = [
  { value: 'ELECTRICITY', label: 'Tiền điện' },
  { value: 'WATER', label: 'Tiền nước' },
  { value: 'INTERNET', label: 'Internet' },
  { value: 'MOBILE', label: 'Điện thoại di động' },
]

export const TX_TYPE_FILTER_OPTIONS = [
  { value: '', label: 'Tất cả loại giao dịch' },
  { value: 'DEPOSIT', label: 'Nạp tiền' },
  { value: 'WITHDRAW', label: 'Rút tiền' },
  { value: 'TRANSFER', label: 'Chuyển khoản' },
  { value: 'PAYMENT', label: 'Thanh toán hóa đơn' },
  { value: 'REFUND', label: 'Hoàn tiền' },
]

export function parseAmountInput(value) {
  const digits = String(value ?? '').replace(/\D/g, '')
  if (!digits) return null
  const n = Number(digits)
  return Number.isFinite(n) && n > 0 ? n : null
}

export function transactionSignedAmount(tx) {
  const amount = Number(tx.amount ?? 0)
  if (tx.direction === 'DEBIT') return -Math.abs(amount)
  if (tx.direction === 'CREDIT') return Math.abs(amount)
  return amount
}

export function notificationVariant(type) {
  switch (type) {
    case 'SECURITY':
      return 'security'
    case 'PROMOTION':
      return 'promo'
    case 'TRANSACTION':
      return 'deposit'
    default:
      return 'success'
  }
}
