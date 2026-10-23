import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { depositTransaction } from '../api/transactions'
import {
  AccountSelect,
  AmountInput,
  PageHeader,
  RadioGroup,
} from '../components/ui/FormBits'
import { getApiErrorMessage } from '../utils/adminDisplay'
import { PAYMENT_METHOD_TO_API, parseAmountInput } from '../utils/userDisplay'

const METHODS = [
  { value: 'bank', label: 'Chuyển khoản ngân hàng' },
  { value: 'atm', label: 'Thẻ ATM' },
  { value: 'ewallet', label: 'Ví điện tử' },
]

export function DepositPage() {
  const navigate = useNavigate()
  const [accountId, setAccountId] = useState('')
  const [method, setMethod] = useState('bank')
  const [amount, setAmount] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    const parsed = parseAmountInput(amount)
    if (!parsed) {
      toast.error('Vui lòng nhập số tiền hợp lệ')
      return
    }
    if (!accountId) {
      toast.error('Vui lòng chọn tài khoản')
      return
    }
    try {
      setSubmitting(true)
      await depositTransaction({
        accountId: Number(accountId),
        amount: parsed,
        method: PAYMENT_METHOD_TO_API[method],
      })
      toast.success('Nạp tiền thành công')
      navigate('/accounts')
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Nạp tiền thất bại'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page-card">
      <PageHeader title="Nạp tiền" backTo={() => navigate(-1)} />
      <form className="form-stack" onSubmit={submit}>
        <div className="form-field">
          <label>Tài khoản nhận</label>
          <AccountSelect accountId={accountId} onChange={setAccountId} />
        </div>
        <div className="form-field">
          <label>Phương thức nạp</label>
          <RadioGroup name="deposit-method" options={METHODS} value={method} onChange={setMethod} />
        </div>
        <div className="form-field">
          <label>Số tiền</label>
          <AmountInput value={amount} onChange={setAmount} />
        </div>
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Đang xử lý...' : 'Nạp tiền'}
        </button>
      </form>
    </div>
  )
}
