package project.smartbanking.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends AppException {

    public InsufficientBalanceException() {
        super(HttpStatus.BAD_REQUEST, "Số dư không đủ để thực hiện giao dịch");
    }

    public InsufficientBalanceException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}