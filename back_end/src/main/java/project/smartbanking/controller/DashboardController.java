package project.smartbanking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import project.smartbanking.dto.response.AdminOverviewResponse;
import project.smartbanking.dto.response.AdminTransactionResponse;
import project.smartbanking.dto.response.ChartResponse;
import project.smartbanking.dto.response.DashboardSummaryResponse;
import project.smartbanking.dto.response.TransactionResponse;
import project.smartbanking.dto.response.TransactionTypeStatsResponse;
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.service.DashboardService;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ===================== NGƯỜI DÙNG =====================
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getSummary(principal.getId()));
    }

    @GetMapping("/dashboard/chart")
    public ResponseEntity<ChartResponse<ChartResponse.DailyPoint>> chart(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "VND") String currency) {
        return ResponseEntity.ok(dashboardService.getChart(principal.getId(), days, currency));
    }

    @GetMapping("/dashboard/recent-transactions")
    public ResponseEntity<List<TransactionResponse>> recentTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentTransactions(principal.getId(), limit));
    }

    // ===================== ADMIN =====================
    @GetMapping("/admin/dashboard/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminOverviewResponse> adminOverview() {
        return ResponseEntity.ok(dashboardService.getAdminOverview());
    }

    @GetMapping("/admin/dashboard/transactions-by-type")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionTypeStatsResponse> transactionsByType() {
        return ResponseEntity.ok(dashboardService.getTransactionsByType());
    }

    @GetMapping("/admin/dashboard/recent-transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminTransactionResponse>> adminRecentTransactions(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getAdminRecentTransactions(limit));
    }

    @GetMapping("/admin/dashboard/chart")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChartResponse<ChartResponse.AdminDailyPoint>> adminChart(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "VND") String currency) {
        return ResponseEntity.ok(dashboardService.getAdminChart(days, currency));
    }
}