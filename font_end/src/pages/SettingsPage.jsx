import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { logoutAllApi } from '../api/auth'
import { useAuth } from '../context/AuthContext'
import { PageHeader } from '../components/ui/FormBits'
import { getApiErrorMessage } from '../utils/adminDisplay'

export function SettingsPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [busy, setBusy] = useState(false)

  async function handleLogoutAll() {
    if (!window.confirm('Đăng xuất khỏi tất cả thiết bị, kể cả thiết bị này?')) return
    try {
      setBusy(true)
      const res = await logoutAllApi()
      toast.success(res?.message ?? 'Đã đăng xuất khỏi tất cả thiết bị')
      logout()
      navigate('/login')
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page-card">
      <PageHeader title="Cài đặt" backTo={() => navigate(-1)} />
      <div className="form-stack">
        <div className="form-field">
          <label>Bảo mật</label>
          <p className="profile-note" style={{ margin: '0 0 0.75rem' }}>
            Mọi giao dịch chuyển tiền và thanh toán đều yêu cầu mã OTP. Phiên đăng nhập tự kết thúc sau 10 phút
            không thao tác.
          </p>
          <div className="profile-actions" style={{ marginTop: 0 }}>
            <button type="button" className="btn-secondary" onClick={() => navigate('/profile')}>
              Đổi mật khẩu / Hồ sơ
            </button>
            <button type="button" className="btn-secondary" disabled={busy} onClick={handleLogoutAll}>
              Đăng xuất khỏi tất cả thiết bị
            </button>
          </div>
        </div>
        <button
          type="button"
          className="btn-primary btn-danger"
          onClick={() => {
            logout()
            toast.success('Đã đăng xuất')
            navigate('/login')
          }}
        >
          Đăng xuất
        </button>
      </div>
    </div>
  )
}
