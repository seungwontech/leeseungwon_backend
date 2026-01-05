package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.AccountTransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountTransaction;
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

    private final AccountTransactionRepository accountTransactionRepository;

    private final TransactionRedisRepository transactionRedisRepository;

    @Transactional
    public AccountTransactionResponse execute(Long accountId, Long amount, String transactionId) {
        AccountTransaction transaction = AccountTransaction.depositPending(accountId, transactionId, amount);


        if (transactionRedisRepository.isLocked(transactionId, 1)) {
            AccountTransaction existing = accountTransactionRepository.findByTransactionId(transactionId)
                    .orElse(transaction);

            return AccountTransactionResponse.from(existing);
        }

        accountTransactionRepository.save(transaction);

        Account account = accountRepository.findLockedByAccountId(accountId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountId));


        Account savedAccount = accountRepository.save(
                account.deposit(amount)
        );

        transaction.success(savedAccount.getBalance(), 0L);

        return AccountTransactionResponse.from(transaction);
    }
}
