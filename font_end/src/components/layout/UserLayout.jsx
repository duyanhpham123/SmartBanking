import { useEffect, useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { FiBell, FiSearch } from 'react-icons/fi'
import { fetchUnreadNotificationCount } from '../../api/notifications'
import { useAuth } from '../../context/AuthContext'
import { initials } from '../../utils/profileDisplay'
import { UserSidebar } from './Sidebar'

export function UserLayout() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const data = await fetchUnreadNotificationCount()
        if (!cancelled) setUnread(Number(data?.unreadCount ?? 0))
      } catch {
        if (!cancelled) setUnread(0)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])


  return (
    <div className="app-shell">
      <UserSidebar />
      <div className="main-area">
        <header className="top-header">
          <div className="header-search">
            <FiSearch className="header-search-icon" />
            <input type="search" placeholder="Tìm kiếm chức năng..." aria-label="Tìm kiếm" />
          </div>
          <div className="header-actions">
            <button
              type="button"
              className="header-bell"
              aria-label="Thông báo"
              onClick={() => navigate('/notifications')}
            >
              <FiBell />
              {unread > 0 && <span className="header-bell-dot" />}
            </button>
            <button
              type="button"
              className="header-user"
              onClick={() => navigate('/profile')}
              title="Hồ sơ cá nhân"
            >
              {/* Avatar chữ cái đầu: không gửi email khách hàng sang dịch vụ ảnh bên ngoài */}
              <span className="header-avatar" aria-hidden="true">
                {initials(user?.name)}
              </span>
              <div className="header-user-meta">
                <div className="header-user-name">{user?.name ?? 'Người dùng'}</div>
                <div className="header-user-role">{user?.role ?? 'USER'}</div>
              </div>
            </button>
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
