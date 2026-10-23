import { useCallback, useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { fetchAdminAccounts, lockAdminAccount, unlockAdminAccount } from '../../api/admin'
import { formatBalance } from '../../data/mockData'
import { PageHeader } from '../../components/ui/FormBits'
import { formatAccountNumber } from '../../utils/accountDisplay'
import { accountStatusLabel, getApiErrorMessage, statusBadgeClass } from '../../utils/adminDisplay'
import { AdminPagination } from './AdminPagination'

export function AdminAccountsPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState('')

  const load = useCallback(async () => {
    try {
      setLoading(true)
      const result = await fetchAdminAccounts({ keyword: search || undefined, page, size: 15 })
      setData(result)
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không tải được danh sách tài khoản'))
    } finally {
      setLoading(false)
    }
  }, [page, search])

  useEffect(() => {
    load()
  }, [load])

  async function toggleLock(account) {
    try {
      if (account.status === 'LOCKED') {
        await unlockAdminAccount(account.id)
        toast.success('Đã mở khóa tài khoản')
      } else {
        await lockAdminAccount(account.id)
        toast.success('Đã khóa tài khoản')
      }
      load()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  return (
    <div className="page-card">
      <PageHeader title="Quản lý tài khoản" />
      <form
        style={{ marginBottom: '1rem', maxWidth: 480, display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}
        onSubmit={(e) => {
          e.preventDefault()
          setPage(0)
          setSearch(keyword.trim())
        }}
      >
        <input
          placeholder="Số TK, tên TK, chủ tài khoản..."
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
                  <th>Số tài khoản</th>
                  <th>Tên tài khoản</th>
                  <th>Chủ TK</th>
                  <th>Số dư</th>
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
                {data.content.map((acc) => (
                  <tr key={acc.id}>
                    <td>{formatAccountNumber(acc.accountNumber)}</td>
                    <td>{acc.accountName}</td>
                    <td>{acc.ownerFullName || acc.ownerUsername || '—'}</td>
                    <td>{formatBalance(Number(acc.balance), acc.currency)}</td>
                    <td>
                      <span className={`status-badge ${statusBadgeClass(acc.status)}`}>
                        {accountStatusLabel(acc.status)}
                      </span>
                    </td>
                    <td>
                      {acc.status !== 'CLOSED' && (
                        <button type="button" className="back-link" onClick={() => toggleLock(acc)}>
                          {acc.status === 'LOCKED' ? 'Mở khóa' : 'Khóa'}
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
