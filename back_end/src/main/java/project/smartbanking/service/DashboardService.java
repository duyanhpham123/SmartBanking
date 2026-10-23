package project.smartbanking.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.response.AdminOverviewResponse;
import project.smartbanking.dto.response.AdminTransactionResponse;
import project.smartbanking.dto.response.ChartResponse;
import project.smartbanking.dto.response.DashboardSummaryResponse;
import project.smartbanking.dto.response.TransactionResponse;
import project.smartbanking.dto.response.TransactionTypeStatsResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.model.Account;
import project.smartbanking.model.BankTransaction;
import project.smartbanking.model.enums.AccountStatus;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;
import project.smartbanking.repository.AccountRepository;
import project.smartbanking.repository.NotificationRepository;
import project.smartbanking.repository.TransactionRepository;
import project.smartbanking.repository.UserRepository;

@Service
public class DashboardService {

    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 30;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final AdminService adminService;

    public DashboardService(AccountRepository accountRepository,
                            TransactionRepository transactionRepository,
                            NotificationRepository notificationRepository,
                            UserRepository userRepository,
                            TransactionService transactionService,
                            AdminService adminService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
        this.adminService = adminService;
    }

    // Bộ cộng dồn theo từng ngày
    private static class Acc {
        long count;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal in = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
    }

    // ===================== NGƯỜI DÙNG: THẺ TỔNG QUAN =====================
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        List<Account> all = accountRepository.findByUserId(userId);
        List<Account> open = all.stream()
                .filter(a -> a.getStatus() != AccountStatus.CLOSED)
                .toList();
        List<Long> allIds = all.stream().map(Account::getId).toList();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long thisMonth = allIds.isEmpty()
                ? 0
                : transactionRepository.countByAccountIdsAndPeriod(allIds, monthStart, monthStart.plusMonths(1));

        return new DashboardSummaryResponse(
                sumBalance(open, "VND"),
                sumBalance(open, "USD"),
                open.size(),
                thisMonth,
                notificationRepository.countByUserIdAndReadFalse(userId));
    }

    // ===================== NGƯỜI DÙNG: BIỂU ĐỒ THEO NGÀY =====================
    @Transactional(readOnly = true)
    public ChartResponse<ChartResponse.DailyPoint> getChart(Long userId, int days, String currency) {
        int d = clampDays(days);
        String cur = normalizeCurrency(currency);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(d - 1L);

        Map<LocalDate, Acc> byDay = emptyDays(start, d);

        List<Long> ids = accountRepository.findByUserId(userId).stream().map(Account::getId).toList();
        if (!ids.isEmpty()) {
            Set<Long> mine = Set.copyOf(ids);
            Page<BankTransaction> page = transactionRepository.searchHistory(
                    ids, null, TransactionStatus.SUCCESS,
                    start.atStartOfDay(), today.plusDays(1).atStartOfDay(), null, Pageable.unpaged());

            for (BankTransaction t : page.getContent()) {
                if (!cur.equals(t.getCurrency())) {
                    continue;
                }
                Acc acc = byDay.get(t.getCreatedAt().toLocalDate());
                if (acc == null) {
                    continue;
                }
                // Cùng quy tắc với lịch sử giao dịch: tài khoản gửi là của mình thì là tiền ra
                boolean out = t.getFromAccount() != null && mine.contains(t.getFromAccount().getId());
                acc.count++;
                acc.total = acc.total.add(t.getAmount());
                if (out) {
                    acc.out = acc.out.add(t.getAmount());
                } else {
                    acc.in = acc.in.add(t.getAmount());
                }
            }
        }

        List<ChartResponse.DailyPoint> points = byDay.entrySet().stream()
                .map(e -> new ChartResponse.DailyPoint(e.getKey(), e.getValue().count,
                        e.getValue().total, e.getValue().in, e.getValue().out))
                .toList();
        return new ChartResponse<>(cur, points);
    }

    // ===================== NGƯỜI DÙNG: GIAO DỊCH GẦN ĐÂY =====================
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(Long userId, int limit) {
        return transactionService.getHistory(userId, null, null, null, null, null, 0, clampLimit(limit)).content();
    }

    // ===================== ADMIN: TỔNG QUAN HỆ THỐNG =====================
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminOverviewResponse getAdminOverview() {
        return new AdminOverviewResponse(
                userRepository.count(),
                accountRepository.count(),
                transactionRepository.count(),
                transactionRepository.sumAmountByStatusAndCurrency(TransactionStatus.SUCCESS, "VND"),
                transactionRepository.sumAmountByStatusAndCurrency(TransactionStatus.SUCCESS, "USD"));
    }

    // ===================== ADMIN: THỐNG KÊ THEO LOẠI =====================
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public TransactionTypeStatsResponse getTransactionsByType() {
        Map<TransactionType, Long> counts = new EnumMap<>(TransactionType.class);
        for (Object[] row : transactionRepository.countGroupByType()) {
            counts.put((TransactionType) row[0], ((Number) row[1]).longValue());
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        List<TransactionTypeStatsResponse.Item> items = Arrays.stream(TransactionType.values())
                .map(type -> {
                    long count = counts.getOrDefault(type, 0L);
                    double percent = total == 0 ? 0 : Math.round(count * 1000.0 / total) / 10.0;
                    return new TransactionTypeStatsResponse.Item(type, count, percent);
                })
                .toList();
        return new TransactionTypeStatsResponse(total, items);
    }

    // ===================== ADMIN: GIAO DỊCH GẦN ĐÂY =====================
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AdminTransactionResponse> getAdminRecentTransactions(int limit) {
        return adminService.getTransactions(null, null, null, null, null, 0, clampLimit(limit)).content();
    }

    // ===================== ADMIN: BIỂU ĐỒ TOÀN HỆ THỐNG =====================
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ChartResponse<ChartResponse.AdminDailyPoint> getAdminChart(int days, String currency) {
        int d = clampDays(days);
        String cur = normalizeCurrency(currency);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(d - 1L);

        Map<LocalDate, Acc> byDay = emptyDays(start, d);

        // Tải các giao dịch trong khoảng ngày rồi cộng dồn trong bộ nhớ (đủ dùng cho đồ án)
        Page<BankTransaction> page = transactionRepository.searchAll(
                null, TransactionStatus.SUCCESS,
                start.atStartOfDay(), today.plusDays(1).atStartOfDay(), null, Pageable.unpaged());

        for (BankTransaction t : page.getContent()) {
            if (!cur.equals(t.getCurrency())) {
                continue;
            }
            Acc acc = byDay.get(t.getCreatedAt().toLocalDate());
            if (acc == null) {
                continue;
            }
            acc.count++;
            acc.total = acc.total.add(t.getAmount());
        }

        List<ChartResponse.AdminDailyPoint> points = byDay.entrySet().stream()
                .map(e -> new ChartResponse.AdminDailyPoint(e.getKey(), e.getValue().count, e.getValue().total))
                .toList();
        return new ChartResponse<>(cur, points);
    }

    // ===================== HÀM PHỤ =====================

    private BigDecimal sumBalance(List<Account> accounts, String currency) {
        return accounts.stream()
                .filter(a -> currency.equals(a.getCurrency()))
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Tạo sẵn đủ các ngày (giá trị 0) để biểu đồ không bị thiếu ngày
    private Map<LocalDate, Acc> emptyDays(LocalDate start, int days) {
        Map<LocalDate, Acc> byDay = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            byDay.put(start.plusDays(i), new Acc());
        }
        return byDay;
    }

    private int clampDays(int days) {
        if (days < 1) {
            return DEFAULT_DAYS;
        }
        return Math.min(days, MAX_DAYS);
    }

    private int clampLimit(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeCurrency(String currency) {
        String c = (currency == null || currency.isBlank()) ? "VND" : currency.trim().toUpperCase();
        if (!c.equals("VND") && !c.equals("USD")) {
            throw new BadRequestException("Chỉ hỗ trợ loại tiền VND hoặc USD");
        }
        return c;
    }
}