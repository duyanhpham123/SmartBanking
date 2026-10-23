import { useEffect, useMemo, useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { FiCreditCard, FiRepeat, FiUsers, FiDollarSign } from 'react-icons/fi'
import toast from 'react-hot-toast'
import { AdminSidebar } from '../../components/layout/Sidebar'
import {
  fetchAdminOverview,
  fetchAdminRecentTransactions,
  fetchAdminTransactionsByType,
} from '../../api/admin'
import { formatMoney } from '../../data/mockData'
import { useAuth } from '../../context/AuthContext'
import { PageHeader } from '../../components/ui/FormBits'
import {
  TX_TYPE_COLORS,
  formatDateTime,
  getApiErrorMessage,
  statusBadgeClass,
  transactionStatusLabel,
  transactionTypeLabel,
  txParticipantLabel,
} from '../../utils/adminDisplay'

export { AdminUsersPage } from './AdminUsersPage'
export { AdminAccountsPage } from './AdminAccountsPage'
export { AdminTransactionsPage } from './AdminTransactionsPage'
export { AdminStatsPage } from './AdminStatsPage'
export { AdminAuditLogsPage } from './AdminAuditLogsPage'

export function AdminLayout() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="app-shell">
      <AdminSidebar
        onLogout={() => {
          logout()
          navigate('/admin/login')
        }}
      />
      <div className="main-area">
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export function AdminDashboardPage() {
  const [overview, setOverview] = useState(null)
  const [typeStats, setTypeStats] = useState(null)
  const [recentTx, setRecentTx] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        setLoading(true)
        const [ov, stats, recent] = await Promise.all([
          fetchAdminOverview(),
          fetchAdminTransactionsByType(),
          fetchAdminRecentTransactions(10),
        ])
        if (!cancelled) {
          setOverview(ov)
          setTypeStats(stats)
          setRecentTx(Array.isArray(recent) ? recent : [])
        }
      } catch (err) {
        if (!cancelled) toast.error(getApiErrorMessage(err, 'Không tải được dữ liệu tổng quan'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  const pieItems = useMemo(() => {
    if (!typeStats?.items?.length) return []
    return typeStats.items.map((item) => ({
      name: transactionTypeLabel(item.type),
      value: item.percent,
      count: item.count,
      type: item.type,
    }))
  }, [typeStats])

  const totalTx = typeStats?.total ?? overview?.totalTransactions ?? 0

  return (
    <>
      <PageHeader title="Tổng quan hệ thống" />
      {loading && <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải dữ liệu...</p>}

      {!loading && overview && (
        <div className="admin-stat-grid">
          <div className="stat-card">
            <div className="stat-card-label">Tổng người dùng</div>
            <div className="stat-card-value">{Number(overview.totalUsers).toLocaleString('vi-VN')}</div>
            <span className="stat-card-icon" style={{ background: '#eff6ff', color: '#2563eb' }}>
              <FiUsers />
            </span>
          </div>
          <div className="stat-card">
            <div className="stat-card-label">Tổng tài khoản</div>
            <div className="stat-card-value">{Number(overview.totalAccounts).toLocaleString('vi-VN')}</div>
            <span className="stat-card-icon" style={{ background: '#dcfce7', color: '#16a34a' }}>
              <FiCreditCard />
            </span>
          </div>
          <div className="stat-card">
            <div className="stat-card-label">Tổng giao dịch</div>
            <div className="stat-card-value">{Number(overview.totalTransactions).toLocaleString('vi-VN')}</div>
            <span className="stat-card-icon" style={{ background: '#f3e8ff', color: '#9333ea' }}>
              <FiRepeat />
            </span>
          </div>
          <div className="stat-card">
            <div className="stat-card-label">Tổng số tiền giao dịch (VND)</div>
            <div className="stat-card-value" style={{ fontSize: '1.1rem' }}>
              {formatMoney(Number(overview.totalAmountVnd ?? 0))}
            </div>
            <span className="stat-card-icon" style={{ background: '#dcfce7', color: '#16a34a' }}>
              <FiDollarSign />
            </span>
          </div>
        </div>
      )}

      {!loading && (
        <div className="admin-grid">
          <div className="panel">
            <h3>Thống kê giao dịch theo loại</h3>
            {pieItems.length === 0 ? (
              <p style={{ color: 'var(--sb-text-muted)' }}>Chưa có giao dịch</p>
            ) : (
              <div className="chart-row">
                <ResponsiveContainer width={200} height={200}>
                  <PieChart>
                    <Pie data={pieItems} innerRadius={55} outerRadius={80} paddingAngle={2} dataKey="value">
                      {pieItems.map((entry) => (
                        <Cell key={entry.type} fill={TX_TYPE_COLORS[entry.type] ?? '#94a3b8'} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(v) => [`${Number(v).toFixed(1)}%`, 'Tỷ lệ']} />
                  </PieChart>
                </ResponsiveContainer>
                <div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.75rem' }}>
                    {Number(totalTx).toLocaleString('vi-VN')}
                  </div>
                  <div className="donut-legend">
                    {pieItems.map((d) => (
                      <div key={d.type} className="legend-item">
                        <span
                          className="legend-dot"
                          style={{ background: TX_TYPE_COLORS[d.type] ?? '#94a3b8' }}
                        />
                        {d.name} — {d.value.toFixed(1)}% ({d.count})
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="panel">
            <h3>Giao dịch gần đây (Admin)</h3>
            <div style={{ overflowX: 'auto' }}>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Thời gian</th>
                    <th>Người dùng</th>
                    <th>Loại</th>
                    <th>Số tiền</th>
                    <th>Trạng thái</th>
                  </tr>
                </thead>
                <tbody>
                  {recentTx.length === 0 && (
                    <tr>
                      <td colSpan={5} style={{ color: 'var(--sb-text-muted)' }}>
                        Không có giao dịch
                      </td>
                    </tr>
                  )}
                  {recentTx.map((row) => (
                    <tr key={row.id}>
                      <td>{formatDateTime(row.createdAt)}</td>
                      <td>{txParticipantLabel(row)}</td>
                      <td>{transactionTypeLabel(row.transactionType)}</td>
                      <td>{formatMoney(Number(row.amount), row.currency ?? 'VND')}</td>
                      <td>
                        <span className={`status-badge ${statusBadgeClass(row.status)}`}>
                          {transactionStatusLabel(row.status)}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      <div className="admin-footer">SmartBanking</div>
    </>
  )
}
