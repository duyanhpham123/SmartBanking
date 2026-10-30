package project.smartbanking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import project.smartbanking.dto.request.DepositRequest;
import project.smartbanking.dto.request.PaymentRequest;
import project.smartbanking.dto.request.TransferRequest;
import project.smartbanking.dto.request.WithdrawRequest;
import project.smartbanking.dto.response.TransactionResponse;
import project.smartbanking.event.TransactionCompletedEvent;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.InsufficientBalanceException;
import project.smartbanking.exception.InvalidOtpException;
import project.smartbanking.exception.ResourceNotFoundException;
import project.smartbanking.mapper.TransactionMapper;
import project.smartbanking.model.Account;
import project.smartbanking.model.BankTransaction;
import project.smartbanking.model.User;
import project.smartbanking.model.enums.AccountStatus;
import project.smartbanking.model.enums.BillProvider;
import project.smartbanking.model.enums.OtpPurpose;
import project.smartbanking.model.enums.PaymentMethod;
import project.smartbanking.model.enums.TransactionDirection;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;
import project.smartbanking.repository.AccountRepository;
import project.smartbanking.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private OtpService otpService;

    private TransactionService service;
    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        service = new TransactionService(accountRepository, transactionRepository,
                new TransactionMapper(), eventPublisher, otpService);
        // save() trả lại chính đối tượng được lưu, giống hành vi thật
        when(transactionRepository.save(any(BankTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        owner = user(USER_ID);
        other = user(OTHER_USER_ID);
    }

    // ===================== NẠP TIỀN =====================

    @Test
    @DisplayName("Nạp tiền: cộng đúng số dư, ghi giao dịch DEPOSIT, không cần OTP")
    void deposit_success() {
        Account account = account(1L, owner, "1111111111111111", "VND", "1000000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        TransactionResponse response = service.deposit(USER_ID,
                new DepositRequest(1L, new BigDecimal("500000"), PaymentMethod.BANK_TRANSFER));

        assertThat(account.getBalance()).isEqualByComparingTo("1500000");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("1500000");
        assertThat(response.transactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(response.direction()).isEqualTo(TransactionDirection.CREDIT);
        assertThat(response.fromAccountNumber()).isNull();
        assertThat(response.toAccountNumber()).isEqualTo("1111111111111111");
        assertThat(response.amount()).isEqualByComparingTo("500000");
        assertThat(response.transactionCode()).startsWith("TXN");
        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("Nạp tiền vào tài khoản bị khóa thì bị từ chối")
    void deposit_lockedAccount() {
        Account account = account(1L, owner, "1111111111111111", "VND", "0");
        account.setStatus(AccountStatus.LOCKED);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.deposit(USER_ID,
                new DepositRequest(1L, new BigDecimal("100000"), PaymentMethod.ATM_CARD)))
                .isInstanceOf(BadRequestException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("0");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nạp vào tài khoản của người khác báo không tìm thấy")
    void deposit_notOwner() {
        Account account = account(1L, other, "1111111111111111", "VND", "0");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.deposit(USER_ID,
                new DepositRequest(1L, new BigDecimal("100000"), PaymentMethod.ATM_CARD)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Nạp dưới mức tối thiểu 10.000 VND bị từ chối")
    void deposit_belowMinimum() {
        Account account = account(1L, owner, "1111111111111111", "VND", "0");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.deposit(USER_ID,
                new DepositRequest(1L, new BigDecimal("5000"), PaymentMethod.ATM_CARD)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tối thiểu");
    }

    @Test
    @DisplayName("Nạp trên mức tối đa 500.000.000 VND bị từ chối")
    void deposit_aboveMaximum() {
        Account account = account(1L, owner, "1111111111111111", "VND", "0");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.deposit(USER_ID,
                new DepositRequest(1L, new BigDecimal("500000001"), PaymentMethod.ATM_CARD)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tối đa");
    }

    @Test
    @DisplayName("Tiền VND không được có phần thập phân")
    void deposit_vndWithDecimals() {
        Account account = account(1L, owner, "1111111111111111", "VND", "0");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.deposit(USER_ID,
                new DepositRequest(1L, new BigDecimal("1000000.5"), PaymentMethod.ATM_CARD)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("số nguyên");
    }

    // ===================== RÚT TIỀN =====================

    @Test
    @DisplayName("Rút tiền: trừ đúng số dư, ghi giao dịch WITHDRAW và phát sự kiện thông báo")
    void withdraw_success() {
        Account account = account(1L, owner, "1111111111111111", "VND", "1000000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        TransactionResponse response = service.withdraw(USER_ID,
                new WithdrawRequest(1L, new BigDecimal("300000"), PaymentMethod.E_WALLET));

        assertThat(account.getBalance()).isEqualByComparingTo("700000");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("700000");
        assertThat(response.transactionType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(response.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(response.toAccountNumber()).isNull();

        ArgumentCaptor<TransactionCompletedEvent> captor = ArgumentCaptor.forClass(TransactionCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(captor.getValue().fromUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().toUserId()).isNull();
    }

    @Test
    @DisplayName("Được rút hết đúng bằng số dư (về 0)")
    void withdraw_exactBalance() {
        Account account = account(1L, owner, "1111111111111111", "VND", "250000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        service.withdraw(USER_ID, new WithdrawRequest(1L, new BigDecimal("250000"), PaymentMethod.BANK_TRANSFER));

        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Rút quá số dư thì báo lỗi, số dư không đổi, không ghi giao dịch, không phát sự kiện")
    void withdraw_insufficientBalance() {
        Account account = account(1L, owner, "1111111111111111", "VND", "100000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.withdraw(USER_ID,
                new WithdrawRequest(1L, new BigDecimal("200000"), PaymentMethod.BANK_TRANSFER)))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100000");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("100000");
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Rút bằng thẻ ATM không được hỗ trợ và không chạm vào tài khoản")
    void withdraw_atmCardRejected() {
        assertThatThrownBy(() -> service.withdraw(USER_ID,
                new WithdrawRequest(1L, new BigDecimal("100000"), PaymentMethod.ATM_CARD)))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(accountRepository);
    }

    @Test
    @DisplayName("Rút trên mức tối đa 50.000.000 VND bị từ chối dù đủ số dư")
    void withdraw_aboveMaximum() {
        Account account = account(1L, owner, "1111111111111111", "VND", "100000000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.withdraw(USER_ID,
                new WithdrawRequest(1L, new BigDecimal("50000001"), PaymentMethod.BANK_TRANSFER)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tối đa");

        assertThat(account.getBalance()).isEqualByComparingTo("100000000");
    }

    @Test
    @DisplayName("Đã ra 180 triệu trong ngày thì rút thêm 30 triệu vượt hạn mức 200 triệu/ngày bị từ chối")
    void withdraw_dailyLimitExceeded() {
        Account account = account(1L, owner, "1111111111111111", "VND", "500000000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.sumOutgoingSince(eq(1L), any())).thenReturn(new BigDecimal("180000000"));

        assertThatThrownBy(() -> service.withdraw(USER_ID,
                new WithdrawRequest(1L, new BigDecimal("30000000"), PaymentMethod.BANK_TRANSFER)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("hạn mức")
                .hasMessageContaining("20000000");

        assertThat(account.getBalance()).isEqualByComparingTo("500000000");
        verify(transactionRepository, never()).save(any());
    }

    // ===================== CHUYỂN TIỀN =====================

    @Test
    @DisplayName("Chuyển tiền: trừ bên gửi, cộng bên nhận, ghi đủ hai tài khoản, phát sự kiện")
    void transfer_success() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        Account to = account(2L, other, "2222222222222222", "VND", "1000000");
        stubTransferAccounts(from, to);

        TransactionResponse response = service.transfer(USER_ID, transfer(from, to, "2000000"));

        assertThat(from.getBalance()).isEqualByComparingTo("3000000");
        assertThat(from.getAvailableBalance()).isEqualByComparingTo("3000000");
        assertThat(to.getBalance()).isEqualByComparingTo("3000000");
        assertThat(to.getAvailableBalance()).isEqualByComparingTo("3000000");
        assertThat(response.transactionType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(response.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(response.amount()).isEqualByComparingTo("2000000");
        assertThat(response.fee()).isEqualByComparingTo("0");
        assertThat(response.fromAccountNumber()).isEqualTo("1111111111111111");
        assertThat(response.toAccountNumber()).isEqualTo("2222222222222222");
        assertThat(response.description()).isEqualTo("Tien an trua");

        ArgumentCaptor<TransactionCompletedEvent> captor = ArgumentCaptor.forClass(TransactionCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(captor.getValue().fromUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().toUserId()).isEqualTo(OTHER_USER_ID);
    }

    @Test
    @DisplayName("Tổng tiền hai tài khoản không đổi sau khi chuyển (tiền không tự sinh ra hay mất đi)")
    void transfer_conservesTotalMoney() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        Account to = account(2L, other, "2222222222222222", "VND", "1000000");
        stubTransferAccounts(from, to);
        BigDecimal before = from.getBalance().add(to.getBalance());

        service.transfer(USER_ID, transfer(from, to, "1234567"));

        assertThat(from.getBalance().add(to.getBalance())).isEqualByComparingTo(before);
    }

    @Test
    @DisplayName("Số tài khoản nhận có dấu cách vẫn tìm đúng")
    void transfer_accountNumberWithSpaces() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        Account to = account(2L, other, "2222333344445555", "VND", "0");
        stubTransferAccounts(from, to);

        service.transfer(USER_ID, new TransferRequest(1L, "2222 3333 4444 5555",
                new BigDecimal("100000"), null, "123456"));

        assertThat(to.getBalance()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Bỏ trống nội dung thì mặc định là 'Chuyển tiền'")
    void transfer_blankDescriptionDefaults() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        Account to = account(2L, other, "2222222222222222", "VND", "0");
        stubTransferAccounts(from, to);

        TransactionResponse response = service.transfer(USER_ID, new TransferRequest(
                1L, "2222222222222222", new BigDecimal("100000"), "   ", "123456"));

        assertThat(response.description()).isEqualTo("Chuyển tiền");
    }

    @Test
    @DisplayName("OTP sai bị từ chối ngay từ đầu, chưa đụng đến tài khoản nào")
    void transfer_invalidOtpStopsEverything() {
        doThrow(new InvalidOtpException()).when(otpService)
                .verifyAndConsume(eq(USER_ID), eq(OtpPurpose.TRANSFER), eq("000000"));

        assertThatThrownBy(() -> service.transfer(USER_ID, new TransferRequest(
                1L, "2222222222222222", new BigDecimal("100000"), null, "000000")))
                .isInstanceOf(InvalidOtpException.class);

        verifyNoInteractions(accountRepository);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chuyển tiền dùng OTP mục đích TRANSFER của đúng người dùng")
    void transfer_consumesOtp() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        Account to = account(2L, other, "2222222222222222", "VND", "0");
        stubTransferAccounts(from, to);

        service.transfer(USER_ID, transfer(from, to, "100000"));

        verify(otpService).verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "123456");
    }

    @Test
    @DisplayName("Tài khoản nguồn không thuộc người dùng thì báo không tìm thấy, không khóa dòng nào")
    void transfer_sourceNotOwned() {
        when(accountRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(USER_ID, new TransferRequest(
                1L, "2222222222222222", new BigDecimal("100000"), null, "123456")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    @DisplayName("Số tài khoản nhận không tồn tại thì báo không tìm thấy")
    void transfer_targetNotFound() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        when(accountRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("9999999999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(USER_ID, new TransferRequest(
                1L, "9999999999999999", new BigDecimal("100000"), null, "123456")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Số tài khoản nhận sai định dạng bị từ chối")
    void transfer_invalidAccountNumber() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        when(accountRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(from));

        assertThatThrownBy(() -> service.transfer(USER_ID, new TransferRequest(
                1L, "abc", new BigDecimal("100000"), null, "123456")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Không được chuyển cho chính tài khoản nguồn")
    void transfer_sameAccount() {
        Account account = account(1L, owner, "1111111111111111", "VND", "5000000");
        when(accountRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumber("1111111111111111")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.transfer(USER_ID, new TransferRequest(
                1L, "1111111111111111", new BigDecimal("100000"), null, "123456")))
                .isInstanceOf(BadRequestException.class);

        verify(accountRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    @DisplayName("Chưa hỗ trợ chuyển giữa hai loại tiền tệ khác nhau")
    void transfer_differentCurrency() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        Account to = account(2L, other, "2222222222222222", "USD", "0");
        stubTransferAccounts(from, to);

        assertThatThrownBy(() -> service.transfer(USER_ID, transfer(from, to, "100000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cùng loại tiền tệ");

        assertThat(from.getBalance()).isEqualByComparingTo("5000000");
        assertThat(to.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Tài khoản nhận bị khóa thì không nhận được tiền")
    void transfer_targetLocked() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        Account to = account(2L, other, "2222222222222222", "VND", "0");
        to.setStatus(AccountStatus.LOCKED);
        stubTransferAccounts(from, to);

        assertThatThrownBy(() -> service.transfer(USER_ID, transfer(from, to, "100000")))
                .isInstanceOf(BadRequestException.class);

        assertThat(from.getBalance()).isEqualByComparingTo("5000000");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản nguồn bị khóa thì không chuyển được")
    void transfer_sourceLocked() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        from.setStatus(AccountStatus.LOCKED);
        Account to = account(2L, other, "2222222222222222", "VND", "0");
        stubTransferAccounts(from, to);

        assertThatThrownBy(() -> service.transfer(USER_ID, transfer(from, to, "100000")))
                .isInstanceOf(BadRequestException.class);

        assertThat(to.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Chuyển quá số dư thì báo lỗi, hai bên không đổi, không ghi giao dịch")
    void transfer_insufficientBalance() {
        Account from = account(1L, owner, "1111111111111111", "VND", "1000000");
        Account to = account(2L, other, "2222222222222222", "VND", "500000");
        stubTransferAccounts(from, to);

        assertThatThrownBy(() -> service.transfer(USER_ID, transfer(from, to, "2000000")))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(from.getBalance()).isEqualByComparingTo("1000000");
        assertThat(to.getBalance()).isEqualByComparingTo("500000");
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Chuyển dưới mức tối thiểu 1.000 VND bị từ chối")
    void transfer_belowMinimum() {
        Account from = account(1L, owner, "1111111111111111", "VND", "5000000");
        Account to = account(2L, other, "2222222222222222", "VND", "0");
        stubTransferAccounts(from, to);

        assertThatThrownBy(() -> service.transfer(USER_ID, transfer(from, to, "500")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tối thiểu");
    }

    @Test
    @DisplayName("Khóa hai tài khoản theo thứ tự id tăng dần khi gửi từ id lớn sang id nhỏ (chống deadlock)")
    void transfer_locksInAscendingOrder_fromHigherId() {
        assertLocksInAscendingIdOrder(5L, 2L);
    }

    @Test
    @DisplayName("Khóa hai tài khoản theo thứ tự id tăng dần khi gửi từ id nhỏ sang id lớn")
    void transfer_locksInAscendingOrder_fromLowerId() {
        assertLocksInAscendingIdOrder(2L, 5L);
    }

    // ===================== THANH TOÁN HÓA ĐƠN =====================

    @Test
    @DisplayName("Thanh toán: trừ tiền, giao dịch PAYMENT không có tài khoản nhận, mã khách hàng viết hoa")
    void pay_success() {
        Account account = account(1L, owner, "1111111111111111", "VND", "1000000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        TransactionResponse response = service.pay(USER_ID, new PaymentRequest(
                1L, BillProvider.ELECTRICITY, "pe12345678", new BigDecimal("500000"), "123456"));

        assertThat(account.getBalance()).isEqualByComparingTo("500000");
        assertThat(response.transactionType()).isEqualTo(TransactionType.PAYMENT);
        assertThat(response.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(response.toAccountNumber()).isNull();
        assertThat(response.description()).contains("PE12345678");
        verify(otpService).verifyAndConsume(USER_ID, OtpPurpose.TRANSFER, "123456");
    }

    @Test
    @DisplayName("Chỉ thanh toán hóa đơn bằng tài khoản VND")
    void pay_usdAccountRejected() {
        Account account = account(1L, owner, "1111111111111111", "USD", "5000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.pay(USER_ID, new PaymentRequest(
                1L, BillProvider.WATER, "PE12345678", new BigDecimal("100"), "123456")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("VND");

        assertThat(account.getBalance()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Thanh toán quá số dư thì báo lỗi, số dư không đổi")
    void pay_insufficientBalance() {
        Account account = account(1L, owner, "1111111111111111", "VND", "100000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.pay(USER_ID, new PaymentRequest(
                1L, BillProvider.INTERNET, "PE12345678", new BigDecimal("200000"), "123456")))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100000");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thanh toán với OTP sai bị từ chối trước khi đụng đến tài khoản")
    void pay_invalidOtp() {
        doThrow(new InvalidOtpException()).when(otpService)
                .verifyAndConsume(eq(USER_ID), eq(OtpPurpose.TRANSFER), eq("000000"));

        assertThatThrownBy(() -> service.pay(USER_ID, new PaymentRequest(
                1L, BillProvider.MOBILE, "PE12345678", new BigDecimal("100000"), "000000")))
                .isInstanceOf(InvalidOtpException.class);

        verifyNoInteractions(accountRepository);
    }

    // ===================== HÀM PHỤ =====================

    private void assertLocksInAscendingIdOrder(long fromId, long toId) {
        Account from = account(fromId, owner, "1111111111111111", "VND", "5000000");
        Account to = account(toId, other, "2222222222222222", "VND", "0");
        stubTransferAccounts(from, to);

        service.transfer(USER_ID, transfer(from, to, "100000"));

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByIdForUpdate(Math.min(fromId, toId));
        order.verify(accountRepository).findByIdForUpdate(Math.max(fromId, toId));
    }

    private void stubTransferAccounts(Account from, Account to) {
        when(accountRepository.findByIdAndUserId(from.getId(), USER_ID)).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber(to.getAccountNumber())).thenReturn(Optional.of(to));
        when(accountRepository.findByIdForUpdate(from.getId())).thenReturn(Optional.of(from));
        when(accountRepository.findByIdForUpdate(to.getId())).thenReturn(Optional.of(to));
    }

    private TransferRequest transfer(Account from, Account to, String amount) {
        return new TransferRequest(from.getId(), to.getAccountNumber(),
                new BigDecimal(amount), "Tien an trua", "123456");
    }

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        return u;
    }

    private Account account(Long id, User owner, String number, String currency, String balance) {
        Account a = new Account();
        a.setId(id);
        a.setUser(owner);
        a.setAccountNumber(number);
        a.setCurrency(currency);
        a.setBalance(new BigDecimal(balance));
        a.setAvailableBalance(new BigDecimal(balance));
        a.setStatus(AccountStatus.ACTIVE);
        return a;
    }
}