package project.smartbanking.mapper;

import org.springframework.stereotype.Component;

import project.smartbanking.dto.response.AdminAccountResponse;
import project.smartbanking.dto.response.AdminTransactionResponse;
import project.smartbanking.model.Account;
import project.smartbanking.model.BankTransaction;
import project.smartbanking.model.User;

@Component
public class AdminMapper {

    // Phải gọi bên trong @Transactional vì account.getUser() là LAZY
    public AdminAccountResponse toAdminAccount(Account a) {
        User owner = a.getUser();
        return new AdminAccountResponse(
                a.getId(),
                a.getAccountNumber(),
                a.getAccountName(),
                a.getAccountType(),
                a.getCurrency(),
                a.getBalance(),
                a.getAvailableBalance(),
                a.getStatus(),
                a.getCreatedAt(),
                owner.getId(),
                owner.getUsername(),
                owner.getFullName());
    }

    // Nạp tiền không có tài khoản gửi, rút tiền không có tài khoản nhận nên có thể null
    public AdminTransactionResponse toAdminTransaction(BankTransaction t) {
        Account from = t.getFromAccount();
        Account to = t.getToAccount();
        return new AdminTransactionResponse(
                t.getId(),
                t.getTransactionCode(),
                t.getTransactionType(),
                t.getStatus(),
                t.getAmount(),
                t.getFee(),
                t.getCurrency(),
                t.getDescription(),
                t.getFailureReason(),
                from == null ? null : from.getAccountNumber(),
                from == null ? null : from.getUser().getUsername(),
                to == null ? null : to.getAccountNumber(),
                to == null ? null : to.getUser().getUsername(),
                t.getCreatedAt(),
                t.getCompletedAt());
    }
}