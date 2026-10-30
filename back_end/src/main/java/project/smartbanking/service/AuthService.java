package project.smartbanking.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.request.ForgotPasswordRequest;
import project.smartbanking.dto.request.LoginRequest;
import project.smartbanking.dto.request.RefreshTokenRequest;
import project.smartbanking.dto.request.RegisterRequest;
import project.smartbanking.dto.request.ResetPasswordRequest;
import project.smartbanking.dto.response.AuthResponse;
import project.smartbanking.dto.response.MessageResponse;
import project.smartbanking.dto.response.OtpSendResponse;
import project.smartbanking.dto.response.UserResponse;
import project.smartbanking.exception.ConflictException;
import project.smartbanking.exception.InvalidOtpException;
import project.smartbanking.exception.ResourceNotFoundException;
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
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.util.AuditContext;
import project.smartbanking.util.IdentifierUtils;

@Service
public class AuthService {

    // Thông báo giống hệt nhau dù tài khoản có tồn tại hay không
    private static final String FORGOT_PASSWORD_MESSAGE = "Nếu tài khoản tồn tại, mã OTP đã được gửi";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AccountService accountService;
    private final OtpService otpService;
    private final LoginAttemptTracker loginAttemptTracker;

    private final long refreshExpirationMs;
    private final long refreshRememberExpirationMs;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserMapper userMapper,
                       AccountService accountService,
                       OtpService otpService,
                       LoginAttemptTracker loginAttemptTracker,
                       @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs,
                       @Value("${app.jwt.refresh-expiration-remember-ms}") long refreshRememberExpirationMs) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.accountService = accountService;
        this.otpService = otpService;
        this.loginAttemptTracker = loginAttemptTracker;
        this.refreshExpirationMs = refreshExpirationMs;
        this.refreshRememberExpirationMs = refreshRememberExpirationMs;
    }

    // ===================== ĐĂNG KÝ =====================
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String phone = IdentifierUtils.normalizePhone(request.phone());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email đã được sử dụng");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("Số điện thoại đã được sử dụng");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Role ROLE_USER chưa có trong database"));

        User user = new User();
        user.setUsername(generateUsername(email));
        user.setEmail(email);
        user.setPhone(phone);
        user.setFullName(request.fullName().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(userRole);

        User saved = userRepository.save(user);
        AuditContext.setUserId(saved.getId());
        accountService.createDefaultAccount(saved);   // xóa dòng này nếu không muốn tự tạo tài khoản
        return userMapper.toResponse(saved);
    }

    // ===================== ĐĂNG NHẬP =====================
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = IdentifierUtils.normalizeIdentifier(request.identifier());
        loginAttemptTracker.checkNotLocked(identifier);

        // Sai mật khẩu / không tồn tại -> 401 kèm số lần còn được thử
        // Bị khóa / chưa kích hoạt -> LockedException / DisabledException (403)
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.password()));
        } catch (BadCredentialsException ex) {
            int remaining = loginAttemptTracker.recordFailure(identifier);
            throw new UnauthorizedException(remaining > 0
                    ? "Tài khoản hoặc mật khẩu không đúng (còn " + remaining + " lần thử)"
                    : "Bạn đã nhập sai mật khẩu quá nhiều lần, vui lòng thử lại sau 15 phút");
        }
        loginAttemptTracker.reset(identifier);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        user.setLastLoginAt(LocalDateTime.now());
        AuditContext.setUserId(user.getId());

        long refreshMs = request.rememberMe() ? refreshRememberExpirationMs : refreshExpirationMs;
        RefreshToken refreshToken = saveRefreshToken(user, LocalDateTime.now().plus(Duration.ofMillis(refreshMs)));

        return buildAuthResponse(user, refreshToken);
    }

    // ===================== LÀM MỚI TOKEN =====================
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken oldToken = refreshTokenRepository
                .findByTokenAndRevokedFalseAndExpiresAtAfter(request.refreshToken(), LocalDateTime.now())
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn"));

        User user = oldToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản không còn hoạt động");
        }

        // Xoay vòng: thu hồi token cũ, cấp token mới (giữ nguyên hạn dùng)
        oldToken.setRevoked(true);
        RefreshToken newToken = saveRefreshToken(user, oldToken.getExpiresAt());

        return buildAuthResponse(user, newToken);
    }

    // ===================== ĐĂNG XUẤT =====================

    // ===================== ĐĂNG XUẤT MỌI THIẾT BỊ =====================
    @Transactional
    public MessageResponse logoutAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        return new MessageResponse("Đã đăng xuất khỏi tất cả thiết bị");
    }

    @Transactional
    public MessageResponse logout(RefreshTokenRequest request) {
        // Không tìm thấy thì thôi, đăng xuất lặp lại vẫn coi là thành công
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> token.setRevoked(true));
        return new MessageResponse("Đăng xuất thành công");
    }

    // ===================== QUÊN MẬT KHẨU: GỬI OTP =====================
    @Transactional
    public OtpSendResponse forgotPassword(ForgotPasswordRequest request) {
        String identifier = IdentifierUtils.normalizeIdentifier(request.identifier());

        return userRepository.findByEmailOrPhone(identifier, identifier)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .map(u -> {
                    OtpSendResponse sent = otpService.send(u.getId(), OtpPurpose.CHANGE_PASSWORD);
                    return new OtpSendResponse(FORGOT_PASSWORD_MESSAGE, sent.expiresInSeconds(), sent.devOtp());
                })
                .orElseGet(() -> new OtpSendResponse(FORGOT_PASSWORD_MESSAGE,
                        otpService.getExpirationSeconds(), null));
    }

    // ===================== ĐẶT LẠI MẬT KHẨU BẰNG OTP =====================
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String identifier = IdentifierUtils.normalizeIdentifier(request.identifier());

        // Không có tài khoản cũng báo "OTP không hợp lệ" để không lộ tài khoản có tồn tại hay không
        User user = userRepository.findByEmailOrPhone(identifier, identifier)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(InvalidOtpException::new);
        AuditContext.setUserId(user.getId());

        // Mã chỉ bị tiêu hủy khi cả việc đổi mật khẩu thành công (cùng một transaction)
        otpService.verifyAndConsume(user.getId(), OtpPurpose.CHANGE_PASSWORD, request.otp());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);                       // ghi trước, vì lệnh dưới sẽ làm sạch bộ nhớ đệm
        refreshTokenRepository.revokeAllByUserId(user.getId());  // đăng xuất mọi thiết bị

        return new MessageResponse("Đặt lại mật khẩu thành công, vui lòng đăng nhập lại");
    }

    // ===================== HÀM PHỤ =====================
    private RefreshToken saveRefreshToken(User user, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(expiresAt);
        return refreshTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(User user, RefreshToken refreshToken) {
        String accessToken = jwtService.generateAccessToken(UserPrincipal.from(user));
        return new AuthResponse(accessToken, refreshToken.getToken(),
                jwtService.getAccessExpirationSeconds(), userMapper.toResponse(user));
    }

    // Form đăng ký không có ô username nên tự sinh từ phần trước dấu @ của email
    private String generateUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9._]", "");
        if (base.isBlank()) {
            base = "user";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        String username = base;
        while (userRepository.existsByUsername(username)) {
            username = base + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return username;
    }
}