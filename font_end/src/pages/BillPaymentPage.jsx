import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { payBill } from '../api/payments'
import { sendOtp } from '../api/otp'
import { formatMoney } from '../data/mockData'
import {
  AccountSelect,
  AmountInput,
  PageHeader,
  RadioGroup,
} from '../components/ui/FormBits'
import { getApiErrorMessage } from '../utils/adminDisplay'
import { BILL_PROVIDER_OPTIONS, parseAmountInput } from '../utils/userDisplay'

const STEPS = ['Thông tin', 'Xác nhận', 'Hoàn tất']

export function BillPaymentPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(0)
  const [accountId, setAccountId] = useState('')
  const [provider, setProvider] = useState('ELECTRICITY')
  const [customerCode, setCustomerCode] = useState('')
  const [amount, setAmount] = useState('')
  const [otp, setOtp] = useState('')
  const [devOtpHint, setDevOtpHint] = useState('')
  const [otpExpiresIn, setOtpExpiresIn] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [txCode, setTxCode] = useState('')

  const parsedAmount = parseAmountInput(amount) ?? 0
  const providerLabel = BILL_PROVIDER_OPTIONS.find((p) => p.value === provider)?.label ?? provider

  const requestPaymentOtp = async () => {
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
    const code = customerCode.trim()
    if (!accountId) {
      toast.error('Vui lòng chọn tài khoản thanh toán')
      return
    }
    if (!/^[A-Za-z0-9]{6,20}$/.test(code)) {
      toast.error('Mã khách hàng gồm 6–20 chữ và số')
      return
    }
    if (!parseAmountInput(amount)) {
      toast.error('Vui lòng nhập số tiền hợp lệ')
      return
    }
    try {
      setSubmitting(true)
      await requestPaymentOtp()
      setStep(1)
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không gửi được OTP'))
    } finally {
      setSubmitting(false)
    }
  }

  const confirmPayment = async () => {
    if (!/^\d{6}$/.test(otp)) {
      toast.error('Mã OTP gồm 6 chữ số')
      return
    }
    try {
      setSubmitting(true)
      const result = await payBill({
        accountId: Number(accountId),
        provider,
        customerCode: customerCode.trim(),
        amount: parsedAmount,
        otp,
      })
      setTxCode(result.transactionCode ?? '')
      setStep(2)
      toast.success('Thanh toán hóa đơn thành công')
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Thanh toán thất bại'))
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
      confirmPayment()
      return
    }
    navigate('/history')
  }

  return (
    <div className="page-card">
      <PageHeader
        title="Thanh toán hóa đơn"
        backTo={() => (step > 0 && step < 2 ? setStep(step - 1) : navigate(-1))}
      />

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
            <label>Loại hóa đơn</label>
            <RadioGroup
              name="bill-provider"
              options={BILL_PROVIDER_OPTIONS}
              value={provider}
              onChange={setProvider}
            />
          </div>
          <div className="form-field">
            <label htmlFor="customer-code">Mã khách hàng</label>
            <input
              id="customer-code"
              className="form-input"
              placeholder="6–20 ký tự chữ hoặc số"
              value={customerCode}
              onChange={(e) => setCustomerCode(e.target.value.replace(/[^A-Za-z0-9]/g, '').slice(0, 20))}
            />
          </div>
          <div className="form-field">
            <label>Tài khoản thanh toán</label>
            <AccountSelect accountId={accountId} onChange={setAccountId} />
          </div>
          <div className="form-field">
            <label>Số tiền</label>
            <AmountInput value={amount} onChange={setAmount} />
          </div>
        </div>
      )}

      {step === 1 && (
        <div className="form-stack">
          <div className="confirm-box">
            <div className="confirm-row">
              <span>Loại hóa đơn</span>
              <strong>{providerLabel}</strong>
            </div>
            <div className="confirm-row">
              <span>Mã khách hàng</span>
              <strong>{customerCode.trim().toUpperCase()}</strong>
            </div>
            <div className="confirm-row">
              <span>Số tiền</span>
              <strong>{formatMoney(parsedAmount)}</strong>
            </div>
          </div>
          <div className="form-field">
            <label htmlFor="bill-otp">Mã OTP</label>
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
                Dùng cùng OTP như chuyển tiền (mục đích TRANSFER trên server).
              </p>
            )}
            <input
              id="bill-otp"
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
                  await requestPaymentOtp()
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
          <h3>Thanh toán thành công</h3>
          <p style={{ color: 'var(--sb-text-muted)' }}>
            Đã thanh toán {formatMoney(parsedAmount)} cho hóa đơn {providerLabel}.
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
              ? 'Xác nhận thanh toán'
              : 'Xem lịch sử giao dịch'}
      </button>
    </div>
  )
}
