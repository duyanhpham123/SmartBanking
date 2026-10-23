import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'
import { PageHeader } from '../components/ui/FormBits'

export function SettingsPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()

  return (
    <div className="page-card">
      <PageHeader title="Cài đặt" backTo={() => navigate(-1)} />
      <div className="form-stack">
        <div className="form-field">
          <label>Ngôn ngữ</label>
          <select className="form-input">
            <option>Tiếng Việt</option>
            <option>English</option>
          </select>
        </div>
        <div className="form-field">
          <label>Bảo mật</label>
          <label className="login-remember">
            <input type="checkbox" defaultChecked />
            Xác thực hai yếu tố (2FA)
          </label>
        </div>
        <button
          type="button"
          className="btn-primary"
          style={{ background: '#dc2626' }}
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
