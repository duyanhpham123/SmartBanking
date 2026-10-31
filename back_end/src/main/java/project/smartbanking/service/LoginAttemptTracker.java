package project.smartbanking.service;

import java.time.Duration;

import org.springframework.stereotype.Component;

import project.smartbanking.exception.TooManyRequestsException;

// Chống dò mật khẩu: sai 5 lần trong 15 phút thì tạm khóa đăng nhập 15 phút.
// Đếm theo email/SĐT nhập vào (kể cả tài khoản không tồn tại, để không lộ tài khoản nào có thật).
// Bộ đếm lưu trong database (bảng attempt_counters), không mất khi restart và dùng chung giữa các server.
@Component
public class LoginAttemptTracker {

    private static final AttemptLimiter.Policy POLICY =
            new AttemptLimiter.Policy(5, Duration.ofMinutes(15), Duration.ofMinutes(15));

    private final AttemptLimiter limiter;

    public LoginAttemptTracker(AttemptLimiter limiter) {
        this.limiter = limiter;
    }

    public void checkNotLocked(String identifier) {
        limiter.lockRemaining(key(identifier)).ifPresent(remaining -> {
            throw new TooManyRequestsException("Bạn đã nhập sai mật khẩu quá nhiều lần, vui lòng thử lại sau "
                    + (remaining.toMinutes() + 1) + " phút");
        });
    }

    // Trả về số lần còn được thử trước khi bị khóa
    public int recordFailure(String identifier) {
        return limiter.recordFailure(key(identifier), POLICY);
    }

    public void reset(String identifier) {
        limiter.reset(key(identifier));
    }

    private String key(String identifier) {
        return "LOGIN:" + identifier;
    }
}
