import { useEffect, useMemo, useState } from 'react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import toast from 'react-hot-toast'
import { fetchAdminChart } from '../../api/admin'
import { formatMoney } from '../../data/mockData'
import { PageHeader } from '../../components/ui/FormBits'
import { formatChartDate, getApiErrorMessage } from '../../utils/adminDisplay'

export function AdminStatsPage() {
  const [days, setDays] = useState(7)
  const [chart, setChart] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        setLoading(true)
        const data = await fetchAdminChart(days, 'VND')
        if (!cancelled) setChart(data)
      } catch (err) {
        if (!cancelled) toast.error(getApiErrorMessage(err, 'Không tải được biểu đồ'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [days])

  const points = useMemo(() => {
    if (!chart?.points) return []
    return chart.points.map((p) => ({
      date: formatChartDate(p.date),
      count: p.count,
      total: Number(p.totalAmount ?? 0),
    }))
  }, [chart])

  return (
    <div className="page-card">
      <PageHeader title="Thống kê giao dịch" />
      <div style={{ marginBottom: '1rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
        {[7, 14, 30].map((d) => (
          <button
            key={d}
            type="button"
            className={`page-btn${days === d ? ' active' : ''}`}
            onClick={() => setDays(d)}
          >
            {d} ngày
          </button>
        ))}
      </div>

      {loading && <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải biểu đồ...</p>}

      {!loading && (
        <div className="panel">
          <h3>Biểu đồ giao dịch toàn hệ thống ({chart?.currency ?? 'VND'})</h3>
          <ResponsiveContainer width="100%" height={320}>
            <AreaChart data={points}>
              <defs>
                <linearGradient id="adminChartFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#2563eb" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#2563eb" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
              <YAxis
                tickFormatter={(v) => `${(v / 1_000_000).toFixed(0)}M`}
                tick={{ fontSize: 12 }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                formatter={(v, name) => {
                  if (name === 'total') return [formatMoney(Number(v)), 'Tổng tiền']
                  return [v, 'Số giao dịch']
                }}
              />
              <Area
                type="monotone"
                dataKey="total"
                stroke="#2563eb"
                strokeWidth={2}
                fill="url(#adminChartFill)"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
