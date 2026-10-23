package project.smartbanking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.smartbanking.dto.request.ForgotPasswordRequest;
import project.smartbanking.dto.request.LoginRequest;
import project.smartbanking.dto.request.RefreshTokenRequest;
import project.smartbanking.dto.request.RegisterRequest;
import project.smartbanking.dto.request.ResetPasswordRequest;
import project.smartbanking.dto.response.AuthResponse;
import project.smartbanking.dto.response.MessageResponse;
import project.smartbanking.dto.response.OtpSendResponse;
import project.smartbanking.dto.response.UserResponse;
import project.smartbanking.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import project.smartbanking.exception.UnauthorizedException;
import project.smartbanking.security.UserPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Đường dẫn /auth/** được mở công khai nên phải tự kiểm tra đã đăng nhập chưa
    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn");
        }
        return ResponseEntity.ok(authService.logoutAll(principal.getId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.logout(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<OtpSendResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}