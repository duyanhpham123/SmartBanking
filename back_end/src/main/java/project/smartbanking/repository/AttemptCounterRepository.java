package project.smartbanking.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import project.smartbanking.model.AttemptCounter;

@Repository
public interface AttemptCounterRepository extends JpaRepository<AttemptCounter, String> {

    // Tạo dòng nếu chưa có. INSERT IGNORE để hai server cùng tạo một lúc không bị lỗi trùng khóa.
    @Modifying
    @Query(value = "INSERT IGNORE INTO attempt_counters (id, failures, updated_at) VALUES (:id, 0, :now)",
            nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("now") LocalDateTime now);

    // Khóa dòng (SELECT ... FOR UPDATE) để các lần sai đồng thời được đếm lần lượt
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AttemptCounter c where c.id = :id")
    Optional<AttemptCounter> findByIdForUpdate(@Param("id") String id);

    @Modifying
    @Query("delete from AttemptCounter c where c.id = :id")
    int deleteByKey(@Param("id") String id);

    // Dọn các bộ đếm lâu không dùng (gọi từ job định kỳ)
    @Modifying
    @Query("delete from AttemptCounter c where c.updatedAt < :before")
    int deleteStale(@Param("before") LocalDateTime before);
}
