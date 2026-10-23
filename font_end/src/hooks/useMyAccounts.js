import { useCallback, useEffect, useState } from 'react'
import { fetchMyAccounts } from '../api/accounts'
import { mapAccountFromApi } from '../utils/accountDisplay'
import { getApiErrorMessage } from '../utils/adminDisplay'

export function useMyAccounts({ currency = 'VND' } = {}) {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchMyAccounts()
      const mapped = Array.isArray(data) ? data.map(mapAccountFromApi) : []
      setAccounts(currency ? mapped.filter((a) => a.currency === currency) : mapped)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không tải được tài khoản'))
      setAccounts([])
    } finally {
      setLoading(false)
    }
  }, [currency])

  useEffect(() => {
    reload()
  }, [reload])

  return { accounts, loading, error, reload }
}
