import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FiEye, FiEyeOff, FiLock, FiMail, FiPhone, FiUser } from 'react-icons/fi'
import toast from 'react-hot-toast'
import { registerApi } from '../api/auth'

const PHONE_PATTERN = /^(0|\+84)\d{9}$/
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).+$/

function apiErrorMessage(err) {
  const data = err.response?.data
  if (data?.errors && typeof data.errors === 'object') {
    const first = Object.values(data.errors)[0]
    if (first) return first
  }
  return data?.message || err.message || 'Đăng ký thất bại'
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [showConfirmPw, setShowConfirmPw] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    const name = fullName.trim()
    const mail = email.trim()
    const phoneNorm = phone.trim()

    if (!name || !mail || !phoneNorm || !password || !confirmPassword) {
      toast.error('Vui lòng nhập đầy đủ thông tin')
      return
    }
    if (!PHONE_PATTERN.test(phoneNorm)) {
      toast.error('Số điện thoại không hợp lệ (vd: 0912345678 hoặc +84912345678)')
      return
    }
    if (password.length < 8 || password.length > 64) {
      toast.error('Mật khẩu từ 8 đến 64 ký tự')
      return
    }
    if (!PASSWORD_PATTERN.test(password)) {
      toast.error('Mật khẩu phải có cả chữ và số')
      return
    }
    if (password !== confirmPassword) {
      toast.error('Mật khẩu xác nhận không khớp')
      return
    }

    setSubmitting(true)
    try {
      await registerApi({ fullName: name, email: mail, phone: phoneNorm, password })
      toast.success('Đăng ký thành công! Vui lòng đăng nhập.')
      navigate('/login', { replace: true })
    } catch (err) {
      toast.error(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-brand">
        <div className="login-brand-icon">S</div>
        <h1>SmartBanking</h1>
        <p>Tạo tài khoản ngân hàng số miễn phí</p>
      </div>

      <div className="login-card">
        <h2>Đăng ký tài khoản</h2>
        <form onSubmit={handleSubmit}>
          <div className="input-icon-wrap">
            <FiUser />
            <input
              type="text"
              placeholder="Họ và tên"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              autoComplete="name"
              maxLength={150}
            />
          </div>
          <div className="input-icon-wrap">
            <FiMail />
            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              maxLength={150}
            />
          </div>
          <div className="input-icon-wrap">
            <FiPhone />
            <input
              type="tel"
              placeholder="Số điện thoại (vd: 0912345678)"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              autoComplete="tel"
            />
          </div>
          <div className="input-icon-wrap">
            <FiLock />
            <input
              type={showPw ? 'text' : 'password'}
              placeholder="Mật khẩu (ít nhất 8 ký tự, có chữ và số)"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
              maxLength={64}
            />
            <button
              type="button"
              className="toggle-pw"
              onClick={() => setShowPw((v) => !v)}
              aria-label={showPw ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            >
              {showPw ? <FiEyeOff /> : <FiEye />}
            </button>
          </div>
          <div className="input-icon-wrap">
            <FiLock />
            <input
              type={showConfirmPw ? 'text' : 'password'}
              placeholder="Xác nhận mật khẩu"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
              maxLength={64}
            />
            <button
              type="button"
              className="toggle-pw"
              onClick={() => setShowConfirmPw((v) => !v)}
              aria-label={showConfirmPw ? 'Ẩn mật khẩu xác nhận' : 'Hiện mật khẩu xác nhận'}
            >
              {showConfirmPw ? <FiEyeOff /> : <FiEye />}
            </button>
          </div>
          <button
            type="submit"
            className="btn-primary"
            style={{ maxWidth: 'none' }}
            disabled={submitting}
          >
            {submitting ? 'Đang xử lý...' : 'Đăng ký'}
          </button>
        </form>
        <hr className="login-divider" />
        <p className="login-register">
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </p>
      </div>
    </div>
  )
}
