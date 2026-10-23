package project.smartbanking.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.repository.IdempotencyKeyRepository;
import project.smartbanking.repository.OtpCodeRepository;
import project.smartbanking.repository.RefreshTokenRepository;

@Component
public class ScheduledCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCleanupJob.class);

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public ScheduledCleanupJob(IdempotencyKeyRepository idempotencyKeyRepository,
                               OtpCodeRepository otpCodeRepository,
                               RefreshTokenRepository refreshTokenRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Chạy vào phút 0 của mỗi giờ
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpired() {
        LocalDateTime now = LocalDateTime.now();
        int keys = idempotencyKeyRepository.deleteExpired(now);
        int otps = otpCodeRepository.deleteExpired(now);
        int tokens = refreshTokenRepository.deleteExpired(now);
        log.info("Dọn dữ liệu hết hạn: {} idempotency key, {} OTP, {} refresh token", keys, otps, tokens);
    }
}