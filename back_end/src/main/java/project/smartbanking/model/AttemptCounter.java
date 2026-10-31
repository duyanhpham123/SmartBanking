package project.smartbanking.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Bộ đếm số lần thử sai (đăng nhập, OTP). Lưu trong database nên restart ứng dụng
// không làm mất bộ đếm, và nhiều server chạy song song dùng chung một bộ đếm.
@Entity
@Table(name = "attempt_counters")
public class AttemptCounter {

    // SHA-256 của "<loại>:<định danh>", không lưu thẳng email/SĐT người dùng nhập vào
    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "failures", nullable = false)
    private int failures;

    @Column(name = "window_start")
    private LocalDateTime windowStart;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AttemptCounter() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
