package project.smartbanking.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.model.AttemptCounter;
import project.smartbanking.repository.AttemptCounterRepository;

// Đếm số lần thử sai trong database, dùng chung cho chống dò mật khẩu và chống đoán OTP.
// Sai maxFailures lần trong khoảng window thì khóa trong khoảng lock.
@Component
public class AttemptLimiter {

    public record Policy(int maxFailures, Duration window, Duration lock) {
    }

    private final AttemptCounterRepository repository;

    public AttemptLimiter(AttemptCounterRepository repository) {
        this.repository = repository;
    }

    // Thời gian còn bị khóa, rỗng nếu không bị khóa
    @Transactional(readOnly = true)
    public Optional<Duration> lockRemaining(String key) {
        LocalDateTime now = LocalDateTime.now();
        return repository.findById(hash(key))
                .map(AttemptCounter::getLockedUntil)
                .filter(now::isBefore)
                .map(until -> Duration.between(now, until));
    }

    // REQUIRES_NEW: lần sai luôn được ghi lại, kể cả khi nghiệp vụ gọi tới ném lỗi và rollback.
    // Trả về số lần còn được thử trước khi bị khóa.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailure(String key, Policy policy) {
        String id = hash(key);
        LocalDateTime now = LocalDateTime.now();
        repository.insertIfAbsent(id, now);
        AttemptCounter c = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalStateException("Không tạo được bộ đếm"));

        if (c.getWindowStart() == null || now.isAfter(c.getWindowStart().plus(policy.window()))) {
            c.setFailures(0);
            c.setWindowStart(now);
            c.setLockedUntil(null);
        }
        c.setFailures(c.getFailures() + 1);
        if (c.getFailures() >= policy.maxFailures()) {
            c.setLockedUntil(now.plus(policy.lock()));
        }
        c.setUpdatedAt(now);
        repository.save(c);
        return Math.max(policy.maxFailures() - c.getFailures(), 0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(String key) {
        repository.deleteByKey(hash(key));
    }

    static String hash(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
