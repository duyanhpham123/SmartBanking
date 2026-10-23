package project.smartbanking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.response.PageResponse;
import project.smartbanking.dto.response.TransactionResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.ResourceNotFoundException;
import project.smartbanking.mapper.TransactionMapper;
import project.smartbanking.model.Account;
import project.smartbanking.model.BankTransaction;
import project.smartbanking.model.enums.TransactionDirection;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;
import project.smartbanking.repository.AccountRepository;
import project.smartbanking.repository.TransactionRepository;

@Service
public class TransactionHistoryService {

    private static final int MAX_EXPORT_ROWS = 5000;
    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    public TransactionHistoryService(AccountRepository accountRepository,
                                     TransactionRepository transactionRepository,
                                     TransactionMapper transactionMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    // ===================== LỊCH SỬ CỦA MỘT TÀI KHOẢN =====================
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAccountHistory(Long userId, Long accountId,
                                                               TransactionType type, TransactionStatus status,
                                                               LocalDate from, LocalDate to, String keyword,
                                                               int page, int size) {
        // Tài khoản của người khác cũng báo 404
        accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 10 : Math.min(size, 100);
        LocalDateTime[] range = dateRange(from, to);
        Pageable pageable = PageRequest.of(safePage, safeSize, newestFirst());

        Page<BankTransaction> result = transactionRepository.searchHistory(
                List.of(accountId), type, status, range[0], range[1], like(keyword), pageable);

        // Chiều giao dịch tính theo riêng tài khoản này: gửi đi từ nó là DEBIT, nhận vào nó là CREDIT
        Set<Long> mine = Set.of(accountId);
        return PageResponse.from(result.map(t -> transactionMapper.toResponse(t, mine)));
    }

    // ===================== XUẤT LỊCH SỬ RA CSV =====================
    @Transactional(readOnly = true)
    public String exportCsv(Long userId, TransactionType type, TransactionStatus status,
                            LocalDate from, LocalDate to, String keyword) {
        LocalDateTime[] range = dateRange(from, to);

        // Ký tự \uFEFF ở đầu giúp Excel đọc đúng tiếng Việt có dấu
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Mã giao dịch,Thời gian,Loại giao dịch,Chiều,Số tiền,Phí,Tiền tệ,Trạng thái,")
                .append("Từ tài khoản,Đến tài khoản,Nội dung\r\n");

        List<Long> accountIds = accountRepository.findByUserId(userId).stream().map(Account::getId).toList();
        if (accountIds.isEmpty()) {
            return sb.toString();
        }

        Pageable pageable = PageRequest.of(0, MAX_EXPORT_ROWS, newestFirst());
        Page<BankTransaction> result = transactionRepository.searchHistory(
                accountIds, type, status, range[0], range[1], like(keyword), pageable);
        Set<Long> mine = Set.copyOf(accountIds);

        for (BankTransaction t : result.getContent()) {
            TransactionResponse r = transactionMapper.toResponse(t, mine);
            sb.append(cell(r.transactionCode())).append(',')
                    .append(cell(r.createdAt() == null ? "" : r.createdAt().format(CSV_TIME))).append(',')
                    .append(cell(r.transactionType().name())).append(',')
                    .append(cell(r.direction() == TransactionDirection.DEBIT ? "Tiền ra" : "Tiền vào")).append(',')
                    .append(cell(r.amount().toPlainString())).append(',')
                    .append(cell(r.fee().toPlainString())).append(',')
                    .append(cell(r.currency())).append(',')
                    .append(cell(r.status().name())).append(',')
                    .append(cell(r.fromAccountNumber())).append(',')
                    .append(cell(r.toAccountNumber())).append(',')
                    .append(cell(r.description())).append("\r\n");
        }
        return sb.toString();
    }

    // ===================== HÀM PHỤ =====================

    // Trả về [từ ngày, đến ngày]; ngày kết thúc lấy đầu ngày hôm sau vì điều kiện trong query là "< toDate"
    private LocalDateTime[] dateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        LocalDateTime fromDate = from == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : from.atStartOfDay();
        LocalDateTime toDate = to == null
                ? LocalDate.now().plusDays(1).atStartOfDay()
                : to.plusDays(1).atStartOfDay();
        return new LocalDateTime[] { fromDate, toDate };
    }

    private Sort newestFirst() {
        return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }

    // Từ khóa -> "%abc%" chữ thường, hoặc null nếu để trống
    private String like(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim().toLowerCase() + "%";
    }

    // Một ô CSV: chặn công thức Excel (ô bắt đầu bằng = + - @ sẽ bị Excel hiểu là công thức) và escape dấu phẩy, nháy kép
    private String cell(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}