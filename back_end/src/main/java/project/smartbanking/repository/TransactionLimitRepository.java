package project.smartbanking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.TransactionLimit;

@Repository
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, String> {
}
