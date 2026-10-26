package project.smartbanking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.smartbanking.dto.request.ChangePasswordRequest;
import project.smartbanking.dto.request.UpdateKycRequest;
import project.smartbanking.dto.request.UpdateProfileRequest;
import project.smartbanking.dto.response.CustomerProfileResponse;
import project.smartbanking.dto.response.MessageResponse;
import project.smartbanking.dto.response.UserResponse;
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.service.CustomerProfileService;
import project.smartbanking.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final CustomerProfileService customerProfileService;

    public UserController(UserService userService, CustomerProfileService customerProfileService) {
        this.userService = userService;
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(principal.getId(), request));
    }

    // ===================== HỒ SƠ ĐỊNH DANH (KYC) =====================
    @GetMapping("/me/kyc")
    public ResponseEntity<CustomerProfileResponse> myKyc(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(customerProfileService.getMyProfile(principal.getId()));
    }

    @PutMapping("/me/kyc")
    public ResponseEntity<CustomerProfileResponse> submitKyc(@AuthenticationPrincipal UserPrincipal principal,
                                                             @Valid @RequestBody UpdateKycRequest request) {
        return ResponseEntity.ok(customerProfileService.submitMyProfile(principal.getId(), request));
    }
}