import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { FiBell, FiGift, FiHome, FiShield } from 'react-icons/fi'
import toast from 'react-hot-toast'
import {
  fetchMyNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from '../api/notifications'
import { PageHeader } from '../components/ui/FormBits'
import { formatDateTime, getApiErrorMessage } from '../utils/adminDisplay'
import { notificationVariant } from '../utils/userDisplay'
import { AdminPagination } from './admin/AdminPagination'

const ICONS = {
  success: FiHome,
  security: FiShield,
  deposit: FiBell,
  promo: FiGift,
}

export function NotificationsPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      const result = await fetchMyNotifications({ page, size: 10 })
      setData(result)
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không tải được thông báo'))
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    load()
  }, [load])

  const handleOpen = async (n) => {
    if (n.read) return
    try {
      await markNotificationAsRead(n.id)
      setData((prev) => ({
        ...prev,
        content: prev.content.map((item) => (item.id === n.id ? { ...item, read: true } : item)),
      }))
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không cập nhật được thông báo'))
    }
  }

  const handleMarkAll = async () => {
    try {
      await markAllNotificationsAsRead()
      toast.success('Đã đánh dấu tất cả đã đọc')
      load()
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không cập nhật được'))
    }
  }

  return (
    <div className="page-card">
      <PageHeader title="Thông báo" backTo={() => navigate(-1)} />
      <div style={{ marginBottom: '1rem' }}>
        <button type="button" className="page-btn" onClick={handleMarkAll}>
          Đánh dấu tất cả đã đọc
        </button>
      </div>

      {loading && <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải...</p>}

      {!loading && data.content.length === 0 && (
        <p style={{ color: 'var(--sb-text-muted)' }}>Không có thông báo.</p>
      )}

      <div className="notif-list">
        {data.content.map((n) => {
          const variant = notificationVariant(n.notificationType)
          const Icon = ICONS[variant] ?? FiBell
          return (
            <article
              key={n.id}
              className={`notif-item${n.read ? '' : ' unread'}`}
              style={{ cursor: 'pointer', opacity: n.read ? 0.85 : 1 }}
              onClick={() => handleOpen(n)}
              onKeyDown={(e) => e.key === 'Enter' && handleOpen(n)}
              role="button"
              tabIndex={0}
            >
              <div className={`notif-icon ${variant}`}>
                <Icon />
              </div>
              <div>
                <div className="notif-title">{n.title}</div>
                <div className="notif-message">{n.message}</div>
                <div className="notif-time">{formatDateTime(n.createdAt)}</div>
              </div>
            </article>
          )
        })}
      </div>

      <AdminPagination
        page={page}
        totalPages={data.totalPages}
        totalElements={data.totalElements}
        onPageChange={setPage}
      />
    </div>
  )
}
