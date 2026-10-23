package project.smartbanking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import project.smartbanking.dto.request.ForgotPasswordRequest;
import project.smartbanking.dto.request.RefreshTokenRequest;
import project.smartbanking.dto.request.RegisterRequest;
import project.smartbanking.dto.request.ResetPasswordRequest;
import project.smartbanking.dto.response.AuthResponse;
import project.smartbanking.dto.response.MessageResponse;
import project.smartbanking.dto.response.OtpSendResponse;
import project.smartbanking.dto.response.UserResponse;
import project.smartbanking.exception.ConflictException;
import project.smartbanking.exception.InvalidOtpException;
import project.smartbanking.exception.UnauthorizedException;
import project.smartbanking.mapper.UserMapper;
import project.smartbanking.model.RefreshToken;
import project.smartbanking.model.Role;
import project.smartbanking.model.User;
import project.smartbanking.model.enums.OtpPurpose;
import project.smartbanking.model.enums.UserStatus;
import project.smartbanking.repository.RefreshTokenRepository;
import project.smartbanking.repository.RoleRepository;
import project.smartbanking.repository.UserRepository;
import project.smartbanking.security.JwtService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private AccountService accountService;
    @Mock private OtpService otpService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, roleRepository, refreshTokenRepository, passwordEncoder,
                authenticationManager, jwtService, new UserMapper(), accountService, otpService,
                604_800_000L, 2_592_000_000L);

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "ENC(" + inv.getArgument(0) + ")");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===================== ĐĂNG KÝ =====================

    @Test
    @DisplayName("Đăng ký: chuẩn hóa email và số điện thoại, mã hóa mật khẩu, gán ROLE_USER, tạo tài khoản mặc định")
    void register_success() {
        UserResponse response = service.register(new RegisterRequest(
                "  Nguyễn Văn A ", "  Nguyen.Van@Example.COM ", "+84912345678", "Abc12345"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("nguyen.van@example.com");
        assertThat(saved.getPhone()).isEqualTo("0912345678");
        assertThat(saved.getUsername()).isEqualTo("nguyen.van");
        assertThat(saved.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(saved.getPassword()).isEqualTo("ENC(Abc12345)");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("ROLE_USER");
        verify(accountService).createDefaultAccount(saved);
        assertThat(response.roles()).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Username bị trùng thì tự thêm 4 chữ số ngẫu nhiên")
    void register_usernameCollision() {
        when(userRepository.existsByUsername("a")).thenReturn(true);

        service.register(new RegisterRequest("A", "a@example.com", "0912345678", "Abc12345"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).matches("a\\d{4}");
    }

    @Test
    @DisplayName("Email đã được dùng thì báo xung đột, không lưu gì")
    void register_duplicateEmail() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("A", "A@example.com", "0912345678", "Abc12345")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
        verify(accountService, never()).createDefaultAccount(any());
    }

    @Test
    @DisplayName("Số điện thoại đã được dùng (kể cả viết dạng +84) thì báo xung đột")
    void register_duplicatePhone() {
        when(userRepository.existsByPhone("0912345678")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("A", "a@example.com", "+84912345678", "Abc12345")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thiếu ROLE_USER trong database là lỗi cấu hình hệ thống")
    void register_missingRole() {
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("A", "a@example.com", "0912345678", "Abc12345")))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any());
    }

    // ===================== QUÊN MẬT KHẨU =====================

    @Test
    @DisplayName("Quên mật khẩu: tài khoản có thật thì gửi OTP mục đích CHANGE_PASSWORD")
    void forgotPassword_knownUser() {
        User user = user(5L, "a@example.com");
        when(userRepository.findByEmailOrPhone("a@example.com", "a@example.com")).thenReturn(Optional.of(user));
        when(otpService.send(5L, OtpPurpose.CHANGE_PASSWORD)).thenReturn(new OtpSendResponse("x", 300L, "654321"));

        OtpSendResponse response = service.forgotPassword(new ForgotPasswordRequest("A@Example.com"));

        verify(otpService).send(5L, OtpPurpose.CHANGE_PASSWORD);
        assertThat(response.devOtp()).isEqualTo("654321");
    }

    @Test
    @DisplayName("Quên mật khẩu: tài khoản không tồn tại trả thông báo giống hệt và không gửi OTP (không lộ tài khoản)")
    void forgotPassword_unknownUserLooksTheSame() {
        User user = user(5L, "a@example.com");
        when(userRepository.findByEmailOrPhone("a@example.com", "a@example.com")).thenReturn(Optional.of(user));
        when(otpService.send(5L, OtpPurpose.CHANGE_PASSWORD)).thenReturn(new OtpSendResponse("x", 300L, "654321"));

        OtpSendResponse known = service.forgotPassword(new ForgotPasswordRequest("a@example.com"));
        OtpSendResponse unknown = service.forgotPassword(new ForgotPasswordRequest("khong.ton.tai@example.com"));

        assertThat(unknown.message()).isEqualTo(known.message());
        assertThat(unknown.devOtp()).isNull();
        verify(otpService).send(anyLong(), any(OtpPurpose.class));   // chỉ đúng 1 lần, cho tài khoản có thật
    }

    @Test
    @DisplayName("Quên mật khẩu: tài khoản bị khóa được đối xử như không tồn tại")
    void forgotPassword_lockedUserTreatedAsUnknown() {
        User user = user(5L, "a@example.com");
        user.setStatus(UserStatus.LOCKED);
        when(userRepository.findByEmailOrPhone("a@example.com", "a@example.com")).thenReturn(Optional.of(user));

        OtpSendResponse response = service.forgotPassword(new ForgotPasswordRequest("a@example.com"));

        assertThat(response.devOtp()).isNull();
        verify(otpService, never()).send(anyLong(), any(OtpPurpose.class));
    }

    // ===================== ĐẶT LẠI MẬT KHẨU =====================

    @Test
    @DisplayName("Đặt lại mật khẩu: dùng OTP trước, rồi đổi mật khẩu, rồi thu hồi mọi refresh token")
    void resetPassword_success() {
        User user = user(5L, "a@example.com");
        when(userRepository.findByEmailOrPhone("a@example.com", "a@example.com")).thenReturn(Optional.of(user));

        MessageResponse response = service.resetPassword(
                new ResetPasswordRequest("A@Example.com", "123456", "NewPass123"));

        InOrder order = inOrder(otpService, userRepository, refreshTokenRepository);
        order.verify(otpService).verifyAndConsume(5L, OtpPurpose.CHANGE_PASSWORD, "123456");
        order.verify(userRepository).saveAndFlush(user);
        order.verify(refreshTokenRepository).revokeAllByUserId(5L);
        assertThat(user.getPassword()).isEqualTo("ENC(NewPass123)");
        assertThat(response.message()).isNotBlank();
    }

    @Test
    @DisplayName("OTP sai thì mật khẩu không bị đổi")
    void resetPassword_wrongOtp() {
        User user = user(5L, "a@example.com");
        user.setPassword("OLD");
        when(userRepository.findByEmailOrPhone("a@example.com", "a@example.com")).thenReturn(Optional.of(user));
        doThrow(new InvalidOtpException()).when(otpService)
                .verifyAndConsume(eq(5L), eq(OtpPurpose.CHANGE_PASSWORD), eq("000000"));

        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest("a@example.com", "000000", "NewPass123")))
                .isInstanceOf(InvalidOtpException.class);

        assertThat(user.getPassword()).isEqualTo("OLD");
        verify(userRepository, never()).saveAndFlush(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
    }

    @Test
    @DisplayName("Tài khoản không tồn tại cũng báo 'OTP không hợp lệ' (không lộ tài khoản)")
    void resetPassword_unknownUser() {
        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest("khong.ton.tai@example.com", "123456", "NewPass123")))
                .isInstanceOf(InvalidOtpException.class);

        verify(otpService, never()).verifyAndConsume(anyLong(), any(OtpPurpose.class), anyString());
    }

    // ===================== LÀM MỚI TOKEN / ĐĂNG XUẤT =====================

    @Test
    @DisplayName("Làm mới token: thu hồi token cũ, cấp token mới giữ nguyên hạn dùng")
    void refresh_rotatesToken() {
        User user = user(5L, "a@example.com");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);
        RefreshToken old = refreshToken(user, "old-token", expiresAt);
        when(refreshTokenRepository.findByTokenAndRevokedFalseAndExpiresAtAfter(
                eq("old-token"), any(LocalDateTime.class))).thenReturn(Optional.of(old));
        when(jwtService.generateAccessToken(any())).thenReturn("ACCESS");

        AuthResponse response = service.refresh(new RefreshTokenRequest("old-token"));

        assertThat(old.isRevoked()).isTrue();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken fresh = captor.getValue();
        assertThat(fresh.getToken()).isNotEqualTo("old-token");
        assertThat(fresh.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(response.accessToken()).isEqualTo("ACCESS");
        assertThat(response.refreshToken()).isEqualTo(fresh.getToken());
    }

    @Test
    @DisplayName("Refresh token không hợp lệ hoặc hết hạn thì báo 401")
    void refresh_invalidToken() {
        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("khong-co")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Người dùng đã bị khóa thì không làm mới được token")
    void refresh_lockedUser() {
        User user = user(5L, "a@example.com");
        user.setStatus(UserStatus.LOCKED);
        RefreshToken old = refreshToken(user, "old-token", LocalDateTime.now().plusDays(3));
        when(refreshTokenRepository.findByTokenAndRevokedFalseAndExpiresAtAfter(
                eq("old-token"), any(LocalDateTime.class))).thenReturn(Optional.of(old));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("old-token")))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng xuất: thu hồi refresh token")
    void logout_revokesToken() {
        RefreshToken token = refreshToken(user(5L, "a@example.com"), "my-token", LocalDateTime.now().plusDays(3));
        when(refreshTokenRepository.findByToken("my-token")).thenReturn(Optional.of(token));

        service.logout(new RefreshTokenRequest("my-token"));

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Đăng xuất bằng token không tồn tại vẫn thành công (đăng xuất lặp lại không lỗi)")
    void logout_unknownTokenIsFine() {
        MessageResponse response = service.logout(new RefreshTokenRequest("khong-co"));

        assertThat(response.message()).isNotBlank();
    }

    @Test
    @DisplayName("Đăng xuất mọi thiết bị: thu hồi toàn bộ refresh token của người dùng")
    void logoutAll_revokesEverything() {
        service.logoutAll(5L);

        verify(refreshTokenRepository).revokeAllByUserId(5L);
    }

    // ===================== HÀM PHỤ =====================

    private User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setUsername("user" + id);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    private RefreshToken refreshToken(User user, String token, LocalDateTime expiresAt) {
        RefreshToken t = new RefreshToken();
        t.setUser(user);
        t.setToken(token);
        t.setExpiresAt(expiresAt);
        return t;
    }
}