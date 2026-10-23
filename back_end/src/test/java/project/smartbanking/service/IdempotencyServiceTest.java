package project.smartbanking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.ConflictException;
import project.smartbanking.model.IdempotencyKey;
import project.smartbanking.model.enums.IdempotencyStatus;
import project.smartbanking.repository.IdempotencyKeyRepository;
import project.smartbanking.repository.TransactionRepository;
import project.smartbanking.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdempotencyServiceTest {

    // Kết quả giả dùng để thử nghiệm (thay cho TransactionResponse)
    public record Result(Long id, String note) {
    }

    private static final Long USER_ID = 3L;
    private static final String KEY = "test-key-0001";
    private static final String HASH = "hash-A";
    private static final String STORED_JSON = "{\"id\":55,\"note\":\"ok\"}";

    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private IdempotencyService service;
    private int actionCalls;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(idempotencyKeyRepository, userRepository,
                transactionRepository, transactionTemplate, objectMapper);
        // Mô phỏng transaction: chạy thẳng đoạn code bên trong
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });
        actionCalls = 0;
    }

    // ===================== MÃ HỢP LỆ =====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "abc", "has space in key", "key/with/slash" })
    @DisplayName("Idempotency-Key sai định dạng bị từ chối, không chạy nghiệp vụ")
    void invalidKeyRejected(String key) {
        assertThatThrownBy(() -> service.execute(USER_ID, key, HASH, Result.class, Result::id,
                action(new Result(1L, "x"))))
                .isInstanceOf(BadRequestException.class);

        assertThat(actionCalls).isZero();
    }

    @Test
    @DisplayName("Idempotency-Key dài quá 100 ký tự bị từ chối")
    void tooLongKeyRejected() {
        String key = "a".repeat(101);

        assertThatThrownBy(() -> service.execute(USER_ID, key, HASH, Result.class, Result::id,
                action(new Result(1L, "x"))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Dấu vân tay request: giống nhau khi cùng nội dung, khác nhau khi đổi nội dung")
    void fingerprint() {
        String a1 = service.fingerprint(1L, "2222", "100000", "an trua");
        String a2 = service.fingerprint(1L, "2222", "100000", "an trua");
        String b = service.fingerprint(1L, "2222", "200000", "an trua");

        assertThat(a1).isEqualTo(a2).hasSize(64);
        assertThat(a1).isNotEqualTo(b);
    }

    // ===================== MÃ MỚI =====================

    @Test
    @DisplayName("Mã mới: chạy nghiệp vụ đúng một lần và lưu kết quả ở trạng thái COMPLETED")
    void newKey_runsActionOnceAndStoresResult() {
        IdempotencyService.IdempotentResult<Result> result = service.execute(
                USER_ID, KEY, HASH, Result.class, Result::id, action(new Result(55L, "ok")));

        assertThat(result.replayed()).isFalse();
        assertThat(result.response()).isEqualTo(new Result(55L, "ok"));
        assertThat(actionCalls).isEqualTo(1);

        verify(idempotencyKeyRepository).saveAndFlush(any(IdempotencyKey.class));
        ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(idempotencyKeyRepository).save(captor.capture());
        IdempotencyKey stored = captor.getValue();
        assertThat(stored.getIdempotencyKey()).isEqualTo(KEY);
        assertThat(stored.getRequestHash()).isEqualTo(HASH);
        assertThat(stored.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(stored.getResponseBody()).contains("\"note\":\"ok\"");
        assertThat(stored.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
        verify(transactionRepository).getReferenceById(55L);
    }

    @Test
    @DisplayName("Nghiệp vụ lỗi thì lỗi được ném ra và kết quả không được lưu là COMPLETED")
    void actionFails_propagatesAndDoesNotComplete() {
        Supplier<Result> failing = () -> {
            actionCalls++;
            throw new BadRequestException("Số dư không đủ");
        };

        assertThatThrownBy(() -> service.execute(USER_ID, KEY, HASH, Result.class, Result::id, failing))
                .isInstanceOf(BadRequestException.class);

        assertThat(actionCalls).isEqualTo(1);
        verify(idempotencyKeyRepository, never()).save(any(IdempotencyKey.class));
    }

    // ===================== GỬI LẠI =====================

    @Test
    @DisplayName("Gửi lại đúng mã và đúng nội dung: trả kết quả cũ, không chạy nghiệp vụ lần nữa")
    void sameKeySameRequest_replaysStoredResult() {
        IdempotencyKey stored = record(HASH, IdempotencyStatus.COMPLETED, STORED_JSON, LocalDateTime.now().plusHours(1));
        when(idempotencyKeyRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY)).thenReturn(Optional.of(stored));

        IdempotencyService.IdempotentResult<Result> result = service.execute(
                USER_ID, KEY, HASH, Result.class, Result::id, action(new Result(99L, "không được dùng")));

        assertThat(result.replayed()).isTrue();
        assertThat(result.response()).isEqualTo(new Result(55L, "ok"));
        assertThat(actionCalls).isZero();
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    @DisplayName("Cùng mã nhưng nội dung khác thì báo xung đột và không chạy nghiệp vụ")
    void sameKeyDifferentRequest_conflict() {
        IdempotencyKey stored = record(HASH, IdempotencyStatus.COMPLETED, STORED_JSON, LocalDateTime.now().plusHours(1));
        when(idempotencyKeyRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.execute(USER_ID, KEY, "hash-B", Result.class, Result::id,
                action(new Result(99L, "x"))))
                .isInstanceOf(ConflictException.class);

        assertThat(actionCalls).isZero();
    }

    @Test
    @DisplayName("Mã đang được xử lý (chưa có kết quả) thì báo xung đột")
    void keyStillProcessing_conflict() {
        IdempotencyKey stored = record(HASH, IdempotencyStatus.PROCESSING, null, LocalDateTime.now().plusHours(1));
        when(idempotencyKeyRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.execute(USER_ID, KEY, HASH, Result.class, Result::id,
                action(new Result(99L, "x"))))
                .isInstanceOf(ConflictException.class);

        assertThat(actionCalls).isZero();
    }

    @Test
    @DisplayName("Mã đã hết hạn được coi như chưa có: xóa bản cũ và chạy nghiệp vụ mới")
    void expiredKey_isReplaced() {
        IdempotencyKey expired = record(HASH, IdempotencyStatus.COMPLETED, STORED_JSON, LocalDateTime.now().minusHours(1));
        when(idempotencyKeyRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY)).thenReturn(Optional.of(expired));

        IdempotencyService.IdempotentResult<Result> result = service.execute(
                USER_ID, KEY, HASH, Result.class, Result::id, action(new Result(77L, "mới")));

        assertThat(result.replayed()).isFalse();
        assertThat(result.response()).isEqualTo(new Result(77L, "mới"));
        assertThat(actionCalls).isEqualTo(1);
        verify(idempotencyKeyRepository).delete(expired);
    }

    @Test
    @DisplayName("Hai request cùng mã đua nhau: bên thua nhận lại kết quả của bên thắng, không chạy nghiệp vụ")
    void race_loserReceivesWinnersResult() {
        IdempotencyKey winner = record(HASH, IdempotencyStatus.COMPLETED, STORED_JSON, LocalDateTime.now().plusHours(1));
        // Lần tra cứu 1 và 2 chưa thấy gì; sau khi ghi bị trùng thì lần 3 thấy kết quả của bên thắng
        when(idempotencyKeyRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(winner));
        when(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        IdempotencyService.IdempotentResult<Result> result = service.execute(
                USER_ID, KEY, HASH, Result.class, Result::id, action(new Result(99L, "không được dùng")));

        assertThat(result.replayed()).isTrue();
        assertThat(result.response()).isEqualTo(new Result(55L, "ok"));
        assertThat(actionCalls).isZero();
    }

    // ===================== HÀM PHỤ =====================

    private Supplier<Result> action(Result result) {
        return () -> {
            actionCalls++;
            return result;
        };
    }

    private IdempotencyKey record(String hash, IdempotencyStatus status, String body, LocalDateTime expiresAt) {
        IdempotencyKey k = new IdempotencyKey();
        k.setIdempotencyKey(KEY);
        k.setRequestHash(hash);
        k.setStatus(status);
        k.setResponseBody(body);
        k.setExpiresAt(expiresAt);
        return k;
    }
}