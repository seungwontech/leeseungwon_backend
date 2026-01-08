package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountDailyLimitUsage;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
import com.sw.remittanceservice.account.repository.*;
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
import static org.mockito.BDDMockito.given;

@SpringBootTest
public class WithdrawUseCaseIntegrationTest {

    @Autowired
    private WithdrawUseCase withdrawUseCase;

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

    @Test
    @DisplayName("출금 성공 - 출금 성공 시 잔액, 일일 출금 사용량, 거래 내역이 정상 반영된다")
    void withdraw_success_integration() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDate today = LocalDate.now();

        // 계좌 생성 (잔액 50만)
        Account initAccount = new Account(
                null,
                accountNo,
                500_000L,
                AccountStatus.ACTIVE,
                now,
                now
        );
        Account savedAccount = accountRepository.save(initAccount);

        // 계좌 출금 한도 설정 (defaultOf: 출금 1,000,000, 이체 3,000,000)
        accountLimitSettingRepository.save(
                AccountLimitSetting.defaultOf(savedAccount.getAccountId())
        );

        // Redis 락 성공
        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);

        // When
        TransactionResponse response =
                withdrawUseCase.execute(accountNo, amount, transactionRequestId);

        // Then - 응답 검증
        assertThat(response.transactionStatus()).isEqualTo(TransactionStatus.SUCCESS.name());
        assertThat(response.balanceAfterTransaction())
                .isEqualTo(500_000L - amount);

        // 계좌 잔액 검증 (DB 재조회)
        Account after = accountRepository.findById(savedAccount.getAccountId())
                .orElseThrow();

        assertThat(after.getBalance()).isEqualTo(500_000L - amount);

        // 일일 출금 사용량 검증
        AccountDailyLimitUsage usage = accountDailyLimitUsageRepository
                .findByAccountIdAndLimitDate(savedAccount.getAccountId(), today)
                .orElseThrow();

        assertThat(usage.getWithdrawUsed()).isEqualTo(amount);

        // 트랜잭션 한 건 저장되었는지 검증
        List<Transaction> txList =
                transactionRepository.findByTransactionRequestId(transactionRequestId);

        assertThat(txList).hasSize(1);

        Transaction tx = txList.get(0);
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(tx.getAmount()).isEqualTo(amount);
        assertThat(tx.getTransactionStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getAccountId()).isEqualTo(savedAccount.getAccountId());
    }

    @Test
    @DisplayName("출금 실패 - 일일 출금 한도 초과 시 예외 발생 및 데이터가 변경되지 않는다")
    void withdraw_fail_exceed_daily_withdraw_limit_integration() {
        String accountNo = UUID.randomUUID().toString();
        String txRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDate today = LocalDate.now();

        Account initAccount = new Account(
                null,
                accountNo,
                500_000L,
                AccountStatus.ACTIVE,
                now,
                now
        );
        Account savedAccount = accountRepository.save(initAccount);

        // 한도 설정 (출금 1,000,000)
        AccountLimitSetting setting = accountLimitSettingRepository.save(
                AccountLimitSetting.defaultOf(savedAccount.getAccountId())
        );
        Long limit = setting.getDailyWithdrawLimit();

        Long alreadyUsed = limit - 10_000L;
        Long amount = 20_000L;

        AccountDailyLimitUsage usage = accountDailyLimitUsageRepository.save(
                AccountDailyLimitUsage.init(savedAccount.getAccountId(), today)
        );
        usage.addWithdrawUsed(alreadyUsed);
        accountDailyLimitUsageRepository.save(usage);

        given(transactionRedisRepository.tryLock(txRequestId, 1)).willReturn(true);

        Long beforeBalance = savedAccount.getBalance();

        // When
        CoreException e = assertThrows(CoreException.class,
                () -> withdrawUseCase.execute(accountNo, amount, txRequestId));

        // Then
        assertThat(e.getErrorType()).isEqualTo(ErrorType.EXCEED_DAILY_WITHDRAW_LIMIT);

        Account after = accountRepository.findById(savedAccount.getAccountId())
                .orElseThrow();
        assertThat(after.getBalance()).isEqualTo(beforeBalance);

        AccountDailyLimitUsage afterUsage = accountDailyLimitUsageRepository
                .findByAccountIdAndLimitDate(savedAccount.getAccountId(), today)
                .orElseThrow();
        assertThat(afterUsage.getWithdrawUsed()).isEqualTo(alreadyUsed);

        List<Transaction> txList =
                transactionRepository.findByTransactionRequestId(txRequestId);
        assertThat(txList).isEmpty();
    }

    @Test
    @DisplayName("출금 실패 - 존재하지 않는 계좌 출금 시 예외 발생 및 거래 내역이 생성되지 않는다")
    void withdraw_fail_account_not_found_integration() {
        // Given
        String notExistsAccountNo = "NOT-EXISTS-" + UUID.randomUUID();
        Long amount = 10_000L;
        String txRequestId = UUID.randomUUID().toString();

        // Redis 락은 성공했다고 가정
        given(transactionRedisRepository.tryLock(txRequestId, 1)).willReturn(true);

        // When
        CoreException e = assertThrows(CoreException.class,
                () -> withdrawUseCase.execute(notExistsAccountNo, amount, txRequestId));

        // Then
        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_FOUND);

        List<Transaction> txList =
                transactionRepository.findByTransactionRequestId(txRequestId);
        assertThat(txList).isEmpty();
    }

    @Test
    @DisplayName("출금 실패 - 해지된 계좌에서 출금 시도 시 예외 발생 및 데이터 롤백")
    void withdraw_fail_account_closed_integration() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long amount = 10_000L;
        String txRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Account closedAccount = accountRepository.save(new Account(
                null,
                accountNo,
                100_000L,
                AccountStatus.CLOSED, // 해지 상태
                now,
                now
        ));

        accountLimitSettingRepository.save(
                AccountLimitSetting.defaultOf(closedAccount.getAccountId())
        );

        given(transactionRedisRepository.tryLock(txRequestId, 1)).willReturn(true);

        // When
        CoreException e = assertThrows(CoreException.class,
                () -> withdrawUseCase.execute(accountNo, amount, txRequestId));

        // Then
        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_ACTIVE);

        Account after = accountRepository.findById(closedAccount.getAccountId()).orElseThrow();
        assertThat(after.getBalance()).isEqualTo(100_000L); // 잔액 그대로
        assertThat(after.getAccountStatus()).isEqualTo(AccountStatus.CLOSED);

        // 거래 내역이 저장되지 않았는지 검증
        List<Transaction> txList = transactionRepository.findByTransactionRequestId(txRequestId);
        assertThat(txList).isEmpty();

        // 일일 사용량 레코드가 생성되거나 변경되지 않았는지 검증
        boolean usageExists = accountDailyLimitUsageRepository
                .findByAccountIdAndLimitDate(closedAccount.getAccountId(), LocalDate.now())
                .isPresent();
        assertThat(usageExists).isFalse();
    }
}
