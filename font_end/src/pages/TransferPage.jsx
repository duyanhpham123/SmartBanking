import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { sendOtp } from '../api/otp'
import { transferTransaction } from '../api/transactions'
import { formatMoney } from '../data/mockData'
import {
  AccountSelect,
  AmountInput,
  PageHeader,
} from '../components/ui/FormBits'
import { useMyAccounts } from '../hooks/useMyAccounts'
import { getApiErrorMessage } from '../utils/adminDisplay'
import { parseAmountInput } from '../utils/userDisplay'

const STEPS = ['Thông tin', 'Xác nhận', 'Hoàn tất']

export function TransferPage() {
  const navigate = useNavigate()
  const { accounts } = useMyAccounts({ currency: 'VND' })
  const [step, setStep] = useState(0)
  const [fromId, setFromId] = useState('')
  const [toAccount, setToAccount] = useState('')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [otp, setOtp] = useState('')
  const [devOtpHint, setDevOtpHint] = useState('')
  const [otpExpiresIn, setOtpExpiresIn] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [txCode, setTxCode] = useState('')

  const from = accounts.find((a) => a.id === fromId) ?? accounts[0]
  const parsedAmount = parseAmountInput(amount) ?? 0

  const requestTransferOtp = async () => {
    const sent = await sendOtp('TRANSFER')
    setOtpExpiresIn(sent.expiresInSeconds ?? null)
    if (sent.devOtp) {
      setDevOtpHint(sent.devOtp)
      toast.success(`Mã OTP (môi trường dev): ${sent.devOtp}`)
    } else {
      setDevOtpHint('')
      toast.success('Đã tạo mã OTP. Kiểm tra SMS/email hoặc log server.')
    }
  }

  const goToConfirm = async () => {
    if (!toAccount.trim() || !parseAmountInput(amount)) {
      toast.error('Vui lòng nhập đầy đủ thông tin')
      return
    }
    if (!fromId && !from?.id) {
      toast.error('Vui lòng chọn tài khoản nguồn')
      return
    }
    try {
      setSubmitting(true)
      await requestTransferOtp()
      setStep(1)
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không gửi được OTP'))
    } finally {
      setSubmitting(false)
    }
  }

  const confirmTransfer = async () => {
    if (!/^\d{6}$/.test(otp)) {
      toast.error('Mã OTP gồm 6 chữ số')
      return
    }
    const sourceId = Number(fromId || from?.id)
    try {
      setSubmitting(true)
      const result = await transferTransaction({
        fromAccountId: sourceId,
        toAccountNumber: toAccount.trim(),
        amount: parsedAmount,
        description: note.trim() || undefined,
        otp,
      })
      setTxCode(result.transactionCode ?? '')
      setStep(2)
      toast.success('Chuyển tiền thành công')
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Chuyển tiền thất bại'))
    } finally {
      setSubmitting(false)
    }
  }

  const handleContinue = () => {
    if (step === 0) {
      goToConfirm()
      return
    }
    if (step === 1) {
      confirmTransfer()
      return
    }
    navigate('/')
  }

  return (
    <div className="page-card">
      <PageHeader title="Chuyển tiền" backTo={() => (step > 0 && step < 2 ? setStep(step - 1) : navigate(-1))} />

      <div className="stepper">
        {STEPS.map((label, i) => (
          <div
            key={label}
            className={`stepper-step${i === step ? ' active' : ''}${i < step ? ' done' : ''}`}
          >
            <div className="stepper-circle">{i < step ? '✓' : i + 1}</div>
            <span className="stepper-label">{label}</span>
          </div>
        ))}
      </div>

      {step === 0 && (
        <div className="form-stack">
          <div className="form-field">
            <label htmlFor="from-account">Từ tài khoản</label>
            <AccountSelect accountId={fromId} onChange={setFromId} />
          </div>
          <div className="form-field">
            <label htmlFor="to-account">Đến tài khoản</label>
            <input
              id="to-account"
              className="form-input"
              placeholder="Nhập số tài khoản hoặc chọn từ danh bạ"
              value={toAccount}
              onChange={(e) => setToAccount(e.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="amount">Số tiền</label>
            <AmountInput value={amount} onChange={setAmount} />
          </div>
          <div className="form-field">
            <label htmlFor="note">Nội dung</label>
            <textarea
              id="note"
              className="form-textarea"
              placeholder="Nhập nội dung chuyển khoản"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>
        </div>
      )}

      {step === 1 && (
        <div className="form-stack">
          <div className="confirm-box">
            <div className="confirm-row">
              <span>Từ tài khoản</span>
              <strong>{from?.number ?? '—'}</strong>
            </div>
            <div className="confirm-row">
              <span>Đến tài khoản</span>
              <strong>{toAccount}</strong>
            </div>
            <div className="confirm-row">
              <span>Số tiền</span>
              <strong>{formatMoney(parsedAmount)}</strong>
            </div>
            <div className="confirm-row">
              <span>Nội dung</span>
              <strong>{note || '—'}</strong>
            </div>
          </div>
          <div className="form-field">
            <label htmlFor="otp">Mã OTP</label>
            {devOtpHint ? (
              <p
                style={{
                  margin: '0 0 0.5rem',
                  padding: '0.75rem 1rem',
                  borderRadius: 8,
                  background: '#eff6ff',
                  color: '#1e40af',
                  fontSize: '0.9rem',
                }}
              >
                <strong>Môi trường dev:</strong> mã OTP là{' '}
                <code style={{ fontSize: '1.1rem', letterSpacing: 2 }}>{devOtpHint}</code>
                {otpExpiresIn != null ? ` (hết hạn sau ${Math.round(otpExpiresIn / 60)} phút).` : '.'}
              </p>
            ) : (
              <p style={{ margin: '0 0 0.5rem', color: 'var(--sb-text-muted)', fontSize: '0.875rem' }}>
                Hệ thống chưa gửi SMS/email. Xem log console backend hoặc bảng{' '}
                <code>otp_codes</code> trên MySQL.
              </p>
            )}
            <input
              id="otp"
              className="form-input"
              inputMode="numeric"
              maxLength={6}
              placeholder="6 chữ số"
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
            />
            <button
              type="button"
              className="page-btn"
              style={{ marginTop: '0.5rem' }}
              disabled={submitting}
              onClick={async () => {
                try {
                  setSubmitting(true)
                  await requestTransferOtp()
                } catch (err) {
                  toast.error(getApiErrorMessage(err, 'Không gửi lại được OTP'))
                } finally {
                  setSubmitting(false)
                }
              }}
            >
              Gửi lại OTP
            </button>
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="success-panel">
          <div className="success-icon">✓</div>
          <h3>Giao dịch hoàn tất</h3>
          <p style={{ color: 'var(--sb-text-muted)' }}>
            Bạn đã chuyển {formatMoney(parsedAmount)} thành công.
            {txCode ? ` Mã GD: ${txCode}.` : ''}
          </p>
        </div>
      )}

      <button
        type="button"
        className="btn-primary"
        style={{ marginTop: '1.5rem' }}
        onClick={handleContinue}
        disabled={submitting}
      >
        {submitting
          ? 'Đang xử lý...'
          : step === 0
            ? 'Tiếp tục'
            : step === 1
              ? 'Xác nhận chuyển tiền'
              : 'Về trang chủ'}
      </button>
    </div>
  )
}
