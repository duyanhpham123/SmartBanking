import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { fetchAdminUsers, lockAdminUser, unlockAdminUser } from '../../api/admin'
import { PageHeader } from '../../components/ui/FormBits'
import { getApiErrorMessage, statusBadgeClass, userStatusLabel } from '../../utils/adminDisplay'
import { KYC_STATUS_OPTIONS } from '../../utils/profileDisplay'
import { AdminPagination } from './AdminPagination'

export function AdminUsersPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState('')
  const [kycStatus, setKycStatus] = useState('')

  const load = useCallback(async () => {
    try {
      setLoading(true)
      const result = await fetchAdminUsers({
        keyword: search || undefined,
        kycStatus: kycStatus || undefined,
        page,
        size: 15,
      })
      setData(result)
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không tải được danh sách người dùng'))
    } finally {
      setLoading(false)
    }
  }, [page, search, kycStatus])

  useEffect(() => {
    load()
  }, [load])

  async function toggleLock(user) {
    try {
      if (user.status === 'LOCKED') {
        await unlockAdminUser(user.id)
        toast.success('Đã mở khóa người dùng')
      } else {
        await lockAdminUser(user.id)
        toast.success('Đã khóa người dùng')
      }
      load()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  return (
    <div className="page-card">
      <PageHeader title="Quản lý khách hàng" />
      <form
        style={{ marginBottom: '1rem', maxWidth: 720, display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}
        onSubmit={(e) => {
          e.preventDefault()
          setPage(0)
          setSearch(keyword.trim())
        }}
      >
        <input
          placeholder="Tìm theo tên, email, SĐT..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <select
          className="form-input"
          style={{ maxWidth: 200 }}
          value={kycStatus}
          aria-label="Lọc theo trạng thái eKYC"
          onChange={(e) => {
            setPage(0)
            setKycStatus(e.target.value)
          }}
        >
          <option value="">eKYC: Tất cả</option>
          {KYC_STATUS_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              eKYC: {o.label}
            </option>
          ))}
        </select>
        <button type="submit" className="btn-primary" style={{ maxWidth: 120 }}>
          Tìm
        </button>
      </form>

      {loading && <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải...</p>}

      {!loading && (
        <>
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Tên đăng nhập</th>
                  <th>Họ tên</th>
                  <th>Email</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={6} style={{ color: 'var(--sb-text-muted)' }}>
                      Không có dữ liệu
                    </td>
                  </tr>
                )}
                {data.content.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.username}</td>
                    <td>
                      <Link to={`/admin/users/${user.id}`}>{user.fullName}</Link>
                    </td>
                    <td>{user.email ?? '—'}</td>
                    <td>
                      <span className={`status-badge ${statusBadgeClass(user.status)}`}>
                        {userStatusLabel(user.status)}
                      </span>
                    </td>
                    <td style={{ display: 'flex', gap: '0.75rem' }}>
                      <Link to={`/admin/users/${user.id}`} className="back-link">
                        Chi tiết
                      </Link>
                      {user.status !== 'DELETED' && (
                        <button type="button" className="back-link" onClick={() => toggleLock(user)}>
                          {user.status === 'LOCKED' ? 'Mở khóa' : 'Khóa'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <AdminPagination
            page={page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  )
}
