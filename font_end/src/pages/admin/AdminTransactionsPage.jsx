import { useCallback, useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { fetchAdminTransactions } from '../../api/admin'
import { formatMoney } from '../../data/mockData'
import { PageHeader } from '../../components/ui/FormBits'
import {
  formatDateTime,
  formatTransactionDescription,
  getApiErrorMessage,
  statusBadgeClass,
  transactionStatusLabel,
  transactionTypeLabel,
  txParticipantLabel,
} from '../../utils/adminDisplay'
import { AdminPagination } from './AdminPagination'

export function AdminTransactionsPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState('')

  const load = useCallback(async () => {
    try {
      setLoading(true)
      const result = await fetchAdminTransactions({ keyword: search || undefined, page, size: 15 })
      setData(result)
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không tải được giao dịch'))
    } finally {
      setLoading(false)
    }
  }, [page, search])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="page-card">
      <PageHeader title="Quản lý giao dịch" />
      <form
        style={{ marginBottom: '1rem', maxWidth: 480, display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}
        onSubmit={(e) => {
          e.preventDefault()
          setPage(0)
          setSearch(keyword.trim())
        }}
      >
        <input
          placeholder="Mã GD, mô tả, số TK..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
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
                  <th>Thời gian</th>
                  <th>Mã GD</th>
                  <th>Loại</th>
                  <th>Người dùng</th>
                  <th>Nội dung</th>
                  <th>Số tiền</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={7} style={{ color: 'var(--sb-text-muted)' }}>
                      Không có dữ liệu
                    </td>
                  </tr>
                )}
                {data.content.map((tx) => (
                  <tr key={tx.id}>
                    <td>{formatDateTime(tx.createdAt)}</td>
                    <td>{tx.transactionCode}</td>
                    <td>{transactionTypeLabel(tx.transactionType)}</td>
                    <td>{txParticipantLabel(tx)}</td>
                    <td
                      title={formatTransactionDescription(tx.description)}
                      style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    >
                      {formatTransactionDescription(tx.description)}
                    </td>
                    <td>{formatMoney(Number(tx.amount), tx.currency ?? 'VND')}</td>
                    <td>
                      <span className={`status-badge ${statusBadgeClass(tx.status)}`}>
                        {transactionStatusLabel(tx.status)}
                      </span>
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
