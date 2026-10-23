import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { FiCalendar, FiDownload, FiFilter, FiSearch } from 'react-icons/fi'
import toast from 'react-hot-toast'
import { exportTransactionHistory, fetchTransactionHistory } from '../api/transactions'
import { formatMoney } from '../data/mockData'
import { PageHeader } from '../components/ui/FormBits'
import {
  formatDateTime,
  formatTransactionDescription,
  getApiErrorMessage,
  statusBadgeClass,
  transactionStatusLabel,
  transactionTypeLabel,
} from '../utils/adminDisplay'
import { TX_TYPE_FILTER_OPTIONS, transactionSignedAmount } from '../utils/userDisplay'
import { AdminPagination } from './admin/AdminPagination'

export function TransactionHistoryPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [type, setType] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState('')

  const load = useCallback(async () => {
    try {
      setLoading(true)
      const result = await fetchTransactionHistory({
        type: type || undefined,
        from: from || undefined,
        to: to || undefined,
        keyword: search || undefined,
        page,
        size: 10,
      })
      setData(result)
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không tải được lịch sử giao dịch'))
    } finally {
      setLoading(false)
    }
  }, [from, page, search, to, type])

  useEffect(() => {
    load()
  }, [load])

  const applyFilters = () => {
    setPage(0)
    setSearch(keyword.trim())
  }

  const handleExport = async () => {
    try {
      const blob = await exportTransactionHistory({
        type: type || undefined,
        from: from || undefined,
        to: to || undefined,
        keyword: search || undefined,
      })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'lich-su-giao-dich.csv'
      a.click()
      URL.revokeObjectURL(url)
      toast.success('Đã tải file CSV')
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không xuất được file'))
    }
  }

  return (
    <div className="page-card">
      <PageHeader title="Lịch sử giao dịch" backTo={() => navigate(-1)} />

      <div className="filters-row">
        <label className="filter-select" style={{ cursor: 'pointer' }}>
          <FiFilter />
          <select
            value={type}
            onChange={(e) => {
              setType(e.target.value)
              setPage(0)
            }}
            style={{ border: 'none', background: 'transparent', flex: 1 }}
            aria-label="Loại giao dịch"
          >
            {TX_TYPE_FILTER_OPTIONS.map((opt) => (
              <option key={opt.value || 'all'} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </label>
        <label className="filter-date" style={{ cursor: 'pointer', gap: '0.35rem' }}>
          <FiCalendar />
          <input
            type="date"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value)
              setPage(0)
            }}
            aria-label="Từ ngày"
          />
          <span>—</span>
          <input
            type="date"
            value={to}
            onChange={(e) => {
              setTo(e.target.value)
              setPage(0)
            }}
            aria-label="Đến ngày"
          />
        </label>
      </div>

      <form
        className="search-full"
        style={{ position: 'relative', display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}
        onSubmit={(e) => {
          e.preventDefault()
          applyFilters()
        }}
      >
        <div style={{ position: 'relative', flex: 1 }}>
          <FiSearch
            style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }}
          />
          <input
            type="search"
            placeholder="Tìm kiếm mã giao dịch, nội dung..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            style={{ paddingLeft: '2.25rem', width: '100%' }}
          />
        </div>
        <button type="submit" className="btn-primary" style={{ maxWidth: 100 }}>
          Tìm
        </button>
        <button type="button" className="page-btn" onClick={handleExport} title="Xuất CSV">
          <FiDownload />
        </button>
      </form>

      {loading && <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải...</p>}

      {!loading && (
        <>
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Loại giao dịch</th>
                  <th>Nội dung</th>
                  <th>Số tiền</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={5} style={{ color: 'var(--sb-text-muted)' }}>
                      Không có giao dịch
                    </td>
                  </tr>
                )}
                {data.content.map((row) => {
                  const signed = transactionSignedAmount(row)
                  return (
                    <tr key={row.id}>
                      <td>{formatDateTime(row.createdAt)}</td>
                      <td>{transactionTypeLabel(row.transactionType)}</td>
                      <td
                        title={formatTransactionDescription(row.description)}
                        style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                      >
                        {formatTransactionDescription(row.description)}
                      </td>
                      <td className={`tx-amount ${signed >= 0 ? 'positive' : 'negative'}`}>
                        {signed >= 0 ? '+' : '-'} {formatMoney(Math.abs(signed), row.currency ?? 'VND')}
                      </td>
                      <td>
                        <span className={`status-badge ${statusBadgeClass(row.status)}`}>
                          {transactionStatusLabel(row.status)}
                        </span>
                      </td>
                    </tr>
                  )
                })}
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
