package project.smartbanking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Hạn mức giao dịch theo loại tiền, do admin chỉnh trên trang quản trị.
// Chưa có dòng nào cho một loại tiền thì dùng giá trị mặc định trong TransactionLimitService.
@Entity
@Table(name = "transaction_limits")
public class TransactionLimit {

    @Id
    @Column(name = "currency", length = 3)
    private String currency;

    // Tổng tiền ra (rút + chuyển + thanh toán) tối đa mỗi ngày trên một tài khoản
    @Column(name = "daily_out_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyOutLimit;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // id của admin chỉnh gần nhất
    @Column(name = "updated_by")
    private Long updatedBy;

    public TransactionLimit() {
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getDailyOutLimit() {
        return dailyOutLimit;
    }

    public void setDailyOutLimit(BigDecimal dailyOutLimit) {
        this.dailyOutLimit = dailyOutLimit;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
