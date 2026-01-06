package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.repository.*;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class DepositUseCase {

    private final AccountRepository accountRepository;

    private final TransactionRepository accountTransactionRepository;

    private final TransactionRedisRepository transactionRedisRepository;

    @Transactional
    public TransactionResponse execute(Long accountId, Long amount, String transactionRequestId) {
        Transaction transaction = Transaction.depositPending(accountId, transactionRequestId, amount);

        if (!transactionRedisRepository.tryLock(transactionRequestId, 1)) {
            return TransactionResponse.from(
                    accountTransactionRepository.findByTransactionRequestId(transactionRequestId).orElse(transaction)
            );
        }

        accountTransactionRepository.save(transaction);

        Account account = accountRepository.findLockedByAccountId(accountId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountId));


        Account savedAccount = accountRepository.save(
                account.deposit(amount)
        );

        transaction.success(savedAccount.getBalance());

        return TransactionResponse.from(transaction);
    }
}
