package project.smartbanking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import project.smartbanking.dto.response.OtpSendResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.InvalidOtpException;
import project.smartbanking.exception.TooManyRequestsException;
import project.smartbanking.model.OtpCode;
import project.smartbanking.model.enums.OtpPurpose;
import project.smartbanking.repository.OtpCodeRepository;
import project.smartbanking.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServiceTest {

    private static final Long USER_ID = 7L;

    @Mock private OtpCodeRepository otpCodeRepository;
    @Mock private UserRepository userRepository;

    private OtpService service;

    @BeforeEach
    void setUp() {
        // Mỗi test dùng bộ đếm nhập sai mới để các test không ảnh hưởng nhau
        service = new OtpService(otpCodeRepository, userRepository, new OtpAttemptTracker(InMemoryAttemptLimiter.create()), 300L, true);
    }

    // ===================== GỬI OTP =====================

    @ParameterizedTest
    @EnumSource(value = OtpPurpose.class, names = { "TRANSFER", "CHANGE_PASSWORD" })
    @DisplayName("Gửi OTP: mã 6 chữ số, hạn 5 phút, hủy mã cũ trước khi lưu mã mới")
    void send_createsSixDigitCode(OtpPurpose purpose) {
        OtpSendResponse response = service.send(USER_ID, purpose);

        ArgumentCaptor<OtpCode> captor = ArgumentCaptor.forClass(OtpCode.class);
        InOrder order = inOrder(otpCodeRepository);
        order.verify(otpCodeRepository).invalidateAllByUserIdAndPurpose(USER_ID, purpose);
        order.verify(otpCodeRepository).save(captor.capture());

        OtpCode saved = captor.getValue();
        assertThat(saved.getCode()).matches("\\d{6}");
        assertThat(saved.getPurpose()).isEqualTo(purpose);
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isBetween(
                LocalDateTime.now().plusSeconds(290), LocalDateTime.now().plusSeconds(310));
        assertThat(response.expiresInSeconds()).isEqualTo(300);
        assertThat(response.devOtp()).isEqualTo(saved.getCode());
    }

    @Test
    @DisplayName("Khi tắt chế độ phát triển thì không trả mã OTP trong kết quả")
    void send_hidesCodeWhenNotExposed() {
        OtpService production = new OtpService(otpCodeRepository, userRepository, new OtpAttemptTracker(InMemoryAttemptLimiter.create()), 300L, false);

        OtpSendResponse response = production.send(USER_ID, OtpPurpose.TRANSFER);

        assertThat(response.devOtp()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = OtpPurpose.class, names = { "LOGIN", "VERIFY_EMAIL", "VERIFY_PHONE" })
    @DisplayName("Mục đích OTP không được hỗ trợ bị từ chối")
    void send_unsupportedPurpose(OtpPurpose purpose) {
        assertThatThrownBy(() -> service.send(USER_ID, purpose))
                .isInstanceOf(BadRequestException.class);

        verify(otpCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đã gửi 3 lần trong 5 phút thì lần thứ 4 bị chặn")
    void send_rateLimited() {
        when(otpCodeRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                eq(USER_ID), eq(OtpPurpose.TRANSFER), any(LocalDateTime.class))).thenReturn(3L);

        assertThatThrownBy(() -> service.send(USER_ID, OtpPurpose.TRANSFER))
                .isInstanceOf(TooManyRequestsException.class);

        verify(otpCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mới gửi 2 lần thì vẫn được gửi tiếp")
    void send_allowedBelowLimit() {
        when(otpCodeRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                eq(USER_ID), eq(OtpPurpose.TRANSFER), any(LocalDateTime.class))).thenReturn(2L);

        service.send(USER_ID, OtpPurpose.TRANSFER);

        verify(otpCodeRepository).save(any(OtpCode.class));
    }

    // ===================== DÙNG OTP =====================

    @Test
    @DisplayName("OTP đúng: được dùng và bị đánh dấu đã dùng")
    void consume_validCode() {
        stubValidOtp(USER_ID, "123456", 99L);

        service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "123456");

        verify(otpCodeRepository).markUsed(99L);
    }

    @Test
    @DisplayName("OTP sai hoặc hết hạn hoặc đã dùng thì báo không hợp lệ")
    void consume_unknownCode() {
        // không stub gì: repository trả Optional.empty()
        assertThatThrownBy(() -> service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "000000"))
                .isInstanceOf(InvalidOtpException.class);

        verify(otpCodeRepository, never()).markUsed(anyLong());
    }

    @Test
    @DisplayName("Hai request cùng lúc dùng một mã: request thứ hai thua và bị từ chối")
    void consume_lostRace() {
        stubValidOtp(USER_ID, "123456", 99L);
        when(otpCodeRepository.markUsed(99L)).thenReturn(0);

        assertThatThrownBy(() -> service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "123456"))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    @DisplayName("Mã không phải 6 chữ số bị từ chối mà không cần truy vấn database")
    void consume_malformedCode() {
        assertThatThrownBy(() -> service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "12ab56"))
                .isInstanceOf(InvalidOtpException.class);

        verify(otpCodeRepository, never()).findFirstByUserIdAndPurposeAndCodeAndUsedFalseAndExpiresAtAfter(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("Chỉ kiểm tra (verify) thì không tiêu hủy mã")
    void verify_doesNotConsume() {
        stubValidOtp(USER_ID, "123456", 99L);

        service.verify(USER_ID, OtpPurpose.TRANSFER, "123456");

        verify(otpCodeRepository, never()).markUsed(anyLong());
    }

    // ===================== CHẶN ĐOÁN MÃ =====================

    @Test
    @DisplayName("Nhập sai 5 lần thì bị khóa, kể cả lần sau nhập đúng mã")
    void lockedAfterFiveFailures() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "000000"))
                    .isInstanceOf(InvalidOtpException.class);
        }
        stubValidOtp(USER_ID, "123456", 99L);

        assertThatThrownBy(() -> service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "123456"))
                .isInstanceOf(TooManyRequestsException.class);

        verify(otpCodeRepository, never()).markUsed(anyLong());
    }

    @Test
    @DisplayName("Khóa của người này không ảnh hưởng người khác")
    void lockIsPerUser() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "000000"))
                    .isInstanceOf(InvalidOtpException.class);
        }
        stubValidOtp(8L, "123456", 100L);

        service.verifyAndConsume(8L, OtpPurpose.TRANSFER, "123456");

        verify(otpCodeRepository).markUsed(100L);
    }

    @Test
    @DisplayName("Nhập đúng thì bộ đếm sai được xóa")
    void successResetsFailureCounter() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "000000"))
                    .isInstanceOf(InvalidOtpException.class);
        }
        stubValidOtp(USER_ID, "123456", 99L);
        service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "123456");

        // Sau khi thành công, 4 lần sai nữa vẫn chưa bị khóa (vẫn là "OTP không hợp lệ")
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "000000"))
                    .isInstanceOf(InvalidOtpException.class);
        }
    }

    // ===================== HÀM PHỤ =====================

    private void stubValidOtp(Long userId, String code, Long otpId) {
        OtpCode otp = new OtpCode();
        otp.setId(otpId);
        otp.setCode(code);
        when(otpCodeRepository.findFirstByUserIdAndPurposeAndCodeAndUsedFalseAndExpiresAtAfter(
                eq(userId), eq(OtpPurpose.TRANSFER), eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(otp));
        when(otpCodeRepository.markUsed(otpId)).thenReturn(1);
    }
}