package project.smartbanking.mapper;

import java.util.Collection;

import org.springframework.stereotype.Component;

import project.smartbanking.dto.response.TransactionResponse;
import project.smartbanking.model.BankTransaction;
import project.smartbanking.model.enums.TransactionDirection;

@Component
public class TransactionMapper {

    // viewerAccountIds: các tài khoản của người đang xem.
    // Tài khoản gửi thuộc về họ thì là DEBIT (tiền ra), còn lại là CREDIT (tiền vào).
    // Phải gọi bên trong @Transactional vì fromAccount/toAccount là LAZY.
    public TransactionResponse toResponse(BankTransaction t, Collection<Long> viewerAccountIds) {
        boolean fromMine = t.getFromAccount() != null
                && viewerAccountIds.contains(t.getFromAccount().getId());

        return new TransactionResponse(
                t.getId(),
                t.getTransactionCode(),
                t.getTransactionType(),
                t.getStatus(),
                fromMine ? TransactionDirection.DEBIT : TransactionDirection.CREDIT,
                t.getAmount(),
                t.getFee(),
                t.getCurrency(),
                t.getDescription(),
                t.getFromAccount() == null ? null : t.getFromAccount().getAccountNumber(),
                t.getToAccount() == null ? null : t.getToAccount().getAccountNumber(),
                t.getCreatedAt(),
                t.getCompletedAt());
    }
}