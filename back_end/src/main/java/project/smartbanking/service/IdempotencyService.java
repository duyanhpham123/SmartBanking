package project.smartbanking.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.ConflictException;
import project.smartbanking.model.IdempotencyKey;
import project.smartbanking.model.enums.IdempotencyStatus;
import project.smartbanking.repository.IdempotencyKeyRepository;
import project.smartbanking.repository.TransactionRepository;
import project.smartbanking.repository.UserRepository;

@Service
public class IdempotencyService {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,100}$");
    private static final Duration KEY_TTL = Duration.ofHours(24);

    // Kết quả trả về: replayed = true nghĩa là đây là kết quả đã lưu của lần gửi trước
    public record IdempotentResult<T>(T response, boolean replayed) {
    }

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository,
                              UserRepository userRepository,
                              TransactionRepository transactionRepository,
                              TransactionTemplate transactionTemplate,
                              ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    // Dấu vân tay của nội dung yêu cầu, dùng để phát hiện "cùng mã nhưng nội dung khác"
    public String fingerprint(Object... parts) {
        String joined = Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining("|"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(joined.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // LƯU Ý: method này cố ý KHÔNG đặt @Transactional, vì nó tự quản lý transaction bằng TransactionTemplate
    public <T> IdempotentResult<T> execute(Long userId, String rawKey, String requestHash,
                                           Class<T> responseType, Function<T, Long> transactionIdOf,
                                           Supplier<T> action) {
        String key = validateKey(rawKey);

        // 1. Mã này đã từng hoàn tất? Trả lại kết quả cũ, không làm lại
        Optional<T> replay = findReplay(userId, key, requestHash, responseType);
        if (replay.isPresent()) {
            return new IdempotentResult<>(replay.get(), true);
        }

        // 2. Chưa có: ghi mã + thực hiện + lưu kết quả trong CÙNG MỘT transaction
        try {
            T result = transactionTemplate.execute(status -> {
                removeIfExpired(userId, key);

                IdempotencyKey record = new IdempotencyKey();
                record.setUser(userRepository.getReferenceById(userId));
                record.setIdempotencyKey(key);
                record.setRequestHash(requestHash);
                record.setStatus(IdempotencyStatus.PROCESSING);
                record.setExpiresAt(LocalDateTime.now().plus(KEY_TTL));
                // Ghi ngay: nếu cùng mã đang được xử lý ở request khác, dòng này sẽ chờ rồi báo trùng
                idempotencyKeyRepository.saveAndFlush(record);

                T value = action.get();

                Long transactionId = transactionIdOf.apply(value);
                if (transactionId != null) {
                    record.setTransaction(transactionRepository.getReferenceById(transactionId));
                }
                record.setResponseBody(toJson(value));
                record.setStatus(IdempotencyStatus.COMPLETED);
                idempotencyKeyRepository.save(record);
                return value;
            });
            return new IdempotentResult<>(result, false);

        } catch (DataIntegrityViolationException ex) {
            // Một request khác cùng mã vừa hoàn tất trước: lấy kết quả của nó
            return findReplay(userId, key, requestHash, responseType)
                    .map(r -> new IdempotentResult<>(r, true))
                    .orElseThrow(() -> ex);
        }
    }

    // ===================== HÀM PHỤ =====================

    private String validateKey(String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim();
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new BadRequestException(
                    "Idempotency-Key phải dài 8-100 ký tự, chỉ gồm chữ, số, dấu gạch ngang và gạch dưới");
        }
        return key;
    }

    private <T> Optional<T> findReplay(Long userId, String key, String requestHash, Class<T> responseType) {
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyKey record = existing.get();
        if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();   // đã hết hạn: coi như chưa có, sẽ bị xóa và tạo lại
        }
        if (!requestHash.equals(record.getRequestHash())) {
            throw new ConflictException("Idempotency-Key này đã được dùng cho một yêu cầu khác");
        }
        if (record.getStatus() != IdempotencyStatus.COMPLETED || record.getResponseBody() == null) {
            throw new ConflictException("Yêu cầu đang được xử lý, vui lòng thử lại sau");
        }
        return Optional.of(fromJson(record.getResponseBody(), responseType));
    }

    private void removeIfExpired(Long userId, String key) {
        idempotencyKeyRepository.findByUserIdAndIdempotencyKey(userId, key)
                .filter(k -> k.getExpiresAt() != null && k.getExpiresAt().isBefore(LocalDateTime.now()))
                .ifPresent(k -> {
                    idempotencyKeyRepository.delete(k);
                    idempotencyKeyRepository.flush();
                });
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không lưu được kết quả giao dịch", e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isTextual()) {   // phòng trường hợp JSON bị bọc thêm một lớp chuỗi
                node = objectMapper.readTree(node.asText());
            }
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được kết quả đã lưu", e);
        }
    }
}