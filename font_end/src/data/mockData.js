export const USER = {
  name: 'Nguyễn Văn A',
  role: 'USER',
  avatar: 'https://i.pravatar.cc/100?img=12',
}

export const ACCOUNTS = [
  {
    id: '1',
    number: '1234 5678 9012 3456',
    label: 'Tài khoản thanh toán',
    balance: 15680000,
    currency: 'VND',
    type: 'payment',
    color: '#2563eb',
    status: 'Active',
  },
  {
    id: '2',
    number: '9876 5432 1096 7654',
    label: 'Tài khoản tiết kiệm',
    balance: 8000000,
    currency: 'VND',
    type: 'savings',
    color: '#0d9488',
    status: 'Active',
  },
  {
    id: '3',
    number: '1111 2222 3333 4444',
    label: 'Tài khoản ngoại tệ (USD)',
    balance: 2000,
    currency: 'USD',
    type: 'foreign',
    color: '#7c3aed',
    status: 'Active',
  },
]

export const RECENT_TRANSACTIONS = [
  {
    id: 't1',
    type: 'Chuyển khoản',
    date: '28/09/2025 10:24',
    amount: -2000000,
    status: 'Thành công',
    user: 'user123',
  },
  {
    id: 't2',
    type: 'Nạp tiền',
    date: '27/09/2025 15:32',
    amount: 5000000,
    status: 'Thành công',
    user: 'user456',
  },
  {
    id: 't3',
    type: 'Rút tiền',
    date: '26/09/2025 09:15',
    amount: -1000000,
    status: 'Thành công',
    user: 'user789',
  },
  {
    id: 't4',
    type: 'Chuyển khoản',
    date: '25/09/2025 14:20',
    amount: -3500000,
    status: 'Thành công',
    user: 'user123',
  },
]

export const TRANSACTION_HISTORY = [
  { id: 'h1', time: '28/08/2025 10:24', type: 'Chuyển khoản', amount: -2000000, status: 'Thành công' },
  { id: 'h2', time: '27/08/2025 15:32', type: 'Nạp tiền', amount: 5000000, status: 'Thành công' },
  { id: 'h3', time: '26/08/2025 09:15', type: 'Rút tiền', amount: -1000000, status: 'Thành công' },
  { id: 'h4', time: '25/08/2025 14:20', type: 'Thanh toán hóa đơn', amount: -850000, status: 'Thành công' },
  { id: 'h5', time: '24/08/2025 11:00', type: 'Chuyển khoản', amount: -3000000, status: 'Thành công' },
  { id: 'h6', time: '23/08/2025 16:45', type: 'Nạp tiền', amount: 2000000, status: 'Thành công' },
]

export const CHART_7_DAYS = [
  { date: '22/09', value: 8 },
  { date: '23/09', value: 10 },
  { date: '24/09', value: 7 },
  { date: '25/09', value: 12 },
  { date: '26/09', value: 9 },
  { date: '27/09', value: 11 },
  { date: '28/09', value: 15 },
]

export const NOTIFICATIONS = [
  {
    id: 'n1',
    title: 'Giao dịch thành công',
    message: 'Bạn vừa chuyển 2,000,000 VND đến tài khoản...',
    time: '10:34 - 28/08/2023',
    variant: 'success',
  },
  {
    id: 'n2',
    title: 'Thông báo bảo mật',
    message: 'Mã OTP của bạn là 123456. Vui lòng không...',
    time: '15:32 - 27/08/2023',
    variant: 'security',
  },
  {
    id: 'n3',
    title: 'Giao dịch thành công',
    message: 'Bạn vừa nạp 5,000,000 VND vào tài khoản...',
    time: '18:32 - 27/08/2023',
    variant: 'deposit',
  },
  {
    id: 'n4',
    title: 'Ưu đãi đặc biệt',
    message: 'Giảm 50% phí chuyển tiền trong tháng 9!',
    time: '12:00 - 25/08/2023',
    variant: 'promo',
  },
]

export const ADMIN_STATS = {
  users: 1248,
  accounts: 2356,
  transactions: 15892,
  totalAmount: 256780000,
}

export const ADMIN_TX_BY_TYPE = [
  { name: 'Chuyển khoản', value: 45, color: '#2563eb' },
  { name: 'Nạp tiền', value: 20, color: '#38bdf8' },
  { name: 'Rút tiền', value: 15, color: '#22c55e' },
  { name: 'Thanh toán hóa đơn', value: 10, color: '#a855f7' },
  { name: 'Khác', value: 10, color: '#f97316' },
]

export const ADMIN_RECENT_TX = [
  { id: 'a1', time: '28/08/2025 10:24', user: 'user123', type: 'Chuyển khoản', amount: -2000000, status: 'Thành công' },
  { id: 'a2', time: '27/08/2025 15:32', user: 'user456', type: 'Nạp tiền', amount: 5000000, status: 'Thành công' },
  { id: 'a3', time: '26/08/2025 09:15', user: 'user789', type: 'Rút tiền', amount: -1000000, status: 'Thất bại' },
  { id: 'a4', time: '25/08/2025 14:20', user: 'user321', type: 'Chuyển khoản', amount: -3500000, status: 'Thành công' },
]

export function formatMoney(amount, currency = 'VND') {
  if (currency === 'USD') {
    return `${amount.toLocaleString('en-US')} USD`
  }
  return `${Math.abs(amount).toLocaleString('vi-VN')} VND`
}

export function formatBalance(amount, currency = 'VND') {
  if (currency === 'USD') {
    return `${amount.toLocaleString('en-US')} USD`
  }
  return `${amount.toLocaleString('vi-VN')} VND`
}
