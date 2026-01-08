package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransferResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountDailyLimitUsage;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.repository.*;
import com.sw.remittanceservice.account.usecase.policy.FeeCalculatorFinder;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
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
class TransferUseCaseTest {

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

    @Mock
    private FeeCalculatorFinder feeCalculatorFinder;

    @InjectMocks
    private TransferUseCase transferUseCase;

    @Test
    @DisplayName("이체 실패 - 동일 계좌 이체 시 예외 발생")
    void transfer_fail_same_account() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long amount = 10_000L;
        String txRequestId = UUID.randomUUID().toString();

        // When & Then
        CoreException e = assertThrows(CoreException.class,
                () -> transferUseCase.execute(accountNo, accountNo, amount, txRequestId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.SAME_ACCOUNT_TRANSFER);

    }



    @Test
    @DisplayName("이체 중복 요청 - 락 실패 + from 계좌 없음 -> ACCOUNT_NOT_FOUND")
    void transfer_idempotent_lock_failed_account_not_found() {
        // Given
        String fromAccountNo = UUID.randomUUID().toString();
        String toAccountNo = UUID.randomUUID().toString();
        Long amount = 10_000L;
        String txRequestId = UUID.randomUUID().toString();

        given(transactionRedisRepository.tryLock(txRequestId, 1)).willReturn(false);
        given(accountRepository.findByAccountNo(fromAccountNo)).willReturn(Optional.empty());

        // When & Then
        CoreException e = assertThrows(CoreException.class,
                () -> transferUseCase.execute(fromAccountNo, toAccountNo, amount, txRequestId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_FOUND);

    }

    @Test
    @DisplayName("이체 성공")
    void transfer_success() {
        // Given
        String fromAccountNo = UUID.randomUUID().toString();
        String toAccountNo = UUID.randomUUID().toString();
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        Long amount = 100_000L;
        Long feeAmount = 1_000L;
        Double feeRate = 0.01;
        String txRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDate today = LocalDate.now();

        // 계좌 기본 정보 (락 걸리지 않은 조회용)
        Account fromAccountInfo = new Account(
                fromAccountId,
                fromAccountNo,
                500_000L,
                AccountStatus.ACTIVE,
                now,
                now
        );
        Account toAccountInfo = new Account(
                toAccountId,
                toAccountNo,
                0L,
                AccountStatus.ACTIVE,
                now,
                now
        );

        Account firstLockedAccount = new Account(
                fromAccountId,
                fromAccountNo,
                500_000L,
                AccountStatus.ACTIVE,
                now,
                now
        );
        Account secondLockedAccount = new Account(
                toAccountId,
                toAccountNo,
                0L,
                AccountStatus.ACTIVE,
                now,
                now
        );

        AccountDailyLimitUsage usage = mock(AccountDailyLimitUsage.class);
        AccountLimitSetting setting = mock(AccountLimitSetting.class);
        FeeResponse feeResponse = mock(FeeResponse.class);

        given(transactionRedisRepository.tryLock(txRequestId, 1)).willReturn(true);

        given(accountRepository.findByAccountNo(fromAccountNo)).willReturn(Optional.of(fromAccountInfo));
        given(accountRepository.findByAccountNo(toAccountNo)).willReturn(Optional.of(toAccountInfo));

        given(accountRepository.findLockedByAccountId(fromAccountId)).willReturn(Optional.of(firstLockedAccount));
        given(accountRepository.findLockedByAccountId(toAccountId)).willReturn(Optional.of(secondLockedAccount));

        // 일일 이체 한도 조회
        given(accountDailyLimitUsageRepository.findByAccountIdAndLimitDate(fromAccountId, today))
                .willReturn(Optional.of(usage));
        given(usage.getTransferUsed()).willReturn(0L);

        given(accountLimitSettingRepository.findByAccountId(fromAccountId))
                .willReturn(Optional.of(setting));
        given(setting.getDailyTransferLimit()).willReturn(100_000L);

        // 수수료 계산
        given(feeCalculatorFinder.calculate(any(FeeRequest.class))).willReturn(feeResponse);
        given(feeResponse.feeAmount()).willReturn(feeAmount);
        given(feeResponse.rate()).willReturn(feeRate);

        // save(Account)는 전달된 객체 그대로 반환하도록
        given(accountRepository.save(any(Account.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // save(Transaction)도 그대로 반환
        given(accountTransactionRepository.save(any(Transaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        TransferResponse response = transferUseCase.execute(fromAccountNo, toAccountNo, amount, txRequestId);

        // Then
        assertThat(response.targetAccountNo()).isEqualTo(toAccountNo);
        assertThat(response.amount()).isEqualTo(amount);
        assertThat(response.fee()).isEqualTo(feeAmount);
        assertThat(response.feeRate()).isEqualTo(feeRate);
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(accountTransactionRepository, times(2)).save(any(Transaction.class));
    }

}