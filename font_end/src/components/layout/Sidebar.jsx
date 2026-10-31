import { NavLink } from 'react-router-dom'
import {
  FiHome,
  FiCreditCard,
  FiRepeat,
  FiPlusCircle,
  FiMinusCircle,
  FiFileText,
  FiClock,
  FiBell,
  FiSettings,
  FiUser,
} from 'react-icons/fi'

const userLinks = [
  { to: '/', icon: FiHome, label: 'Trang chủ' },
  { to: '/accounts', icon: FiCreditCard, label: 'Tài khoản' },
  { to: '/transfer', icon: FiRepeat, label: 'Chuyển tiền' },
  { to: '/deposit', icon: FiPlusCircle, label: 'Nạp tiền' },
  { to: '/withdraw', icon: FiMinusCircle, label: 'Rút tiền' },
  { to: '/bills', icon: FiFileText, label: 'Thanh toán hóa đơn' },
  { to: '/history', icon: FiClock, label: 'Lịch sử giao dịch' },
  { to: '/notifications', icon: FiBell, label: 'Thông báo' },
  { to: '/profile', icon: FiUser, label: 'Hồ sơ cá nhân' },
  { to: '/settings', icon: FiSettings, label: 'Cài đặt' },
]

const adminLinks = [
  { to: '/admin', icon: FiHome, label: 'Tổng quan', end: true },
  { to: '/admin/users', icon: FiUser, label: 'Quản lý khách hàng' },
  { to: '/admin/accounts', icon: FiCreditCard, label: 'Quản lý tài khoản' },
  { to: '/admin/transactions', icon: FiRepeat, label: 'Quản lý giao dịch' },
  { to: '/admin/stats', icon: FiClock, label: 'Thống kê' },
  { to: '/admin/audit-logs', icon: FiFileText, label: 'Nhật ký hệ thống' },
  { to: '/admin/settings', icon: FiSettings, label: 'Cài đặt hệ thống' },
]

export function UserSidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-brand-icon">S</span>
        SmartBanking
      </div>
      <nav className="sidebar-nav">
        {userLinks.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}

export function AdminSidebar({ onLogout }) {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-brand-icon">A</span>
        Admin Dashboard
      </div>
      <nav className="sidebar-nav">
        {adminLinks.map(({ to, icon: Icon, label, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>
      <button type="button" className="sidebar-link" onClick={onLogout} style={{ marginTop: 'auto' }}>
        <FiSettings size={18} />
        Đăng xuất
      </button>
    </aside>
  )
}
