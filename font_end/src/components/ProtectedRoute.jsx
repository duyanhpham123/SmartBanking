import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function ProtectedRoute({ admin = false }) {
  const { isAuthenticated, isAdmin } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to={admin ? '/admin/login' : '/login'} state={{ from: location }} replace />
  }

  if (admin && !isAdmin) {
    return <Navigate to="/" replace />
  }

  if (!admin && isAdmin) {
    return <Navigate to="/admin" replace />
  }

  return <Outlet />
}
