import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import { UserLayout } from './components/layout/UserLayout'
import { LoginPage, AdminLoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { DashboardPage } from './pages/DashboardPage'
import { AccountsPage } from './pages/AccountsPage'
import { TransferPage } from './pages/TransferPage'
import { DepositPage } from './pages/DepositPage'
import { WithdrawPage } from './pages/WithdrawPage'
import { BillPaymentPage } from './pages/BillPaymentPage'
import { TransactionHistoryPage } from './pages/TransactionHistoryPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { SettingsPage } from './pages/SettingsPage'
import { ProfilePage } from './pages/ProfilePage'
import {
  AdminAccountsPage,
  AdminAuditLogsPage,
  AdminDashboardPage,
  AdminLayout,
  AdminStatsPage,
  AdminTransactionsPage,
  AdminUserDetailPage,
  AdminUsersPage,
} from './pages/admin/AdminPages'
import './styles/smartbanking.css'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/admin/login" element={<AdminLoginPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<UserLayout />}>
              <Route index element={<DashboardPage />} />
              <Route path="accounts" element={<AccountsPage />} />
              <Route path="transfer" element={<TransferPage />} />
              <Route path="deposit" element={<DepositPage />} />
              <Route path="withdraw" element={<WithdrawPage />} />
              <Route path="bills" element={<BillPaymentPage />} />
              <Route path="history" element={<TransactionHistoryPage />} />
              <Route path="notifications" element={<NotificationsPage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="settings" element={<SettingsPage />} />
            </Route>
          </Route>

          <Route element={<ProtectedRoute admin />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<AdminDashboardPage />} />
              <Route path="users" element={<AdminUsersPage />} />
              <Route path="users/:id" element={<AdminUserDetailPage />} />
              <Route path="accounts" element={<AdminAccountsPage />} />
              <Route path="transactions" element={<AdminTransactionsPage />} />
              <Route path="stats" element={<AdminStatsPage />} />
              <Route path="settings" element={<AdminAuditLogsPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
      <Toaster position="top-right" />
    </AuthProvider>
  )
}

export default App
