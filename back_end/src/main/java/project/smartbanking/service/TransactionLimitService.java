package project.smartbanking.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.response.TransactionLimitResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.model.TransactionLimit;
import project.smartbanking.model.User;
import project.smartbanking.repository.TransactionLimitRepository;
import project.smartbanking.repository.UserRepository;

// Hạn mức tổng tiền ra mỗi ngày trên một tài khoản, lưu trong database để admin chỉnh được
// mà không cần sửa code. Đọc lại mỗi giao dịch nên mọi server thấy giá trị mới ngay.
@Service
public class TransactionLimitService {

    private static final List<String> CURRENCIES = List.of("VND", "USD");

    // Dùng khi admin chưa chỉnh lần nào
    private static final Map<String, BigDecimal> DEFAULT_DAILY_OUT = Map.of(
            "VND", new BigDecimal("200000000"),
            "USD", new BigDecimal("20000"));

    // Chặn nhập nhầm thêm vài số 0
    private static final Map<String, BigDecimal> MAX_DAILY_OUT = Map.of(
            "VND", new BigDecimal("10000000000"),
            "USD", new BigDecimal("1000000"));

    private final TransactionLimitRepository limitRepository;
    private final UserRepository userRepository;

    public TransactionLimitService(TransactionLimitRepository limitRepository, UserRepository userRepository) {
        this.limitRepository = limitRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal getDailyOutLimit(String currency) {
        String c = requireSupported(currency);
        return limitRepository.findById(c)
                .map(TransactionLimit::getDailyOutLimit)
                .orElse(DEFAULT_DAILY_OUT.get(c));
    }

    @Transactional(readOnly = true)
    public List<TransactionLimitResponse> getAll() {
        return CURRENCIES.stream()
                .map(c -> toResponse(c, limitRepository.findById(c).orElse(null)))
                .toList();
    }

    @Transactional
    public TransactionLimitResponse update(Long adminId, String currency, BigDecimal dailyOutLimit) {
        String c = requireSupported(currency);
        BigDecimal amount = dailyOutLimit.stripTrailingZeros();
        if ("VND".equals(c) && amount.scale() > 0) {
            throw new BadRequestException("Hạn mức VND phải là số nguyên");
        }
        if (amount.compareTo(MAX_DAILY_OUT.get(c)) > 0) {
            throw new BadRequestException("Hạn mức tối đa là " + MAX_DAILY_OUT.get(c).toPlainString() + " " + c);
        }

        TransactionLimit limit = limitRepository.findById(c).orElseGet(() -> {
            TransactionLimit created = new TransactionLimit();
            created.setCurrency(c);
            return created;
        });
        limit.setDailyOutLimit(amount.setScale(2));
        limit.setUpdatedAt(LocalDateTime.now());
        limit.setUpdatedBy(adminId);
        return toResponse(c, limitRepository.save(limit));
    }

    private TransactionLimitResponse toResponse(String currency, TransactionLimit limit) {
        BigDecimal def = DEFAULT_DAILY_OUT.get(currency);
        if (limit == null) {
            return new TransactionLimitResponse(currency, def, def, MAX_DAILY_OUT.get(currency), null, null);
        }
        String updatedByName = limit.getUpdatedBy() == null ? null
                : userRepository.findById(limit.getUpdatedBy()).map(User::getFullName).orElse(null);
        return new TransactionLimitResponse(currency, limit.getDailyOutLimit(), def, MAX_DAILY_OUT.get(currency),
                limit.getUpdatedAt(), updatedByName);
    }

    private String requireSupported(String currency) {
        String c = currency == null ? "" : currency.trim().toUpperCase();
        if (!CURRENCIES.contains(c)) {
            throw new BadRequestException("Loại tiền không được hỗ trợ: " + currency);
        }
        return c;
    }
}
