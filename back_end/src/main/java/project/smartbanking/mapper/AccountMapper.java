package project.smartbanking.mapper;

import org.springframework.stereotype.Component;

import project.smartbanking.dto.response.AccountLookupResponse;
import project.smartbanking.dto.response.AccountResponse;
import project.smartbanking.model.Account;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountName(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                account.getAvailableBalance(),
                account.getStatus(),
                account.getCreatedAt());
    }

    // Phải gọi bên trong @Transactional vì account.getUser() là LAZY
    public AccountLookupResponse toLookupResponse(Account account) {
        return new AccountLookupResponse(
                account.getAccountNumber(),
                account.getUser().getFullName(),
                account.getCurrency());
    }
}