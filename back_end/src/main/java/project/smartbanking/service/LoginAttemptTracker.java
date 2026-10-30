package project.smartbanking.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import project.smartbanking.exception.TooManyRequestsException;

// Chống dò mật khẩu: sai 5 lần trong 15 phút thì tạm khóa đăng nhập 15 phút.
// Đếm theo email/SĐT nhập vào (kể cả tài khoản không tồn tại, để không lộ tài khoản nào có thật).
// Lưu trong bộ nhớ: tắt ứng dụng thì đếm lại từ đầu.
@Component
public class LoginAttemptTracker {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(15);

    private static class State {
        int failures;
        Instant windowStart;
        Instant lockedUntil;
    }

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    public void checkNotLocked(String identifier) {
        State s = states.get(identifier);
        if (s == null) {
            return;
        }
        Instant now = Instant.now();
        synchronized (s) {
            if (s.lockedUntil != null && now.isBefore(s.lockedUntil)) {
                long minutes = Duration.between(now, s.lockedUntil).toMinutes() + 1;
                throw new TooManyRequestsException(
                        "Bạn đã nhập sai mật khẩu quá nhiều lần, vui lòng thử lại sau " + minutes + " phút");
            }
        }
    }

    // Trả về số lần còn được thử trước khi bị khóa
    public int recordFailure(String identifier) {
        Instant now = Instant.now();
        State s = states.computeIfAbsent(identifier, k -> new State());
        synchronized (s) {
            if (s.windowStart == null || now.isAfter(s.windowStart.plus(WINDOW))) {
                s.failures = 0;
                s.windowStart = now;
                s.lockedUntil = null;
            }
            s.failures++;
            if (s.failures >= MAX_FAILURES) {
                s.lockedUntil = now.plus(LOCK);
            }
            return Math.max(MAX_FAILURES - s.failures, 0);
        }
    }

    public void reset(String identifier) {
        states.remove(identifier);
    }
}
