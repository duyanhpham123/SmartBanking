package project.smartbanking.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.smartbanking.dto.request.RejectKycRequest;
import project.smartbanking.dto.response.AdminAccountResponse;
import project.smartbanking.dto.response.AdminTransactionResponse;
import project.smartbanking.dto.response.AdminUserDetailResponse;
import project.smartbanking.dto.response.CustomerProfileResponse;
import project.smartbanking.dto.response.PageResponse;
import project.smartbanking.dto.response.UserResponse;
import project.smartbanking.model.enums.AccountStatus;
import project.smartbanking.model.enums.AccountType;
import project.smartbanking.model.enums.KycStatus;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;
import project.smartbanking.model.enums.UserStatus;
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.service.AdminService;
import project.smartbanking.service.CustomerProfileService;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final CustomerProfileService customerProfileService;

    public AdminController(AdminService adminService, CustomerProfileService customerProfileService) {
        this.adminService = adminService;
        this.customerProfileService = customerProfileService;
    }

    // ===================== NGƯỜI DÙNG =====================
    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) KycStatus kycStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getUsers(status, kycStatus, keyword, page, size));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUser(id));
    }

    @PutMapping("/users/{id}/lock")
    public ResponseEntity<UserResponse> lockUser(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable Long id) {
        return ResponseEntity.ok(adminService.lockUser(principal.getId(), id));
    }

    @PutMapping("/users/{id}/unlock")
    public ResponseEntity<UserResponse> unlockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unlockUser(id));
    }

    // Xét duyệt hồ sơ định danh (KYC)
    @PutMapping("/users/{id}/kyc/verify")
    public ResponseEntity<CustomerProfileResponse> verifyKyc(@AuthenticationPrincipal UserPrincipal principal,
                                                             @PathVariable Long id) {
        return ResponseEntity.ok(customerProfileService.verify(principal.getId(), id));
    }

    @PutMapping("/users/{id}/kyc/reject")
    public ResponseEntity<CustomerProfileResponse> rejectKyc(@AuthenticationPrincipal UserPrincipal principal,
                                                             @PathVariable Long id,
                                                             @Valid @RequestBody RejectKycRequest request) {
        return ResponseEntity.ok(customerProfileService.reject(principal.getId(), id, request));
    }

    // ===================== TÀI KHOẢN NGÂN HÀNG =====================
    @GetMapping("/accounts")
    public ResponseEntity<PageResponse<AdminAccountResponse>> getAccounts(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAccounts(status, type, currency, keyword, page, size));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AdminAccountResponse> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAccount(id));
    }

    @PutMapping("/accounts/{id}/lock")
    public ResponseEntity<AdminAccountResponse> lockAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.lockAccount(id));
    }

    @PutMapping("/accounts/{id}/unlock")
    public ResponseEntity<AdminAccountResponse> unlockAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unlockAccount(id));
    }

    // ===================== GIAO DỊCH =====================
    // from/to dạng yyyy-MM-dd
    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<AdminTransactionResponse>> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getTransactions(type, status, from, to, keyword, page, size));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<AdminTransactionResponse> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getTransaction(id));
    }
}