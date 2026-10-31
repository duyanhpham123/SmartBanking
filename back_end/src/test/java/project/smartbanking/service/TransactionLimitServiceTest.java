package project.smartbanking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import project.smartbanking.dto.response.TransactionLimitResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.model.TransactionLimit;
import project.smartbanking.repository.TransactionLimitRepository;
import project.smartbanking.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionLimitServiceTest {

    @Mock private TransactionLimitRepository limitRepository;
    @Mock private UserRepository userRepository;

    private TransactionLimitService service;

    @BeforeEach
    void setUp() {
        service = new TransactionLimitService(limitRepository, userRepository);
        when(limitRepository.save(any(TransactionLimit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Chưa chỉnh lần nào thì dùng mặc định 200 triệu VND / 20.000 USD")
    void defaults() {
        assertThat(service.getDailyOutLimit("VND")).isEqualByComparingTo("200000000");
        assertThat(service.getDailyOutLimit("USD")).isEqualByComparingTo("20000");

        List<TransactionLimitResponse> all = service.getAll();
        assertThat(all).extracting(TransactionLimitResponse::currency).containsExactly("VND", "USD");
        assertThat(all.get(0).updatedAt()).isNull();
    }

    @Test
    @DisplayName("Admin đổi hạn mức: lưu giá trị mới và người chỉnh")
    void update() {
        TransactionLimitResponse res = service.update(1L, "vnd", new BigDecimal("300000000"));

        assertThat(res.currency()).isEqualTo("VND");
        assertThat(res.dailyOutLimit()).isEqualByComparingTo("300000000");
        assertThat(res.updatedAt()).isNotNull();
        verify(limitRepository).save(any(TransactionLimit.class));
    }

    @Test
    @DisplayName("Giá trị đã lưu được dùng thay cho mặc định")
    void storedValueWins() {
        TransactionLimit stored = new TransactionLimit();
        stored.setCurrency("USD");
        stored.setDailyOutLimit(new BigDecimal("5000"));
        when(limitRepository.findById("USD")).thenReturn(Optional.of(stored));

        assertThat(service.getDailyOutLimit("USD")).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Từ chối: loại tiền lạ, VND có phần lẻ, vượt mức tối đa")
    void rejectsInvalid() {
        assertThatThrownBy(() -> service.update(1L, "EUR", BigDecimal.TEN)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.update(1L, "VND", new BigDecimal("1000.5")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.update(1L, "VND", new BigDecimal("20000000000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tối đa");
        verify(limitRepository, never()).save(any());
    }
}
