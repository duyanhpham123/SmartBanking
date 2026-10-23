import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FiFileText, FiMoreHorizontal, FiPlusCircle, FiMinusCircle, FiRepeat } from 'react-icons/fi'
import toast from 'react-hot-toast'
import { fetchMyAccounts } from '../api/accounts'
import { formatBalance } from '../data/mockData'
import { PageHeader } from '../components/ui/FormBits'
import { mapAccountFromApi } from '../utils/accountDisplay'

export function AccountsPage() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        setLoading(true)
        const data = await fetchMyAccounts()
        if (!cancelled) {
          setAccounts(Array.isArray(data) ? data.map(mapAccountFromApi) : [])
        }
      } catch (err) {
        if (!cancelled) {
          const message =
            err.response?.data?.message || err.message || 'Không tải được danh sách tài khoản'
          toast.error(message)
          setAccounts([])
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="page-card">
      <PageHeader title="Tài khoản của tôi" />
      {loading && <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải tài khoản...</p>}
      {!loading && accounts.length === 0 && (
        <p style={{ color: 'var(--sb-text-muted)' }}>Bạn chưa có tài khoản nào.</p>
      )}
      <div className="account-cards">
        {accounts.map((acc) => (
          <article key={acc.id} className="bank-card">
            <div className="bank-card-top" style={{ background: acc.color }}>
              <span className="bank-card-badge">{acc.status}</span>
              <div className="bank-card-type">{acc.label}</div>
              <div className="bank-card-number">{acc.number}</div>
              <div className="bank-card-balance-label">Số dư</div>
              <div className="bank-card-balance">{formatBalance(acc.balance, acc.currency)}</div>
              <button type="button" className="back-link" style={{ position: 'absolute', bottom: '1rem', right: '1rem', color: '#fff' }} aria-label="Tùy chọn">
                <FiMoreHorizontal size={20} />
              </button>
            </div>
            <div className="bank-card-actions">
              <Link to="/transfer" className="bank-card-action">
                <FiRepeat /> Chuyển tiền
              </Link>
              <Link to="/deposit" className="bank-card-action">
                <FiPlusCircle /> Nạp tiền
              </Link>
              <Link to="/withdraw" className="bank-card-action">
                <FiMinusCircle /> Rút tiền
              </Link>
              <Link to="/bills" className="bank-card-action">
                <FiFileText /> Hóa đơn
              </Link>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}
