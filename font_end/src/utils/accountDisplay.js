const TYPE_META = {
  PAYMENT: { label: 'Tài khoản thanh toán', color: '#2563eb' },
  SAVINGS: { label: 'Tài khoản tiết kiệm', color: '#0d9488' },
}

const STATUS_LABELS = {
  ACTIVE: 'Hoạt động',
  LOCKED: 'Khóa',
  CLOSED: 'Đã đóng',
  PENDING: 'Chờ duyệt',
}

export function formatAccountNumber(accountNumber) {
  const digits = String(accountNumber ?? '').replace(/\s/g, '')
  return digits.replace(/(\d{4})(?=\d)/g, '$1 ').trim()
}

export function mapAccountFromApi(account) {
  const type = account.accountType ?? 'PAYMENT'
  const meta = TYPE_META[type] ?? { label: type, color: '#64748b' }
  const balance = Number(account.balance ?? 0)

  return {
    id: String(account.id),
    number: formatAccountNumber(account.accountNumber),
    label: account.accountName?.trim() || meta.label,
    balance,
    currency: account.currency ?? 'VND',
    type: type.toLowerCase(),
    color: meta.color,
    status: STATUS_LABELS[account.status] ?? account.status,
  }
}
