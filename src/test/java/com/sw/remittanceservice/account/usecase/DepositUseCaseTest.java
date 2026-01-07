package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
import com.sw.remittanceservice.account.repository.AccountRepository;
import com.sw.remittanceservice.account.repository.TransactionRepository;
import com.sw.remittanceservice.account.repository.TransactionRedisRepository;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository accountTransactionRepository;

    @Mock
    private TransactionRedisRepository transactionRedisRepository;

    @InjectMocks
    private DepositUseCase depositUseCase;


    @Test
    @DisplayName("입금 성공")
    void deposit_success() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long accountId = 1L;
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();
        TransactionType type = TransactionType.DEPOSIT;
        LocalDateTime now = LocalDateTime.of(2026,1,1,0,0);

        Account lockedAccount = new Account(
                accountId,
                accountNo,
                0L,
                AccountStatus.ACTIVE,
                now,
                now
        );

        Account savedAccount = new Account(
                accountId,
                lockedAccount.getAccountNo(),
                amount,
                AccountStatus.ACTIVE,
                lockedAccount.getCreatedAt(),
                now
        );

        Transaction savedTransaction = Transaction.create(savedAccount, transactionRequestId, amount, type);

        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);
        given(accountRepository.findLockedByAccountNo(accountNo)).willReturn(Optional.of(lockedAccount));
        given(accountRepository.save(any(Account.class))).willReturn(savedAccount);
        given(accountTransactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);

        // When
        TransactionResponse response = depositUseCase.execute(accountNo, amount, transactionRequestId);

        // Then
        assertThat(response.transactionStatus()).isEqualTo("SUCCESS");
        assertThat(response.balanceAfterTransaction()).isEqualTo(amount);

    }

    @Test
    @DisplayName("입금 중복 요청 - 레디스에 이미 transactionId 존재")
    void deposit_idempotent_lock_failed_and_tx_missing_returns_pending() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();

        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(false);
        given(accountTransactionRepository.findByTransactionRequestId(transactionRequestId))
                .willReturn(Optional.empty());

        // When
        TransactionResponse response = depositUseCase.execute(accountNo, amount, transactionRequestId);

        // Then
        assertThat(response.transactionStatus()).isEqualTo(TransactionStatus.PENDING.name());

        verify(accountTransactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("입금 실패 - 계좌 없음")
    void deposit_fail_account_not_found() {
        // Given
        String accountNo = UUID.randomUUID().toString();
        Long amount = 10_000L;
        String transactionRequestId = UUID.randomUUID().toString();

        given(transactionRedisRepository.tryLock(transactionRequestId, 1)).willReturn(true);
        given(accountRepository.findLockedByAccountNo(accountNo)).willReturn(Optional.empty());

        // When/Then
        CoreException e = assertThrows(CoreException.class,
                () -> depositUseCase.execute(accountNo, amount, transactionRequestId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_FOUND);
    }

}