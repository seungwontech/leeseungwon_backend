package com.sw.remittanceservice.account.usecase;


import com.sw.remittanceservice.account.dto.TransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
import com.sw.remittanceservice.account.repository.AccountLimitSettingRepository;
import com.sw.remittanceservice.account.repository.AccountRepository;
import com.sw.remittanceservice.account.repository.TransactionRedisRepository;
import com.sw.remittanceservice.account.repository.TransactionRepository;
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
public class DepositUseCaseIntegrationTest {

    @Autowired
    private DepositUseCase depositUseCase;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountLimitSettingRepository accountLimitSettingRepository;

    @MockitoBean
    private TransactionRedisRepository transactionRedisRepository;

    @Test
    @DisplayName("입금 성공 - 입금 성공 시 계좌 잔액과 거래 내역이 정상 반영된다")
    void deposit_success_integration() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

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

        accountLimitSettingRepository.save(
                AccountLimitSetting.defaultOf(savedAccount.getAccountId())
        );

        // Redis 락은 항상 성공했다고 가정
        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);

        // When
        TransactionResponse response = depositUseCase.execute(accountNo, amount, transactionRequestId);

        // Then
        assertThat(response.transactionStatus()).isEqualTo(TransactionStatus.SUCCESS.name());
        assertThat(response.balanceAfterTransaction()).isEqualTo(savedAccount.getBalance() + amount);

        // 계좌 잔액 검증 (DB 재조회)
        Account after = accountRepository.findByAccountNo(accountNo)
                .orElseThrow();

        assertThat(after.getBalance()).isEqualTo(savedAccount.getBalance() + amount);

        // 트랜잭션 한 건 저장되었는지 검증
        List<Transaction> txList = transactionRepository.findByTransactionRequestId(transactionRequestId);
        assertThat(txList).hasSize(1);

        Transaction tx = txList.get(0);
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.getAmount()).isEqualTo(amount);
        assertThat(tx.getTransactionStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getAccountId()).isEqualTo(after.getAccountId());
    }


    @Test
    @DisplayName("입금 실패 - 존재하지 않는 계좌에 입금 시 예외 발생 및 데이터가 변경되지 않는다")
    void deposit_fail_account_not_found_integration() {
        // Given
        String notExistsAccountNo = "not" + UUID.randomUUID();
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();

        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);

        // When
        CoreException e = assertThrows(CoreException.class,
                () -> depositUseCase.execute(notExistsAccountNo, amount, transactionRequestId));

        // Then
        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_FOUND);

        List<Transaction> txList = transactionRepository.findByTransactionRequestId(transactionRequestId);
        assertThat(txList).isEmpty();
    }
}
