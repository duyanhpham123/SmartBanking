package project.smartbanking.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import project.smartbanking.model.AttemptCounter;
import project.smartbanking.repository.AttemptCounterRepository;

// AttemptLimiter thật, nhưng bảng attempt_counters được thay bằng một HashMap để test không cần MySQL
final class InMemoryAttemptLimiter {

    private InMemoryAttemptLimiter() {
    }

    static AttemptLimiter create() {
        return new AttemptLimiter(repository(new HashMap<>()));
    }

    static AttemptCounterRepository repository(Map<String, AttemptCounter> rows) {
        AttemptCounterRepository repo = mock(AttemptCounterRepository.class);
        when(repo.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(rows.get(inv.<String>getArgument(0))));
        when(repo.findByIdForUpdate(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(rows.get(inv.<String>getArgument(0))));
        when(repo.insertIfAbsent(anyString(), any())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            if (rows.containsKey(id)) {
                return 0;
            }
            AttemptCounter c = new AttemptCounter();
            c.setId(id);
            c.setUpdatedAt(inv.<LocalDateTime>getArgument(1));
            rows.put(id, c);
            return 1;
        });
        when(repo.save(any(AttemptCounter.class))).thenAnswer(inv -> {
            AttemptCounter c = inv.getArgument(0);
            rows.put(c.getId(), c);
            return c;
        });
        when(repo.deleteByKey(anyString())).thenAnswer(inv -> rows.remove(inv.<String>getArgument(0)) != null ? 1 : 0);
        return repo;
    }
}
