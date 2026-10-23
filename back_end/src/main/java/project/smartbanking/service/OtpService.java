package project.smartbanking.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.response.OtpSendResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.InvalidOtpException;
import project.smartbanking.exception.TooManyRequestsException;
import project.smartbanking.model.OtpCode;
import project.smartbanking.model.enums.OtpPurpose;
import project.smartbanking.repository.OtpCodeRepository;
import project.smartbanking.repository.UserRepository;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int MAX_SENDS_PER_WINDOW = 3;
    private static final Duration SEND_WINDOW = Duration.ofMinutes(5);
    // Các mục đích cho phép người dùng đã đăng nhập tự yêu cầu
    private static final Set<OtpPurpose> SENDABLE = EnumSet.of(OtpPurpose.TRANSFER, OtpPurpose.CHANGE_PASSWORD);

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final OtpAttemptTracker attemptTracker;
    private final long expirationSeconds;
    private final boolean exposeInResponse;

    public OtpService(OtpCodeRepository otpCodeRepository,
                      UserRepository userRepository,
                      OtpAttemptTracker attemptTracker,
                      @Value("${app.otp.expiration-seconds:300}") long expirationSeconds,
                      @Value("${app.otp.expose-in-response:false}") boolean exposeInResponse) {
        this.otpCodeRepository = otpCodeRepository;
        this.userRepository = userRepository;
        this.attemptTracker = attemptTracker;
        this.expirationSeconds = expirationSeconds;
        this.exposeInResponse = exposeInResponse;
    }

    // ===================== GỬI OTP =====================
    @Transactional
    public OtpSendResponse send(Long userId, OtpPurpose purpose) {
        if (!SENDABLE.contains(purpose)) {
            throw new BadRequestException("Mục đích OTP không được hỗ trợ");
        }

        LocalDateTime now = LocalDateTime.now();
        long recent = otpCodeRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                userId, purpose, now.minus(SEND_WINDOW));
        if (recent >= MAX_SENDS_PER_WINDOW) {
            throw new TooManyRequestsException("Bạn yêu cầu OTP quá nhiều lần, vui lòng thử lại sau ít phút");
        }

        // Mỗi người chỉ có một OTP còn hiệu lực cho mỗi mục đích
        otpCodeRepository.invalidateAllByUserIdAndPurpose(userId, purpose);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setUser(userRepository.getReferenceById(userId));
        otp.setCode(code);
        otp.setPurpose(purpose);
        otp.setExpiresAt(now.plusSeconds(expirationSeconds));
        otp.setUsed(false);
        otpCodeRepository.save(otp);

        // Chưa có email/SMS: khi phát triển thì in ra console và trả về trong kết quả
        if (exposeInResponse) {
            log.info("[DEV] OTP của user {} cho mục đích {}: {}", userId, purpose, code);
        }

        return new OtpSendResponse("Mã OTP đã được gửi", expirationSeconds, exposeInResponse ? code : null);
    }

    // ===================== KIỂM TRA OTP (không tiêu hủy mã) =====================
    @Transactional(readOnly = true)
    public void verify(Long userId, OtpPurpose purpose, String code) {
        findValid(userId, purpose, code);
        attemptTracker.reset(userId, purpose);
    }

    // ===================== DÙNG OTP (tiêu hủy mã) =====================
    // Gọi bên trong transaction của nghiệp vụ (ví dụ chuyển tiền): nếu nghiệp vụ lỗi và rollback
    // thì việc tiêu hủy này cũng được hoàn tác, người dùng vẫn dùng lại được mã.
    @Transactional
    public void verifyAndConsume(Long userId, OtpPurpose purpose, String code) {
        OtpCode otp = findValid(userId, purpose, code);

        // Cập nhật có điều kiện used = false: hai request cùng lúc không thể cùng dùng một mã
        if (otpCodeRepository.markUsed(otp.getId()) == 0) {
            throw new InvalidOtpException();
        }
        attemptTracker.reset(userId, purpose);
    }

    // ===================== HÀM PHỤ =====================
    private OtpCode findValid(Long userId, OtpPurpose purpose, String code) {
        attemptTracker.checkNotLocked(userId, purpose);

        if (code == null || !code.matches("\\d{6}")) {
            attemptTracker.recordFailure(userId, purpose);
            throw new InvalidOtpException();
        }

        return otpCodeRepository
                .findFirstByUserIdAndPurposeAndCodeAndUsedFalseAndExpiresAtAfter(
                        userId, purpose, code, LocalDateTime.now())
                .orElseThrow(() -> {
                    attemptTracker.recordFailure(userId, purpose);
                    return new InvalidOtpException();
                });
    }
    
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}