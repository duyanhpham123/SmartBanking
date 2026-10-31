import { useCallback, useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { fetchAdminLimits, updateAdminLimit } from '../../api/admin'
import { Field, InfoList, PageHeader } from '../../components/ui/FormBits'
import { formatBalance } from '../../data/mockData'
import { formatDateTime, getApiErrorMessage } from '../../utils/adminDisplay'

const CURRENCY_LABEL = { VND: 'Tài khoản VND', USD: 'Tài khoản USD' }

// "200.000.000" -> 200000000 (VND); "1,234.50" -> 1234.5 (USD)
function parseAmount(raw, currency) {
  const cleaned = currency === 'VND' ? raw.replace(/\D/g, '') : raw.replace(/,/g, '').trim()
  if (!cleaned || !/^\d+(\.\d{1,2})?$/.test(cleaned)) return null
  const n = Number(cleaned)
  return n > 0 ? n : null
}

export function AdminSettingsPage() {
  const [limits, setLimits] = useState([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setLimits(await fetchAdminLimits())
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không tải được cài đặt'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="page-card">
      <PageHeader title="Cài đặt hệ thống" />

      <h3 style={{ margin: '0 0 0.25rem' }}>Hạn mức giao dịch mỗi ngày</h3>
      <p className="profile-note" style={{ margin: '0 0 1rem' }}>
        Tổng tiền ra (rút + chuyển khoản + thanh toán) tối đa của một tài khoản trong một ngày. Thay đổi có hiệu
        lực ngay với mọi giao dịch tiếp theo.
      </p>

      {loading && <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải...</p>}

      {!loading && (
        <div className="detail-grid">
          {limits.map((limit) => (
            <LimitPanel key={limit.currency} limit={limit} onSaved={load} />
          ))}

          <section className="panel wide">
            <h3>Chính sách bảo mật đăng nhập</h3>
            <InfoList
              rows={[
                ['Sai mật khẩu', '5 lần trong 15 phút thì khóa đăng nhập 15 phút'],
                ['Sai mã OTP', '5 lần trong 15 phút thì khóa xác thực OTP 15 phút'],
                ['Gửi OTP', 'Tối đa 3 lần trong 5 phút'],
                ['Tự đăng xuất', 'Sau 10 phút không thao tác'],
                ['Lưu bộ đếm', 'Trong database, không mất khi khởi động lại server'],
              ]}
            />
          </section>
        </div>
      )}
    </div>
  )
}

function LimitPanel({ limit, onSaved }) {
  const { currency } = limit
  const [value, setValue] = useState('')
  const [busy, setBusy] = useState(false)

  async function save(amount) {
    if (amount == null) {
      toast.error(currency === 'VND' ? 'Hạn mức phải là số nguyên dương' : 'Hạn mức không hợp lệ (tối đa 2 số lẻ)')
      return
    }
    if (amount > Number(limit.maxDailyOutLimit)) {
      toast.error(`Hạn mức tối đa là ${formatBalance(Number(limit.maxDailyOutLimit), currency)}`)
      return
    }
    if (!window.confirm(`Đổi hạn mức ngày của ${CURRENCY_LABEL[currency]} thành ${formatBalance(amount, currency)}?`)) {
      return
    }
    try {
      setBusy(true)
      await updateAdminLimit(currency, amount)
      toast.success('Đã cập nhật hạn mức')
      setValue('')
      onSaved()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const isDefault = Number(limit.dailyOutLimit) === Number(limit.defaultDailyOutLimit)

  return (
    <section className="panel">
      <div className="panel-head">
        <h3>{CURRENCY_LABEL[currency] ?? currency}</h3>
        <span className={`status-badge ${isDefault ? 'pending' : 'success'}`}>
          {isDefault ? 'Mặc định' : 'Đã tùy chỉnh'}
        </span>
      </div>
      <InfoList
        rows={[
          ['Hạn mức hiện tại', formatBalance(Number(limit.dailyOutLimit), currency)],
          ['Mặc định', formatBalance(Number(limit.defaultDailyOutLimit), currency)],
          ['Cập nhật lần cuối', limit.updatedAt ? formatDateTime(limit.updatedAt) : null],
          ['Người cập nhật', limit.updatedByName],
        ]}
      />
      <form
        className="form-stack"
        style={{ marginTop: '1rem' }}
        onSubmit={(e) => {
          e.preventDefault()
          save(parseAmount(value, currency))
        }}
      >
        <Field label="Hạn mức mới" hint={`Tối đa ${formatBalance(Number(limit.maxDailyOutLimit), currency)}`}>
          <div className="input-with-suffix">
            <input
              type="text"
              inputMode={currency === 'VND' ? 'numeric' : 'decimal'}
              placeholder={currency === 'VND' ? 'Ví dụ: 300000000' : 'Ví dụ: 25000'}
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
            <span className="input-suffix">{currency}</span>
          </div>
        </Field>
        <div className="kyc-review-actions" style={{ marginTop: 0 }}>
          <button type="submit" className="btn-primary" disabled={busy || !value.trim()}>
            Lưu hạn mức
          </button>
          {!isDefault && (
            <button
              type="button"
              className="btn-secondary"
              disabled={busy}
              onClick={() => save(Number(limit.defaultDailyOutLimit))}
            >
              Khôi phục mặc định
            </button>
          )}
        </div>
      </form>
    </section>
  )
}
