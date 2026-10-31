package project.smartbanking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import project.smartbanking.model.AttemptCounter;

class AttemptLimiterTest {

    private static final AttemptLimiter.Policy POLICY =
            new AttemptLimiter.Policy(3, Duration.ofMinutes(15), Duration.ofMinutes(15));

    private Map<String, AttemptCounter> rows;
    private AttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        rows = new HashMap<>();
        limiter = new AttemptLimiter(InMemoryAttemptLimiter.repository(rows));
    }

    @Test
    @DisplayName("Đếm ngược số lần còn lại, đủ số lần sai thì bị khóa")
    void locksAfterMaxFailures() {
        assertThat(limiter.recordFailure("k", POLICY)).isEqualTo(2);
        assertThat(limiter.recordFailure("k", POLICY)).isEqualTo(1);
        assertThat(limiter.lockRemaining("k")).isEmpty();

        assertThat(limiter.recordFailure("k", POLICY)).isZero();
        assertThat(limiter.lockRemaining("k")).hasValueSatisfying(d -> assertThat(d).isPositive());
    }

    @Test
    @DisplayName("Không lưu khóa thô (email/SĐT) trong database, chỉ lưu SHA-256")
    void storesHashedKey() {
        limiter.recordFailure("LOGIN:a@example.com", POLICY);

        assertThat(rows).hasSize(1);
        String id = rows.keySet().iterator().next();
        assertThat(id).hasSize(64).doesNotContain("example");
    }

    @Test
    @DisplayName("Hết khoảng đếm 15 phút thì đếm lại từ đầu")
    void windowExpiryRestartsCount() {
        limiter.recordFailure("k", POLICY);
        limiter.recordFailure("k", POLICY);
        rows.values().forEach(c -> c.setWindowStart(LocalDateTime.now().minusMinutes(16)));

        assertThat(limiter.recordFailure("k", POLICY)).isEqualTo(2);
        assertThat(limiter.lockRemaining("k")).isEmpty();
    }

    @Test
    @DisplayName("Hết thời gian khóa thì đăng nhập lại được")
    void lockExpires() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure("k", POLICY);
        }
        rows.values().forEach(c -> c.setLockedUntil(LocalDateTime.now().minusSeconds(1)));

        assertThat(limiter.lockRemaining("k")).isEmpty();
    }

    @Test
    @DisplayName("reset xóa bộ đếm")
    void resetClearsCounter() {
        limiter.recordFailure("k", POLICY);
        limiter.reset("k");

        assertThat(rows).isEmpty();
        assertThat(limiter.recordFailure("k", POLICY)).isEqualTo(2);
    }
}
