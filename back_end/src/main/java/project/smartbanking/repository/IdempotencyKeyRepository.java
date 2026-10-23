package project.smartbanking.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.IdempotencyKey;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    // Tìm key đã gửi trước đó của đúng người dùng
    Optional<IdempotencyKey> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    boolean existsByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    // Dọn các key đã hết hạn (gọi từ một job định kỳ)
    @Modifying
    @Query("delete from IdempotencyKey k where k.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}