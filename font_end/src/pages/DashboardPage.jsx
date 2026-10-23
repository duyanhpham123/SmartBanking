import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { FiArrowDownLeft, FiArrowUpRight, FiBell, FiCreditCard, FiEye, FiTrendingUp } from 'react-icons/fi'
import toast from 'react-hot-toast'
import {
  fetchDashboardChart,
  fetchDashboardRecentTransactions,
  fetchDashboardSummary,
} from '../api/dashboard'
import { formatMoney } from '../data/mockData'
import { useAuth } from '../context/AuthContext'
import {
  formatChartDate,
  formatDateTime,
  getApiErrorMessage,
  transactionTypeLabel,
} from '../utils/adminDisplay'
import { transactionSignedAmount } from '../utils/userDisplay'

export function DashboardPage() {
  const { user } = useAuth()
  const [summary, setSummary] = useState(null)
  const [chart, setChart] = useState(null)
  const [recentTx, setRecentTx] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        setLoading(true)
        const [summaryData, chartData, recent] = await Promise.all([
          fetchDashboardSummary(),
          fetchDashboardChart(7, 'VND'),
          fetchDashboardRecentTransactions(5),
        ])
        if (!cancelled) {
          setSummary(summaryData)
          setChart(chartData)
          setRecentTx(Array.isArray(recent) ? recent : [])
        }
      } catch (err) {
        if (!cancelled) toast.error(getApiErrorMessage(err, 'Không tải được trang chủ'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  const chartPoints = useMemo(() => {
    if (!chart?.points) return []
    return chart.points.map((p) => ({
      date: formatChartDate(p.date),
      total: Number(p.totalAmount ?? 0),
    }))
  }, [chart])

  const totalBalance = Number(summary?.totalBalanceVnd ?? 0)

  return (
    <>
      <div className="welcome-block">
        <h2>Xin chào, {user?.name ?? 'bạn'} 👋</h2>
        <p>Chúc bạn một ngày tốt lành!</p>
      </div>

      {loading && <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải dữ liệu...</p>}

      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-card-label">Tổng số dư</div>
          <div className="stat-card-value">{formatMoney(totalBalance)}</div>
          <span className="stat-card-icon" style={{ background: '#eff6ff', color: '#2563eb' }}>
            <FiEye />
          </span>
        </div>
        <div className="stat-card">
          <div className="stat-card-label">Tài khoản</div>
          <div className="stat-card-value">{summary?.accountCount ?? '—'}</div>
          <span className="stat-card-icon" style={{ background: '#dcfce7', color: '#16a34a' }}>
            <FiCreditCard />
          </span>
        </div>
        <div className="stat-card">
          <div className="stat-card-label">Giao dịch tháng này</div>
          <div className="stat-card-value">{summary?.transactionsThisMonth ?? '—'}</div>
          <span className="stat-card-icon" style={{ background: '#eff6ff', color: '#2563eb' }}>
            <FiTrendingUp />
          </span>
        </div>
        <div className="stat-card">
          <div className="stat-card-label">Thông báo mới</div>
          <div className="stat-card-value">{summary?.unreadNotifications ?? '—'}</div>
          <span className="stat-card-icon" style={{ background: '#fee2e2', color: '#dc2626' }}>
            <FiBell />
          </span>
        </div>
      </div>

      <div className="dashboard-grid">
        <div className="panel">
          <h3>Biểu đồ giao dịch 7 ngày gần đây</h3>
          {!loading && chartPoints.length === 0 && (
            <p style={{ color: 'var(--sb-text-muted)' }}>Chưa có dữ liệu biểu đồ.</p>
          )}
          {chartPoints.length > 0 && (
            <ResponsiveContainer width="100%" height={260}>
              <AreaChart data={chartPoints}>
                <defs>
                  <linearGradient id="chartFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#2563eb" stopOpacity={0.35} />
                    <stop offset="100%" stopColor="#2563eb" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="date" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis
                  tickFormatter={(v) => `${Math.round(v / 1_000_000)}M`}
                  tick={{ fontSize: 12 }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip formatter={(v) => [formatMoney(Number(v)), 'Tổng GD']} />
                <Area type="monotone" dataKey="total" stroke="#2563eb" strokeWidth={2} fill="url(#chartFill)" />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="panel">
          <h3>Giao dịch gần đây</h3>
          {!loading && recentTx.length === 0 && (
            <p style={{ color: 'var(--sb-text-muted)' }}>Chưa có giao dịch.</p>
          )}
          {recentTx.map((tx) => {
            const signed = transactionSignedAmount(tx)
            return (
              <div key={tx.id} className="tx-list-item">
                <div className="tx-list-icon">
                  {signed < 0 ? <FiArrowUpRight /> : <FiArrowDownLeft />}
                </div>
                <div className="tx-list-body">
                  <div className="tx-list-type">{transactionTypeLabel(tx.transactionType)}</div>
                  <div className="tx-list-date">{formatDateTime(tx.createdAt)}</div>
                </div>
                <div className={`tx-amount ${signed >= 0 ? 'positive' : 'negative'}`}>
                  {signed >= 0 ? '+' : '-'} {formatMoney(Math.abs(signed), tx.currency ?? 'VND')}
                </div>
              </div>
            )
          })}
          <Link to="/history" className="panel-footer-link">
            Xem tất cả
          </Link>
        </div>
      </div>
    </>
  )
}
