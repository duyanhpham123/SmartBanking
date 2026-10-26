package project.smartbanking.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import project.smartbanking.model.enums.IdDocumentType;
import project.smartbanking.model.enums.IncomeRange;
import project.smartbanking.model.enums.KycStatus;

// Hồ sơ định danh khách hàng (KYC): giấy tờ tùy thân, địa chỉ, nghề nghiệp.
// Tách khỏi bảng users để dữ liệu nhạy cảm không bị trả kèm mỗi lần đọc User.
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile {

	// ==================== CỘT BẢNG ====================

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	// users.id trong database là BIGINT UNSIGNED, khóa ngoại phải cùng kiểu
	@JoinColumn(name = "user_id", nullable = false, unique = true, columnDefinition = "BIGINT UNSIGNED")
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "id_type", length = 20)
	private IdDocumentType idType;

	@Column(name = "id_number", unique = true, length = 20)
	private String idNumber;

	@Column(name = "id_issue_date")
	private LocalDate idIssueDate;

	@Column(name = "id_issue_place", length = 150)
	private String idIssuePlace;

	@Column(length = 60)
	private String nationality;

	@Column(name = "permanent_address", length = 255)
	private String permanentAddress;

	@Column(name = "current_address", length = 255)
	private String currentAddress;

	@Column(length = 100)
	private String occupation;

	@Column(length = 150)
	private String employer;

	@Enumerated(EnumType.STRING)
	@Column(name = "monthly_income", length = 30)
	private IncomeRange monthlyIncome;

	@Enumerated(EnumType.STRING)
	@Column(name = "kyc_status", nullable = false, length = 20)
	private KycStatus kycStatus = KycStatus.NOT_SUBMITTED;

	// Lý do từ chối / ghi chú của nhân viên xét duyệt
	@Column(name = "kyc_note", length = 500)
	private String kycNote;

	@Column(name = "submitted_at")
	private LocalDateTime submittedAt;

	@Column(name = "reviewed_at")
	private LocalDateTime reviewedAt;

	@Column(name = "reviewed_by")
	private Long reviewedBy;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public CustomerProfile() {
		super();
	}

	public CustomerProfile(User user) {
		super();
		this.user = user;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public IdDocumentType getIdType() {
		return idType;
	}

	public void setIdType(IdDocumentType idType) {
		this.idType = idType;
	}

	public String getIdNumber() {
		return idNumber;
	}

	public void setIdNumber(String idNumber) {
		this.idNumber = idNumber;
	}

	public LocalDate getIdIssueDate() {
		return idIssueDate;
	}

	public void setIdIssueDate(LocalDate idIssueDate) {
		this.idIssueDate = idIssueDate;
	}

	public String getIdIssuePlace() {
		return idIssuePlace;
	}

	public void setIdIssuePlace(String idIssuePlace) {
		this.idIssuePlace = idIssuePlace;
	}

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getPermanentAddress() {
		return permanentAddress;
	}

	public void setPermanentAddress(String permanentAddress) {
		this.permanentAddress = permanentAddress;
	}

	public String getCurrentAddress() {
		return currentAddress;
	}

	public void setCurrentAddress(String currentAddress) {
		this.currentAddress = currentAddress;
	}

	public String getOccupation() {
		return occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	public String getEmployer() {
		return employer;
	}

	public void setEmployer(String employer) {
		this.employer = employer;
	}

	public IncomeRange getMonthlyIncome() {
		return monthlyIncome;
	}

	public void setMonthlyIncome(IncomeRange monthlyIncome) {
		this.monthlyIncome = monthlyIncome;
	}

	public KycStatus getKycStatus() {
		return kycStatus;
	}

	public void setKycStatus(KycStatus kycStatus) {
		this.kycStatus = kycStatus;
	}

	public String getKycNote() {
		return kycNote;
	}

	public void setKycNote(String kycNote) {
		this.kycNote = kycNote;
	}

	public LocalDateTime getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(LocalDateTime submittedAt) {
		this.submittedAt = submittedAt;
	}

	public LocalDateTime getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(LocalDateTime reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public Long getReviewedBy() {
		return reviewedBy;
	}

	public void setReviewedBy(Long reviewedBy) {
		this.reviewedBy = reviewedBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
