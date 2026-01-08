package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransferResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountDailyLimitUsage;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
import com.sw.remittanceservice.account.repository.*;
import com.sw.remittanceservice.account.usecase.policy.FeeCalculatorFinder;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;


@SpringBootTest
public class TransferUseCaseIntegrationTest {

    @Autowired
    private TransferUseCase transferUseCase;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountLimitSettingRepository accountLimitSettingRepository;

    @Autowired
    private AccountDailyLimitUsageRepository accountDailyLimitUsageRepository;

    @MockitoBean
    private TransactionRedisRepository transactionRedisRepository;

    @MockitoBean
    private FeeCalculatorFinder feeCalculatorFinder;

    @Test
    @DisplayName("이체 성공 - 계좌 생성 후 이체 시 잔액, 일일 한도, 트랜잭션이 정상 반영된다")
    void transfer_success_integration()  {
        // Given
        String fromAccountNo = UUID.randomUUID().toString();
        String toAccountNo = UUID.randomUUID().toString();
        Long amount = 100_000L;
        Long feeAmount = 1_000L;
        Double feeRate = 0.01;
        String txRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDate today = LocalDate.now();
        LocalDateTime requestedAt = LocalDateTime.now();

        // 1. 출금/입금 계좌 생성
        Account fromAccount = new Account(
                null,
                fromAccountNo,
                500_000L,
                AccountStatus.ACTIVE,
                now,
                now
        );
        Account toAccount = new Account(
                null,
                toAccountNo,
                0L,
                AccountStatus.ACTIVE,
                now,
                now
        );

        Account savedFrom = accountRepository.save(fromAccount);
        Account savedTo = accountRepository.save(toAccount);

        accountLimitSettingRepository.save(AccountLimitSetting.defaultOf(savedFrom.getAccountId()));

        given(transactionRedisRepository.tryLock(txRequestId, 1)).willReturn(true);

        // 4. 수수료 계산 Mock
        FeeResponse feeResponse = new FeeResponse(FeePolicyType.DEFAULT, feeRate, feeAmount, requestedAt);
        given(feeCalculatorFinder.calculate(any(FeeRequest.class))).willReturn(feeResponse);

        // When
        TransferResponse response =
                transferUseCase.execute(fromAccountNo, toAccountNo, amount, txRequestId);

        // Then
        assertThat(response.targetAccountNo()).isEqualTo(toAccountNo);
        assertThat(response.amount()).isEqualTo(amount);
        assertThat(response.fee()).isEqualTo(feeAmount);
        assertThat(response.feeRate()).isEqualTo(feeRate);

        // 계좌 잔액 검증
        Account fromAfter = accountRepository.findById(savedFrom.getAccountId())
                .orElseThrow();
        Account toAfter = accountRepository.findById(savedTo.getAccountId())
                .orElseThrow();

        assertThat(fromAfter.getBalance()).isEqualTo(500_000L - amount - feeAmount);
        assertThat(toAfter.getBalance()).isEqualTo(amount);

        // 일일 이체 한도 사용량 검증
        AccountDailyLimitUsage usage = accountDailyLimitUsageRepository
                .findByAccountIdAndLimitDate(savedFrom.getAccountId(), today)
                .orElseThrow();

        assertThat(usage.getTransferUsed()).isEqualTo(amount);

        // createTransferWithdraw, createTransferDeposit 두 건이 저장되어야 함
        List<Transaction> txList =
                transactionRepository.findByTransactionRequestId(txRequestId);

        assertThat(txList).hasSize(2);

        boolean hasWithdraw = txList.stream()
                .anyMatch(tx -> tx.getTransactionType() == TransactionType.WITHDRAW);
        boolean hasDeposit = txList.stream()
                .anyMatch(tx -> tx.getTransactionType() == TransactionType.DEPOSIT);

        assertThat(hasWithdraw).isTrue();
        assertThat(hasDeposit).isTrue();
    }

    @Test
    @DisplayName("이체 실패 - 일일 이체 한도 초과 시 예외 발생 및 데이터가 변경되지 않는다")
    void transfer_fail_exceed_daily_limit_integration() {
        // Given
        String fromAccountNo = UUID.randomUUID().toString();
        String toAccountNo = UUID.randomUUID().toString();
        Long amount = 150_000L;
        long feeAmount = 1_000L;
        double feeRate = 0.01;
        String txRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDate today = LocalDate.now();
        LocalDateTime requestedAt = LocalDateTime.now();

        Account fromAccount = new Account(
                null,
                fromAccountNo,
                500_000L,
                AccountStatus.ACTIVE,
                now,
                now
        );
        Account toAccount = new Account(
                null,
                toAccountNo,
                0L,
                AccountStatus.ACTIVE,
                now,
                now
        );

        Account savedFrom = accountRepository.save(fromAccount);
        Account savedTo = accountRepository.save(toAccount);

        accountLimitSettingRepository.save(AccountLimitSetting.defaultOf(savedFrom.getAccountId()));

        AccountDailyLimitUsage usage = accountDailyLimitUsageRepository.save(
                AccountDailyLimitUsage.init(savedFrom.getAccountId(), today)
        );
        usage.addTransferUsed(2_900_000L); // 이미 290만 사용
        accountDailyLimitUsageRepository.save(usage);

        given(transactionRedisRepository.tryLock(txRequestId, 1)).willReturn(true);

        FeeResponse feeResponse = new FeeResponse(FeePolicyType.DEFAULT, feeRate, feeAmount, requestedAt);
        given(feeCalculatorFinder.calculate(any(FeeRequest.class))).willReturn(feeResponse);

        // When
        CoreException e = assertThrows(CoreException.class,
                () -> transferUseCase.execute(fromAccountNo, toAccountNo, amount, txRequestId));

        // Then
        assertThat(e.getErrorType()).isEqualTo(ErrorType.EXCEED_DAILY_TRANSFER_LIMIT);

        // 한도 초과이므로 실제 잔액은 변하지 않아야 함
        Account fromAfter = accountRepository.findById(savedFrom.getAccountId())
                .orElseThrow();
        Account toAfter = accountRepository.findById(savedTo.getAccountId())
                .orElseThrow();

        assertThat(fromAfter.getBalance()).isEqualTo(500_000L);
        assertThat(toAfter.getBalance()).isEqualTo(0L);

        // 해당 txRequestId 트랜잭션이 없어야 함
        List<Transaction> txList =
                transactionRepository.findByTransactionRequestId(txRequestId);
        assertThat(txList).isEmpty();
    }

    @Test
    @DisplayName("이체 실패 - 보내는 계좌가 해지된 상태인 경우 예외 발생 및 트랜잭션 롤백")
    void transfer_fail_from_account_closed_integration() {
        // Given
        String fromAccountNo = UUID.randomUUID().toString();
        String toAccountNo = UUID.randomUUID().toString();
        Long amount = 50_000L;
        String txRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        // 1. 보내는 계좌(CLOSED), 받는 계좌(ACTIVE) 생성 및 저장
        Account fromAccount = accountRepository.save(new Account(
                null, fromAccountNo, 100_000L, AccountStatus.CLOSED, now, now
        ));
        Account toAccount = accountRepository.save(new Account(
                null, toAccountNo, 10_000L, AccountStatus.ACTIVE, now, now
        ));

        given(transactionRedisRepository.tryLock(txRequestId, 1)).willReturn(true);

        // When & Then
        CoreException e = assertThrows(CoreException.class,
                () -> transferUseCase.execute(fromAccountNo, toAccountNo, amount, txRequestId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_ACTIVE);

        // 데이터 정합성 검증 (롤백 확인)
        Account fromAfter = accountRepository.findById(fromAccount.getAccountId()).orElseThrow();
        Account toAfter = accountRepository.findById(toAccount.getAccountId()).orElseThrow();

        assertThat(fromAfter.getBalance()).isEqualTo(100_000L); // 잔액 불변
        assertThat(toAfter.getBalance()).isEqualTo(10_000L);   // 잔액 불변

        // 거래 내역이 저장되지 않았는지 확인
        assertThat(transactionRepository.findByTransactionRequestId(txRequestId)).isEmpty();
    }

    @Test
    @DisplayName("이체 실패 - 받는 계좌가 해지된 상태인 경우 예외 발생 및 트랜잭션 롤백")
    void transfer_fail_to_account_closed_integration() {
        // Given
        String fromAccountNo = UUID.randomUUID().toString();
        String toAccountNo = UUID.randomUUID().toString();
        Long amount = 50_000L;
        String txRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        // 1. 보내는 계좌(ACTIVE), 받는 계좌(CLOSED) 생성 및 저장
        Account fromAccount = accountRepository.save(new Account(
                null, fromAccountNo, 100_000L, AccountStatus.ACTIVE, now, now
        ));
        Account toAccount = accountRepository.save(new Account(
                null, toAccountNo, 10_000L, AccountStatus.CLOSED, now, now
        ));

        // 한도 설정 (한도 체크 로직 통과를 위해 필요)
        accountLimitSettingRepository.save(AccountLimitSetting.defaultOf(fromAccount.getAccountId()));

        given(transactionRedisRepository.tryLock(txRequestId, 1)).willReturn(true);

        // When & Then
        CoreException e = assertThrows(CoreException.class,
                () -> transferUseCase.execute(fromAccountNo, toAccountNo, amount, txRequestId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_ACTIVE);

        // 잔액 변화 없음 확인
        assertThat(accountRepository.findById(fromAccount.getAccountId()).get().getBalance()).isEqualTo(100_000L);
        assertThat(accountRepository.findById(toAccount.getAccountId()).get().getBalance()).isEqualTo(10_000L);

        // 거래 내역 미생성 확인
        assertThat(transactionRepository.findByTransactionRequestId(txRequestId)).isEmpty();
    }
}
