import { useCallback, useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { fetchAdminAuditLogs } from '../../api/admin'
import { PageHeader } from '../../components/ui/FormBits'
import { formatDateTime, getApiErrorMessage } from '../../utils/adminDisplay'
import { AdminPagination } from './AdminPagination'

export function AdminAuditLogsPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      const result = await fetchAdminAuditLogs({ page, size: 20 })
      setData(result)
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Không tải được nhật ký'))
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="page-card">
      <PageHeader title="Nhật ký hệ thống" />
      {loading && <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải...</p>}

      {!loading && (
        <>
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>User ID</th>
                  <th>Hành động</th>
                  <th>Mô tả</th>
                  <th>HTTP</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={5} style={{ color: 'var(--sb-text-muted)' }}>
                      Không có dữ liệu
                    </td>
                  </tr>
                )}
                {data.content.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDateTime(log.createdAt)}</td>
                    <td>{log.userId ?? '—'}</td>
                    <td>{log.action}</td>
                    <td>{log.description ?? log.requestUri ?? '—'}</td>
                    <td>
                      {log.requestMethod} {log.responseStatus ?? ''}
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
