import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FiEye, FiEyeOff, FiLock, FiUser } from 'react-icons/fi'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'

export function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [remember, setRemember] = useState(false)

  const handleSubmit = async (e, asAdmin = false) => {
    e.preventDefault()
    if (!identifier.trim() || !password.trim()) {
      toast.error('Vui lòng nhập đầy đủ thông tin')
      return
    }
    try {
      await login({ identifier, password, remember }, asAdmin)
      toast.success(asAdmin ? 'Đăng nhập Admin thành công' : 'Đăng nhập thành công')
      navigate(asAdmin ? '/admin' : '/')
    } catch (err) {
      const message = err.response?.data?.message || err.message || 'Đăng nhập thất bại'
      toast.error(message)
    }
  }

  return (
    <div className="login-page">
      <div className="login-brand">
        <div className="login-brand-icon">S</div>
        <h1>SmartBanking</h1>
        <p>Ngân hàng số - Cuộc sống dễ dàng hơn</p>
      </div>

      <div className="login-card">
        <h2>Đăng nhập</h2>
        <form onSubmit={(e) => handleSubmit(e, false)}>
          <div className="input-icon-wrap">
            <FiUser />
            <input
              type="text"
              placeholder="Số điện thoại / Email"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              autoComplete="username"
            />
          </div>
          <div className="input-icon-wrap">
            <FiLock />
            <input
              type={showPw ? 'text' : 'password'}
              placeholder="Mật khẩu"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
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
          <label className="login-remember">
            <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
            Ghi nhớ đăng nhập
          </label>
          <button type="submit" className="btn-primary" style={{ maxWidth: 'none' }}>
            Đăng nhập
          </button>
        </form>
        <Link to="#" className="login-forgot" onClick={(e) => e.preventDefault()}>
          Quên mật khẩu?
        </Link>
        <hr className="login-divider" />
        <p className="login-register">
          Chưa có tài khoản? <Link to="#">Đăng ký ngay</Link>
        </p>
      </div>

      <p className="login-admin-hint">
        Dành cho quản trị viên:{' '}
        <Link to="/admin/login">Admin Dashboard</Link>
      </p>
    </div>
  )
}

export function AdminLoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')

  return (
    <div className="login-page">
      <div className="login-brand">
        <div className="login-brand-icon">A</div>
        <h1>SmartBanking Admin</h1>
      </div>
      <div className="login-card">
        <h2>Đăng nhập Admin</h2>
        <form
          onSubmit={async (e) => {
            e.preventDefault()
            try {
              await login({ identifier, password, remember: false }, true)
              toast.success('Đăng nhập Admin thành công')
              navigate('/admin')
            } catch (err) {
              const message = err.response?.data?.message || err.message || 'Đăng nhập thất bại'
              toast.error(message)
            }
          }}
        >
          <div className="input-icon-wrap">
            <FiUser />
            <input
              placeholder="Email admin"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
            />
          </div>
          <div className="input-icon-wrap">
            <FiLock />
            <input
              type="password"
              placeholder="Mật khẩu"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" className="btn-primary" style={{ maxWidth: 'none' }}>
            Đăng nhập
          </button>
        </form>
        <Link to="/login" className="login-forgot">
          ← Quay lại đăng nhập người dùng
        </Link>
      </div>
    </div>
  )
}
