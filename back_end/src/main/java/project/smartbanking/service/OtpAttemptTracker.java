package project.smartbanking.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import project.smartbanking.exception.TooManyRequestsException;
import project.smartbanking.model.enums.OtpPurpose;

// Đếm số lần nhập sai OTP trong bộ nhớ. Tắt ứng dụng thì đếm lại từ đầu.
@Component
public class OtpAttemptTracker {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(15);

    private static class State {
        int failures;
        Instant windowStart;
        Instant lockedUntil;
    }

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    public void checkNotLocked(Long userId, OtpPurpose purpose) {
        State s = states.get(key(userId, purpose));
        if (s == null) {
            return;
        }
        Instant now = Instant.now();
        synchronized (s) {
            if (s.lockedUntil != null && now.isBefore(s.lockedUntil)) {
                long minutes = Duration.between(now, s.lockedUntil).toMinutes() + 1;
                throw new TooManyRequestsException(
                        "Bạn đã nhập sai OTP quá nhiều lần, vui lòng thử lại sau " + minutes + " phút");
            }
        }
    }

    public void recordFailure(Long userId, OtpPurpose purpose) {
        Instant now = Instant.now();
        State s = states.computeIfAbsent(key(userId, purpose), k -> new State());
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
        }
    }

    public void reset(Long userId, OtpPurpose purpose) {
        states.remove(key(userId, purpose));
    }

    private String key(Long userId, OtpPurpose purpose) {
        return userId + ":" + purpose;
    }
}