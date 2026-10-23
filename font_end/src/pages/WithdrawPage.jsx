import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { withdrawTransaction } from '../api/transactions'
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
  { value: 'ewallet', label: 'Ví điện tử' },
]

export function WithdrawPage() {
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
      await withdrawTransaction({
        accountId: Number(accountId),
        amount: parsed,
        method: PAYMENT_METHOD_TO_API[method],
      })
      toast.success('Rút tiền thành công')
      navigate('/accounts')
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Rút tiền thất bại'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page-card">
      <PageHeader title="Rút tiền" backTo={() => navigate(-1)} />
      <form className="form-stack" onSubmit={submit}>
        <div className="form-field">
          <label>Tài khoản rút</label>
          <AccountSelect accountId={accountId} onChange={setAccountId} />
        </div>
        <div className="form-field">
          <label>Số tiền</label>
          <AmountInput value={amount} onChange={setAmount} />
        </div>
        <div className="form-field">
          <label>Phương thức rút</label>
          <RadioGroup name="withdraw-method" options={METHODS} value={method} onChange={setMethod} />
        </div>
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Đang xử lý...' : 'Rút tiền'}
        </button>
      </form>
    </div>
  )
}
