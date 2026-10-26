import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { FiCheckCircle, FiAlertCircle, FiShield, FiUser, FiFileText, FiLock } from 'react-icons/fi'
import { changeMyPassword, fetchMyKyc, fetchMyProfile, submitMyKyc, updateMyProfile } from '../api/user'
import { useAuth } from '../context/AuthContext'
import { Field, InfoList, PageHeader } from '../components/ui/FormBits'
import { formatDateTime, getApiErrorMessage } from '../utils/adminDisplay'
import {
  GENDER_OPTIONS,
  ID_TYPE_OPTIONS,
  INCOME_OPTIONS,
  formatDate,
  idTypeLabel,
  initials,
  incomeLabel,
  kycBadgeClass,
  kycStatusLabel,
} from '../utils/profileDisplay'

// Giới hạn chọn ngày (ngày sinh, ngày cấp) không vượt quá hôm nay
const TODAY = new Date().toISOString().slice(0, 10)

const TABS = [
  { key: 'personal', label: 'Thông tin cá nhân', icon: FiUser },
  { key: 'kyc', label: 'Định danh (eKYC)', icon: FiFileText },
  { key: 'security', label: 'Bảo mật', icon: FiLock },
]

export function ProfilePage() {
  const navigate = useNavigate()
  const [tab, setTab] = useState('personal')
  const [profile, setProfile] = useState(null)
  const [kyc, setKyc] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    Promise.all([fetchMyProfile(), fetchMyKyc()])
      .then(([p, k]) => {
        if (cancelled) return
        setProfile(p)
        setKyc(k)
      })
      .catch((err) => toast.error(getApiErrorMessage(err, 'Không tải được hồ sơ')))
      .finally(() => !cancelled && setLoading(false))
    return () => {
      cancelled = true
    }
  }, [])

  if (loading) {
    return (
      <div className="page-card">
        <p style={{ color: 'var(--sb-text-muted)' }}>Đang tải hồ sơ...</p>
      </div>
    )
  }

  if (!profile) return null

  return (
    <div className="page-card">
      <PageHeader title="Hồ sơ của tôi" backTo={() => navigate(-1)} />

      <div className="profile-hero">
        <div className="profile-avatar">{initials(profile.fullName)}</div>
        <div className="profile-hero-body">
          <div className="profile-hero-name">{profile.fullName}</div>
          <div className="profile-hero-meta">
            @{profile.username} · Khách hàng từ {formatDate(profile.createdAt)}
          </div>
        </div>
        <span className={`status-badge ${kycBadgeClass(kyc?.kycStatus)}`}>
          {kycStatusLabel(kyc?.kycStatus)}
        </span>
      </div>

      {kyc?.kycStatus !== 'VERIFIED' && (
        <KycBanner status={kyc?.kycStatus} note={kyc?.kycNote} onOpen={() => setTab('kyc')} />
      )}

      <div className="profile-tabs" role="tablist">
        {TABS.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            type="button"
            role="tab"
            aria-selected={tab === key}
            className={`profile-tab${tab === key ? ' active' : ''}`}
            onClick={() => setTab(key)}
          >
            <Icon size={16} />
            {label}
          </button>
        ))}
      </div>

      {tab === 'personal' && <PersonalSection profile={profile} onSaved={setProfile} />}
      {tab === 'kyc' && <KycSection kyc={kyc} onSaved={setKyc} />}
      {tab === 'security' && <SecuritySection profile={profile} />}
    </div>
  )
}

// ===================== THÔNG TIN CÁ NHÂN =====================

function PersonalSection({ profile, onSaved }) {
  const { updateUser } = useAuth()
  const [form, setForm] = useState({
    fullName: profile.fullName ?? '',
    dateOfBirth: profile.dateOfBirth ?? '',
    gender: profile.gender ?? '',
  })
  const [saving, setSaving] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.fullName.trim()) {
      toast.error('Họ tên không được để trống')
      return
    }
    try {
      setSaving(true)
      const updated = await updateMyProfile({
        fullName: form.fullName.trim(),
        dateOfBirth: form.dateOfBirth || null,
        gender: form.gender || null,
        avatarUrl: profile.avatarUrl ?? null,
      })
      onSaved(updated)
      updateUser(updated)
      toast.success('Đã cập nhật thông tin cá nhân')
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="profile-section" onSubmit={handleSubmit}>
      <div className="profile-grid">
        <Field label="Họ và tên">
          <input
            className="form-input"
            value={form.fullName}
            maxLength={150}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
          />
        </Field>
        <Field label="Ngày sinh">
          <input
            type="date"
            className="form-input"
            value={form.dateOfBirth}
            max={TODAY}
            onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
          />
        </Field>
        <Field label="Giới tính">
          <select
            className="form-input"
            value={form.gender}
            onChange={(e) => setForm({ ...form, gender: e.target.value })}
          >
            <option value="">— Chọn —</option>
            {GENDER_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Tên đăng nhập">
          <input className="form-input" value={profile.username} disabled />
        </Field>
        <Field label="Email" hint={<VerifiedTag ok={profile.emailVerified} />}>
          <input className="form-input" value={profile.email ?? ''} disabled />
        </Field>
        <Field label="Số điện thoại" hint={<VerifiedTag ok={profile.phoneVerified} />}>
          <input className="form-input" value={profile.phone ?? ''} disabled />
        </Field>
      </div>
      <p className="profile-note">
        Email và số điện thoại dùng để nhận OTP nên không thể tự thay đổi. Vui lòng liên hệ ngân hàng nếu cần cập nhật.
      </p>
      <button type="submit" className="btn-primary profile-submit" disabled={saving}>
        {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
      </button>
    </form>
  )
}

// ===================== ĐỊNH DANH (eKYC) =====================

function KycSection({ kyc, onSaved }) {
  const status = kyc?.kycStatus ?? 'NOT_SUBMITTED'
  const [editing, setEditing] = useState(status === 'NOT_SUBMITTED' || status === 'REJECTED')

  if (!editing) {
    return (
      <div className="profile-section">
        <InfoList
          rows={[
            ['Loại giấy tờ', idTypeLabel(kyc.idType)],
            ['Số giấy tờ', kyc.idNumber],
            ['Ngày cấp', formatDate(kyc.idIssueDate)],
            ['Nơi cấp', kyc.idIssuePlace],
            ['Quốc tịch', kyc.nationality],
            ['Địa chỉ thường trú', kyc.permanentAddress],
            ['Địa chỉ hiện tại', kyc.currentAddress],
            ['Nghề nghiệp', kyc.occupation],
            ['Nơi làm việc', kyc.employer],
            ['Thu nhập hàng tháng', incomeLabel(kyc.monthlyIncome)],
            ['Ngày gửi hồ sơ', formatDateTime(kyc.submittedAt)],
            ['Ngày xác minh', formatDateTime(kyc.reviewedAt)],
          ]}
        />
        {status === 'PENDING' && (
          <button type="button" className="btn-primary profile-submit" onClick={() => setEditing(true)}>
            Sửa và gửi lại
          </button>
        )}
        {status === 'VERIFIED' && (
          <p className="profile-note">
            Hồ sơ đã được ngân hàng xác minh. Để thay đổi thông tin định danh, vui lòng đến quầy giao dịch gần nhất.
          </p>
        )}
      </div>
    )
  }

  return (
    <KycForm
      kyc={kyc}
      onCancel={status === 'PENDING' ? () => setEditing(false) : null}
      onSaved={(next) => {
        onSaved(next)
        setEditing(false)
      }}
    />
  )
}

function KycForm({ kyc, onCancel, onSaved }) {
  // Số giấy tờ trả về đã bị che nên luôn phải nhập lại khi gửi
  const [form, setForm] = useState({
    idType: kyc?.idType ?? 'CCCD',
    idNumber: '',
    idIssueDate: kyc?.idIssueDate ?? '',
    idIssuePlace: kyc?.idIssuePlace ?? '',
    nationality: kyc?.nationality ?? 'Việt Nam',
    permanentAddress: kyc?.permanentAddress ?? '',
    currentAddress: kyc?.currentAddress ?? '',
    occupation: kyc?.occupation ?? '',
    employer: kyc?.employer ?? '',
    monthlyIncome: kyc?.monthlyIncome ?? '',
  })
  const [sameAddress, setSameAddress] = useState(
    !kyc?.currentAddress || kyc.currentAddress === kyc.permanentAddress,
  )
  const [agreed, setAgreed] = useState(false)
  const [saving, setSaving] = useState(false)

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })
  const idHint = ID_TYPE_OPTIONS.find((o) => o.value === form.idType)?.hint

  async function handleSubmit(e) {
    e.preventDefault()
    const error = validateKyc(form)
    if (error) {
      toast.error(error)
      return
    }
    if (!agreed) {
      toast.error('Vui lòng xác nhận thông tin khai báo là chính xác')
      return
    }
    try {
      setSaving(true)
      const saved = await submitMyKyc({
        ...form,
        idNumber: form.idNumber.replace(/\s+/g, '').toUpperCase(),
        currentAddress: sameAddress ? form.permanentAddress : form.currentAddress,
        monthlyIncome: form.monthlyIncome || null,
      })
      onSaved(saved)
      toast.success('Đã gửi hồ sơ, ngân hàng sẽ xác minh trong thời gian sớm nhất')
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="profile-section" onSubmit={handleSubmit}>
      <h3 className="profile-subtitle">Giấy tờ tùy thân</h3>
      <div className="profile-grid">
        <Field label="Loại giấy tờ *">
          <select className="form-input" value={form.idType} onChange={set('idType')}>
            {ID_TYPE_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Số giấy tờ *" hint={idHint}>
          <input
            className="form-input"
            value={form.idNumber}
            maxLength={20}
            autoComplete="off"
            placeholder={kyc?.idNumber ? `Nhập lại (hiện tại: ${kyc.idNumber})` : ''}
            onChange={set('idNumber')}
          />
        </Field>
        <Field label="Ngày cấp *">
          <input
            type="date"
            className="form-input"
            value={form.idIssueDate}
            max={TODAY}
            onChange={set('idIssueDate')}
          />
        </Field>
        <Field label="Nơi cấp *">
          <input
            className="form-input"
            value={form.idIssuePlace}
            maxLength={150}
            placeholder="VD: Cục Cảnh sát QLHC về TTXH"
            onChange={set('idIssuePlace')}
          />
        </Field>
        <Field label="Quốc tịch *">
          <input className="form-input" value={form.nationality} maxLength={60} onChange={set('nationality')} />
        </Field>
      </div>

      <h3 className="profile-subtitle">Địa chỉ</h3>
      <div className="form-stack" style={{ maxWidth: 'none', gap: '1rem' }}>
        <Field label="Địa chỉ thường trú *">
          <input
            className="form-input"
            value={form.permanentAddress}
            maxLength={255}
            placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
            onChange={set('permanentAddress')}
          />
        </Field>
        <label className="login-remember">
          <input type="checkbox" checked={sameAddress} onChange={(e) => setSameAddress(e.target.checked)} />
          Địa chỉ hiện tại giống địa chỉ thường trú
        </label>
        {!sameAddress && (
          <Field label="Địa chỉ hiện tại">
            <input className="form-input" value={form.currentAddress} maxLength={255} onChange={set('currentAddress')} />
          </Field>
        )}
      </div>

      <h3 className="profile-subtitle">Nghề nghiệp</h3>
      <div className="profile-grid">
        <Field label="Nghề nghiệp">
          <input className="form-input" value={form.occupation} maxLength={100} onChange={set('occupation')} />
        </Field>
        <Field label="Nơi làm việc">
          <input className="form-input" value={form.employer} maxLength={150} onChange={set('employer')} />
        </Field>
        <Field label="Thu nhập hàng tháng">
          <select className="form-input" value={form.monthlyIncome} onChange={set('monthlyIncome')}>
            <option value="">— Chọn —</option>
            {INCOME_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </Field>
      </div>

      <label className="login-remember" style={{ marginTop: '1.25rem' }}>
        <input type="checkbox" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} />
        Tôi cam kết thông tin khai báo là chính xác và đồng ý cho ngân hàng sử dụng để xác minh danh tính.
      </label>

      <div className="profile-actions">
        {onCancel && (
          <button type="button" className="btn-secondary" onClick={onCancel}>
            Hủy
          </button>
        )}
        <button type="submit" className="btn-primary profile-submit" disabled={saving}>
          {saving ? 'Đang gửi...' : 'Gửi hồ sơ xác minh'}
        </button>
      </div>
    </form>
  )
}

function validateKyc(form) {
  const id = form.idNumber.replace(/\s+/g, '').toUpperCase()
  if (!id) return 'Vui lòng nhập số giấy tờ'
  if (form.idType === 'CCCD' && !/^\d{12}$/.test(id)) return 'Số CCCD phải gồm 12 chữ số'
  if (form.idType === 'CMND' && !/^\d{9}$/.test(id)) return 'Số CMND phải gồm 9 chữ số'
  if (form.idType === 'PASSPORT' && !/^[A-Z]\d{7}$/.test(id)) return 'Số hộ chiếu gồm 1 chữ cái và 7 chữ số'
  if (!form.idIssueDate) return 'Vui lòng nhập ngày cấp'
  if (!form.idIssuePlace.trim()) return 'Vui lòng nhập nơi cấp'
  if (!form.nationality.trim()) return 'Vui lòng nhập quốc tịch'
  if (!form.permanentAddress.trim()) return 'Vui lòng nhập địa chỉ thường trú'
  return null
}

// ===================== BẢO MẬT =====================

function SecuritySection({ profile }) {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [saving, setSaving] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.currentPassword || !form.newPassword) {
      toast.error('Vui lòng nhập đầy đủ mật khẩu')
      return
    }
    if (form.newPassword.length < 8 || !/[A-Za-z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) {
      toast.error('Mật khẩu mới từ 8 ký tự, gồm cả chữ và số')
      return
    }
    if (form.newPassword !== form.confirmPassword) {
      toast.error('Mật khẩu xác nhận không khớp')
      return
    }
    try {
      setSaving(true)
      const res = await changeMyPassword({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      })
      toast.success(res?.message ?? 'Đổi mật khẩu thành công')
      logout()
      navigate('/login')
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="profile-section">
      <InfoList
        rows={[
          ['Lần đăng nhập gần nhất', formatDateTime(profile.lastLoginAt)],
          ['Xác thực email', profile.emailVerified ? 'Đã xác thực' : 'Chưa xác thực'],
          ['Xác thực số điện thoại', profile.phoneVerified ? 'Đã xác thực' : 'Chưa xác thực'],
        ]}
      />

      <h3 className="profile-subtitle">Đổi mật khẩu</h3>
      <form className="form-stack" onSubmit={handleSubmit} style={{ maxWidth: 420 }}>
        <Field label="Mật khẩu hiện tại">
          <input
            type="password"
            className="form-input"
            autoComplete="current-password"
            value={form.currentPassword}
            onChange={(e) => setForm({ ...form, currentPassword: e.target.value })}
          />
        </Field>
        <Field label="Mật khẩu mới" hint="Tối thiểu 8 ký tự, gồm cả chữ và số">
          <input
            type="password"
            className="form-input"
            autoComplete="new-password"
            value={form.newPassword}
            onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
          />
        </Field>
        <Field label="Nhập lại mật khẩu mới">
          <input
            type="password"
            className="form-input"
            autoComplete="new-password"
            value={form.confirmPassword}
            onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
          />
        </Field>
        <p className="profile-note" style={{ margin: 0 }}>
          Sau khi đổi mật khẩu, bạn sẽ bị đăng xuất khỏi tất cả thiết bị.
        </p>
        <button type="submit" className="btn-primary" disabled={saving}>
          {saving ? 'Đang xử lý...' : 'Đổi mật khẩu'}
        </button>
      </form>
    </div>
  )
}

// ===================== THÀNH PHẦN PHỤ =====================

function KycBanner({ status, note, onOpen }) {
  const content = {
    NOT_SUBMITTED: {
      icon: FiShield,
      text: 'Bạn chưa khai báo thông tin định danh. Hoàn tất eKYC để sử dụng đầy đủ dịch vụ và tăng bảo mật tài khoản.',
      action: 'Khai báo ngay',
    },
    PENDING: {
      icon: FiAlertCircle,
      text: 'Hồ sơ định danh đang chờ ngân hàng xác minh.',
    },
    REJECTED: {
      icon: FiAlertCircle,
      text: `Hồ sơ định danh chưa được chấp nhận${note ? `: ${note}` : ''}.`,
      action: 'Cập nhật hồ sơ',
    },
  }[status ?? 'NOT_SUBMITTED']
  if (!content) return null
  const Icon = content.icon
  return (
    <div className={`profile-banner ${status === 'REJECTED' ? 'danger' : ''}`}>
      <Icon size={18} />
      <span>{content.text}</span>
      {content.action && (
        <button type="button" className="back-link" onClick={onOpen}>
          {content.action}
        </button>
      )}
    </div>
  )
}

function VerifiedTag({ ok }) {
  return ok ? (
    <span className="verified-tag ok">
      <FiCheckCircle size={12} /> Đã xác thực
    </span>
  ) : (
    <span className="verified-tag">Chưa xác thực</span>
  )
}

