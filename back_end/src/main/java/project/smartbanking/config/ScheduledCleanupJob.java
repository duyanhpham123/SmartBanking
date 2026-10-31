package project.smartbanking.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.repository.AttemptCounterRepository;
import project.smartbanking.repository.IdempotencyKeyRepository;
import project.smartbanking.repository.OtpCodeRepository;
import project.smartbanking.repository.RefreshTokenRepository;

@Component
public class ScheduledCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCleanupJob.class);

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AttemptCounterRepository attemptCounterRepository;

    public ScheduledCleanupJob(IdempotencyKeyRepository idempotencyKeyRepository,
                               OtpCodeRepository otpCodeRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               AttemptCounterRepository attemptCounterRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.attemptCounterRepository = attemptCounterRepository;
    }

    // Chạy vào phút 0 của mỗi giờ
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpired() {
        LocalDateTime now = LocalDateTime.now();
        int keys = idempotencyKeyRepository.deleteExpired(now);
        int otps = otpCodeRepository.deleteExpired(now);
        int tokens = refreshTokenRepository.deleteExpired(now);
        // Bộ đếm sai mật khẩu/OTP: khóa chỉ kéo dài 15 phút, quá 1 ngày không dùng thì bỏ
        int counters = attemptCounterRepository.deleteStale(now.minusDays(1));
        log.info("Dọn dữ liệu hết hạn: {} idempotency key, {} OTP, {} refresh token, {} bộ đếm thử sai",
                keys, otps, tokens, counters);
    }
}