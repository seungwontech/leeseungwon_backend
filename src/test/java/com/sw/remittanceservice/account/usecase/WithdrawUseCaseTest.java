package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountDailyLimitUsage;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
import com.sw.remittanceservice.account.repository.*;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WithdrawUseCaseTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository accountTransactionRepository;

    @Mock
    private TransactionRedisRepository transactionRedisRepository;

    @Mock
    private AccountLimitSettingRepository accountLimitSettingRepository;

    @Mock
    private AccountDailyLimitUsageRepository accountDailyLimitUsageRepository;

    @InjectMocks
    private WithdrawUseCase withdrawUseCase;

    @Test
    @DisplayName("출금 성공")
    void withdraw_success() {

        String accountNo = UUID.randomUUID().toString();
        Long accountId = 1L;
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);


        // Given
        Account lockedAccount = new Account(accountId, accountNo, 50_000L, AccountStatus.ACTIVE, now, now);
        Account savedAccount = new Account(accountId, accountNo, 40_000L, AccountStatus.ACTIVE, now, now);
        AccountLimitSetting setting = mock(AccountLimitSetting.class);
        given(setting.getDailyWithdrawLimit()).willReturn(100_000L);
        AccountDailyLimitUsage usage = mock(AccountDailyLimitUsage.class);
        given(usage.getWithdrawUsed()).willReturn(0L);

        Transaction savedTransaction = Transaction.create(savedAccount, transactionRequestId, amount, TransactionType.WITHDRAW);
        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);
        given(accountRepository.findLockedByAccountNo(accountNo)).willReturn(Optional.of(lockedAccount));
        given(accountLimitSettingRepository.findByAccountId(accountId)).willReturn(Optional.of(setting));

        // 존재한 경우
        given(accountDailyLimitUsageRepository.findLockedByAccountIdAndLimitDate(accountId, today)).willReturn(Optional.of(usage));

        given(accountRepository.save(any(Account.class))).willReturn(savedAccount);
        given(accountTransactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);

        // When
        TransactionResponse response = withdrawUseCase.execute(accountNo, amount, transactionRequestId);

        // Then
        assertThat(response.transactionStatus()).isEqualTo("SUCCESS");
        assertThat(response.balanceAfterTransaction()).isEqualTo(savedAccount.getBalance());

    }

    @Test
    @DisplayName("출금 실패 - 계좌 없음")
    void withdraw_fail_account_not_found() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();

        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);
        given(accountRepository.findLockedByAccountNo(accountNo)).willReturn(Optional.empty());

        // When/Then
        CoreException e = assertThrows(CoreException.class,
                () -> withdrawUseCase.execute(accountNo, amount, transactionRequestId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_FOUND);
    }


    @Test
    @DisplayName("출금 실패 - 계좌 제한 설정 없음")
    void withdraw_fail_limit_setting_not_found() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long accountId = 1L;
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        Account lockedAccount = new Account(accountId, accountNo, 50_000L, AccountStatus.ACTIVE, now, now);

        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);
        given(accountRepository.findLockedByAccountNo(accountNo)).willReturn(Optional.of(lockedAccount));
        given(accountLimitSettingRepository.findByAccountId(accountId)).willReturn(Optional.empty());

        // When/Then
        CoreException e = assertThrows(CoreException.class,
                () -> withdrawUseCase.execute(accountNo, amount, transactionRequestId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_LIMIT_SETTING_NOT_FOUND);
    }

    @Test
    @DisplayName("출금 실패 - 일일 출금 한도 초과")
    void withdraw_fail_exceed_daily_withdraw_limit() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long accountId = 1L;
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        Account lockedAccount = new Account(accountId, accountNo, 50_000L, AccountStatus.ACTIVE, now, now);


        AccountLimitSetting setting = mock(AccountLimitSetting.class);
        given(setting.getDailyWithdrawLimit()).willReturn(15_000L);

        AccountDailyLimitUsage usage = mock(AccountDailyLimitUsage.class);
        given(usage.getWithdrawUsed()).willReturn(10_000L); // 이미 1만 사용

        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);
        given(accountRepository.findLockedByAccountNo(accountNo)).willReturn(Optional.of(lockedAccount));
        given(accountLimitSettingRepository.findByAccountId(accountId)).willReturn(Optional.of(setting));
        given(accountDailyLimitUsageRepository.findLockedByAccountIdAndLimitDate(accountId, today)).willReturn(Optional.of(usage));

        // When/Then
        CoreException e = assertThrows(CoreException.class,
                () -> withdrawUseCase.execute(accountNo, amount, transactionRequestId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.EXCEED_DAILY_WITHDRAW_LIMIT);
    }



    @Test
    @DisplayName("출금 성공 - 일일 사용량이 없으면 생성 후 사용량 누적")
    void withdraw_success_creates_usage_when_missing() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long accountId = 1L;
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        Account lockedAccount = new Account(
                accountId,
                accountNo,
                50_000L,
                AccountStatus.ACTIVE,
                now,
                now
        );

        Account savedAccount = new Account(
                accountId,
                accountNo,
                40_000L,
                AccountStatus.ACTIVE,
                lockedAccount.getCreatedAt(),
                now
        );

        AccountLimitSetting setting = mock(AccountLimitSetting.class);
        given(setting.getDailyWithdrawLimit()).willReturn(100_000L);

        AccountDailyLimitUsage newUsage = mock(AccountDailyLimitUsage.class);
        given(newUsage.getWithdrawUsed()).willReturn(0L);

        Transaction savedTransaction = Transaction.create(savedAccount, transactionRequestId, amount, TransactionType.WITHDRAW);

        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);
        given(accountRepository.findLockedByAccountNo(accountNo)).willReturn(Optional.of(lockedAccount));
        given(accountLimitSettingRepository.findByAccountId(accountId)).willReturn(Optional.of(setting));

        // 존재 하지 않음
        given(accountDailyLimitUsageRepository.findLockedByAccountIdAndLimitDate(accountId, today)).willReturn(Optional.empty());
        given(accountDailyLimitUsageRepository.save(any(AccountDailyLimitUsage.class))).willReturn(newUsage);

        given(accountRepository.save(any(Account.class))).willReturn(savedAccount);
        given(accountTransactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);

        // When
        TransactionResponse response = withdrawUseCase.execute(accountNo, amount, transactionRequestId);

        // Then
        assertThat(response.transactionStatus()).isEqualTo("SUCCESS");
        assertThat(response.balanceAfterTransaction()).isEqualTo(savedAccount.getBalance());

        verify(accountDailyLimitUsageRepository, times(1)).findLockedByAccountIdAndLimitDate(accountId, today);
        verify(accountDailyLimitUsageRepository, times(1)).save(any(AccountDailyLimitUsage.class));
    }

    @Test
    @DisplayName("출금 실패 - 해지된 계좌인 경우 예외 발생")
    void withdraw_fail_account_closed() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long accountId = 1L;
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        // CLOSED 상태의 계좌 생성
        Account closedAccount = new Account(
                accountId,
                accountNo,
                100_000L,
                AccountStatus.CLOSED, // 해지된 상태
                now,
                now
        );

        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);
        given(accountRepository.findLockedByAccountNo(accountNo)).willReturn(Optional.of(closedAccount));

        // When/Then
        CoreException e = assertThrows(CoreException.class,
                () -> withdrawUseCase.execute(accountNo, amount, transactionRequestId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_ACTIVE);

        // 계좌가 해지되었으므로 출금 및 트랜잭션 저장이 발생하면 안 됨
        verify(accountRepository, never()).save(any());
        verify(accountTransactionRepository, never()).save(any());
        verify(accountDailyLimitUsageRepository, never()).save(any());
    }
}