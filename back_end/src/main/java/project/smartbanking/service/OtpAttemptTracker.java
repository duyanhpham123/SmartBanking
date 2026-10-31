package project.smartbanking.service;

import java.time.Duration;

import org.springframework.stereotype.Component;

import project.smartbanking.exception.TooManyRequestsException;
import project.smartbanking.model.enums.OtpPurpose;

// Chống đoán OTP: sai 5 lần trong 15 phút thì khóa 15 phút (theo người dùng và mục đích).
// Bộ đếm lưu trong database (bảng attempt_counters), không mất khi restart và dùng chung giữa các server.
@Component
public class OtpAttemptTracker {

    private static final AttemptLimiter.Policy POLICY =
            new AttemptLimiter.Policy(5, Duration.ofMinutes(15), Duration.ofMinutes(15));

    private final AttemptLimiter limiter;

    public OtpAttemptTracker(AttemptLimiter limiter) {
        this.limiter = limiter;
    }

    public void checkNotLocked(Long userId, OtpPurpose purpose) {
        limiter.lockRemaining(key(userId, purpose)).ifPresent(remaining -> {
            throw new TooManyRequestsException("Bạn đã nhập sai OTP quá nhiều lần, vui lòng thử lại sau "
                    + (remaining.toMinutes() + 1) + " phút");
        });
    }

    public void recordFailure(Long userId, OtpPurpose purpose) {
        limiter.recordFailure(key(userId, purpose), POLICY);
    }

    public void reset(Long userId, OtpPurpose purpose) {
        limiter.reset(key(userId, purpose));
    }

    private String key(Long userId, OtpPurpose purpose) {
        return "OTP:" + userId + ":" + purpose;
    }
}
