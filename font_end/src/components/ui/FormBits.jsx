import { useEffect } from 'react'
import { FiChevronDown } from 'react-icons/fi'
import { useMyAccounts } from '../../hooks/useMyAccounts'
import { formatBalance } from '../../data/mockData'

export function AccountSelect({ accountId, onChange, currency = 'VND' }) {
  const { accounts, loading } = useMyAccounts({ currency })

  useEffect(() => {
    if (loading || accounts.length === 0) return
    const valid = accounts.some((a) => a.id === accountId)
    if (!valid) onChange?.(accounts[0].id)
  }, [accounts, accountId, loading, onChange])

  if (loading) {
    return (
      <select className="account-select-native" disabled aria-label="Chọn tài khoản">
        <option>Đang tải tài khoản...</option>
      </select>
    )
  }

  if (accounts.length === 0) {
    return (
      <select className="account-select-native" disabled aria-label="Chọn tài khoản">
        <option>Chưa có tài khoản VND</option>
      </select>
    )
  }

  const value = accounts.some((a) => a.id === accountId) ? accountId : accounts[0].id

  return (
    <select
      className="account-select-native"
      value={value}
      onChange={(e) => onChange?.(e.target.value)}
      aria-label="Chọn tài khoản"
    >
      {accounts.map((a) => (
        <option key={a.id} value={a.id}>
          {a.number} - {a.label} | Số dư: {formatBalance(a.balance, a.currency)}
        </option>
      ))}
    </select>
  )
}

export function AccountSelectDisplay({ accountId = '1' }) {
  const { accounts } = useMyAccounts({ currency: 'VND' })
  const account = accounts.find((a) => a.id === accountId) ?? accounts[0]
  if (!account) {
    return (
      <div className="account-select">
        <div className="account-select-main">—</div>
      </div>
    )
  }
  return (
    <div className="account-select">
      <div>
        <div className="account-select-main">
          {account.number} - {account.label}
        </div>
        <div className="account-select-balance">Số dư: {formatBalance(account.balance, account.currency)}</div>
      </div>
      <FiChevronDown color="#94a3b8" />
    </div>
  )
}

export function RadioGroup({ options, value, onChange, name }) {
  return (
    <div className="radio-group" role="radiogroup" aria-label={name}>
      {options.map((opt) => (
        <label
          key={opt.value}
          className={`radio-option${value === opt.value ? ' selected' : ''}`}
        >
          <input
            type="radio"
            name={name}
            value={opt.value}
            checked={value === opt.value}
            onChange={() => onChange(opt.value)}
            style={{ position: 'absolute', opacity: 0, pointerEvents: 'none' }}
          />
          <span className="radio-dot">
            <span className="radio-dot-inner" />
          </span>
          {opt.label}
        </label>
      ))}
    </div>
  )
}

export function PageHeader({ title, backTo }) {
  return (
    <div className="page-header">
      {backTo != null && (
        <button type="button" className="back-link" onClick={backTo} aria-label="Quay lại">
          ‹
        </button>
      )}
      <h1>{title}</h1>
    </div>
  )
}

export function AmountInput({ value, onChange, placeholder = 'Nhập số tiền' }) {
  return (
    <div className="input-with-suffix">
      <input
        type="text"
        inputMode="numeric"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      <span className="input-suffix">VND</span>
    </div>
  )
}

export function Field({ label, hint, children }) {
  return (
    <div className="form-field">
      <label>
        {label}
        {hint && <span className="field-hint">{hint}</span>}
      </label>
      {children}
    </div>
  )
}

// rows: [[nhãn, giá trị], ...] — hiển thị thông tin chỉ đọc dạng 2 cột
export function InfoList({ rows }) {
  return (
    <dl className="info-list">
      {rows.map(([label, value]) => (
        <div key={label} className="info-row">
          <dt>{label}</dt>
          <dd>{value || '—'}</dd>
        </div>
      ))}
    </dl>
  )
}
