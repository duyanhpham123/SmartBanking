export const GENDER_OPTIONS = [
  { value: 'MALE', label: 'Nam' },
  { value: 'FEMALE', label: 'Nữ' },
  { value: 'OTHER', label: 'Khác' },
]

export const ID_TYPE_OPTIONS = [
  { value: 'CCCD', label: 'Căn cước công dân', hint: '12 chữ số' },
  { value: 'CMND', label: 'Chứng minh nhân dân', hint: '9 chữ số' },
  { value: 'PASSPORT', label: 'Hộ chiếu', hint: '1 chữ cái + 7 chữ số' },
]

export const INCOME_OPTIONS = [
  { value: 'UNDER_10M', label: 'Dưới 10 triệu' },
  { value: 'FROM_10M_TO_30M', label: '10 – 30 triệu' },
  { value: 'FROM_30M_TO_50M', label: '30 – 50 triệu' },
  { value: 'OVER_50M', label: 'Trên 50 triệu' },
]

export const KYC_STATUS_OPTIONS = [
  { value: 'NOT_SUBMITTED', label: 'Chưa khai báo' },
  { value: 'PENDING', label: 'Chờ xác minh' },
  { value: 'VERIFIED', label: 'Đã xác minh' },
  { value: 'REJECTED', label: 'Bị từ chối' },
]

function labelOf(options, value) {
  if (!value) return '—'
  return options.find((o) => o.value === value)?.label ?? value
}

export const genderLabel = (v) => labelOf(GENDER_OPTIONS, v)
export const idTypeLabel = (v) => labelOf(ID_TYPE_OPTIONS, v)
export const incomeLabel = (v) => labelOf(INCOME_OPTIONS, v)
export const kycStatusLabel = (v) => labelOf(KYC_STATUS_OPTIONS, v ?? 'NOT_SUBMITTED')

export function kycBadgeClass(status) {
  if (status === 'VERIFIED') return 'success'
  if (status === 'REJECTED') return 'failed'
  if (status === 'PENDING') return 'pending'
  return 'neutral'
}

export function formatDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

// "Nguyễn Văn An" -> "NA"
export function initials(name) {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  const last = parts[parts.length - 1]?.[0] ?? ''
  const first = parts.length > 1 ? parts[0][0] : ''
  return (first + last).toUpperCase()
}
