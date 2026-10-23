package project.smartbanking.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends AppException {

    public InvalidOtpException() {
        super(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ hoặc đã hết hạn");
    }
}