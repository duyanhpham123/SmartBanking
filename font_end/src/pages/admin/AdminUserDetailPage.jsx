import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import {
  fetchAdminUser,
  lockAdminUser,
  rejectAdminUserKyc,
  unlockAdminUser,
  verifyAdminUserKyc,
} from '../../api/admin'
import { InfoList, PageHeader } from '../../components/ui/FormBits'
import { formatBalance } from '../../data/mockData'
import {
  accountStatusLabel,
  formatDateTime,
  getApiErrorMessage,
  statusBadgeClass,
  userStatusLabel,
} from '../../utils/adminDisplay'
import {
  formatDate,
  genderLabel,
  idTypeLabel,
  incomeLabel,
  initials,
  kycBadgeClass,
  kycStatusLabel,
} from '../../utils/profileDisplay'

export function AdminUserDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setData(await fetchAdminUser(id))
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không tải được thông tin khách hàng'))
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  async function toggleLock() {
    const locked = data.user.status === 'LOCKED'
    if (!locked && !window.confirm('Khóa người dùng này? Mọi phiên đăng nhập của họ sẽ bị thu hồi.')) return
    try {
      if (locked) {
        await unlockAdminUser(id)
        toast.success('Đã mở khóa người dùng')
      } else {
        await lockAdminUser(id)
        toast.success('Đã khóa người dùng')
      }
      load()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  if (loading && !data) {
    return (
      <div className="page-card">
        <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải...</p>
      </div>
    )
  }

  if (!data) {
    return (
      <div className="page-card">
        <PageHeader title="Chi tiết khách hàng" backTo={() => navigate('/admin/users')} />
        <p style={{ color: 'var(--sb-text-muted)' }}>Không tìm thấy khách hàng.</p>
      </div>
    )
  }

  const { user, profile, accounts, recentActivities } = data

  return (
    <div className="page-card">
      <PageHeader title="Chi tiết khách hàng" backTo={() => navigate('/admin/users')} />

      <div className="profile-hero">
        <div className="profile-avatar">{initials(user.fullName)}</div>
        <div className="profile-hero-body">
          <div className="profile-hero-name">{user.fullName}</div>
          <div className="profile-hero-meta">
            ID #{user.id} · @{user.username} · Đăng ký {formatDate(user.createdAt)}
          </div>
        </div>
        <span className={`status-badge ${statusBadgeClass(user.status)}`}>{userStatusLabel(user.status)}</span>
        <span className={`status-badge ${kycBadgeClass(profile?.kycStatus)}`}>
          eKYC: {kycStatusLabel(profile?.kycStatus)}
        </span>
        {user.status !== 'DELETED' && !user.roles?.includes('ROLE_ADMIN') && (
          <button
            type="button"
            className={`btn-primary${user.status === 'LOCKED' ? '' : ' btn-danger'}`}
            style={{ width: 'auto', padding: '0.6rem 1.1rem', fontSize: '0.9rem' }}
            onClick={toggleLock}
          >
            {user.status === 'LOCKED' ? 'Mở khóa' : 'Khóa người dùng'}
          </button>
        )}
      </div>

      <div className="detail-grid">
        <section className="panel">
          <h3>Thông tin cá nhân</h3>
          <InfoList
            rows={[
              ['Họ và tên', user.fullName],
              ['Ngày sinh', formatDate(user.dateOfBirth)],
              ['Giới tính', genderLabel(user.gender)],
              ['Email', `${user.email ?? '—'}${user.emailVerified ? ' ✓' : ' (chưa xác thực)'}`],
              ['Số điện thoại', `${user.phone ?? '—'}${user.phoneVerified ? ' ✓' : ' (chưa xác thực)'}`],
              ['Vai trò', (user.roles ?? []).map((r) => r.replace('ROLE_', '')).join(', ')],
              ['Đăng nhập gần nhất', formatDateTime(user.lastLoginAt)],
            ]}
          />
        </section>

        <KycPanel userId={id} profile={profile} onChanged={load} />

        <section className="panel wide">
          <h3>Tài khoản ngân hàng ({accounts.length})</h3>
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Số tài khoản</th>
                  <th>Tên</th>
                  <th>Loại</th>
                  <th>Số dư</th>
                  <th>Trạng thái</th>
                  <th>Ngày mở</th>
                </tr>
              </thead>
              <tbody>
                {accounts.length === 0 && (
                  <tr>
                    <td colSpan={6} style={{ color: 'var(--sb-text-muted)' }}>
                      Chưa có tài khoản
                    </td>
                  </tr>
                )}
                {accounts.map((a) => (
                  <tr key={a.id}>
                    <td>{a.accountNumber}</td>
                    <td>{a.accountName ?? '—'}</td>
                    <td>{a.accountType}</td>
                    <td>{formatBalance(Number(a.balance ?? 0), a.currency)}</td>
                    <td>
                      <span className={`status-badge ${statusBadgeClass(a.status)}`}>
                        {accountStatusLabel(a.status)}
                      </span>
                    </td>
                    <td>{formatDate(a.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel wide">
          <h3>Hoạt động gần đây</h3>
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Hành động</th>
                  <th>Địa chỉ IP</th>
                  <th>Thiết bị</th>
                  <th>Kết quả</th>
                </tr>
              </thead>
              <tbody>
                {recentActivities.length === 0 && (
                  <tr>
                    <td colSpan={5} style={{ color: 'var(--sb-text-muted)' }}>
                      Chưa có hoạt động
                    </td>
                  </tr>
                )}
                {recentActivities.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDateTime(log.createdAt)}</td>
                    <td>{log.description ?? log.action}</td>
                    <td>{log.ipAddress ?? '—'}</td>
                    <td title={log.userAgent ?? ''}>{shortAgent(log.userAgent)}</td>
                    <td>
                      <span className={`status-badge ${log.responseStatus < 400 ? 'success' : 'failed'}`}>
                        {log.responseStatus ?? '—'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  )
}

function KycPanel({ userId, profile, onChanged }) {
  const [rejecting, setRejecting] = useState(false)
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const status = profile?.kycStatus ?? 'NOT_SUBMITTED'

  async function verify() {
    if (!window.confirm('Xác nhận đã đối chiếu giấy tờ và duyệt hồ sơ này?')) return
    try {
      setBusy(true)
      await verifyAdminUserKyc(userId)
      toast.success('Đã xác minh hồ sơ')
      onChanged()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function reject() {
    if (!note.trim()) {
      toast.error('Vui lòng nhập lý do từ chối')
      return
    }
    try {
      setBusy(true)
      await rejectAdminUserKyc(userId, note.trim())
      toast.success('Đã từ chối hồ sơ')
      setRejecting(false)
      setNote('')
      onChanged()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <h3>Định danh (eKYC)</h3>
        <span className={`status-badge ${kycBadgeClass(status)}`}>{kycStatusLabel(status)}</span>
      </div>

      {status === 'NOT_SUBMITTED' ? (
        <p style={{ color: 'var(--sb-text-muted)', margin: 0 }}>Khách hàng chưa khai báo thông tin định danh.</p>
      ) : (
        <InfoList
          rows={[
            ['Loại giấy tờ', idTypeLabel(profile.idType)],
            ['Số giấy tờ', profile.idNumber],
            ['Ngày cấp', formatDate(profile.idIssueDate)],
            ['Nơi cấp', profile.idIssuePlace],
            ['Quốc tịch', profile.nationality],
            ['Thường trú', profile.permanentAddress],
            ['Hiện tại', profile.currentAddress],
            ['Nghề nghiệp', profile.occupation],
            ['Nơi làm việc', profile.employer],
            ['Thu nhập', incomeLabel(profile.monthlyIncome)],
            ['Ngày gửi', formatDateTime(profile.submittedAt)],
            ['Ngày xét duyệt', formatDateTime(profile.reviewedAt)],
            ...(profile.kycNote ? [['Lý do từ chối', profile.kycNote]] : []),
          ]}
        />
      )}

      {(status === 'PENDING' || status === 'VERIFIED') && (
        <div className="kyc-review">
          {rejecting ? (
            <>
              <textarea
                className="form-textarea"
                placeholder="Lý do từ chối (khách hàng sẽ nhận được thông báo này)"
                maxLength={500}
                value={note}
                onChange={(e) => setNote(e.target.value)}
              />
              <div className="kyc-review-actions">
                <button type="button" className="btn-primary btn-danger" disabled={busy} onClick={reject}>
                  Xác nhận từ chối
                </button>
                <button type="button" className="btn-secondary" onClick={() => setRejecting(false)}>
                  Hủy
                </button>
              </div>
            </>
          ) : (
            <div className="kyc-review-actions" style={{ marginTop: 0 }}>
              {status === 'PENDING' && (
                <button type="button" className="btn-primary" disabled={busy} onClick={verify}>
                  Duyệt hồ sơ
                </button>
              )}
              <button type="button" className="btn-secondary" onClick={() => setRejecting(true)}>
                {status === 'VERIFIED' ? 'Thu hồi xác minh' : 'Từ chối'}
              </button>
            </div>
          )}
        </div>
      )}
    </section>
  )
}

// Rút gọn user-agent thành "Chrome · Windows" cho dễ đọc
function shortAgent(ua) {
  if (!ua) return '—'
  const browser = /Edg\//.test(ua)
    ? 'Edge'
    : /Chrome\//.test(ua)
      ? 'Chrome'
      : /Firefox\//.test(ua)
        ? 'Firefox'
        : /Safari\//.test(ua)
          ? 'Safari'
          : 'Khác'
  const os = /Windows/.test(ua)
    ? 'Windows'
    : /Android/.test(ua)
      ? 'Android'
      : /iPhone|iPad/.test(ua)
        ? 'iOS'
        : /Mac OS/.test(ua)
          ? 'macOS'
          : /Linux/.test(ua)
            ? 'Linux'
            : ''
  return os ? `${browser} · ${os}` : browser
}
