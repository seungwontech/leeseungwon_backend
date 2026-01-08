package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
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
    public TransactionResponse execute(String accountNo, Long amount, String transactionRequestId) {

        if (!transactionRedisRepository.tryLock(transactionRequestId, 1)) {
            Account account = accountRepository.findByAccountNo(accountNo)
                    .orElseThrow(()-> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountNo));

            Transaction transaction = accountTransactionRepository
                    .findByAccountIdAndTransactionRequestId(account.getAccountId(), transactionRequestId)
                    .orElse(Transaction.init(transactionRequestId, amount, TransactionType.DEPOSIT));
            return TransactionResponse.from(transaction);
        }

        Account lockedAccount = accountRepository.findLockedByAccountNo(accountNo)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountNo));

        lockedAccount.validateActive();

        Account updatedLockAccount = lockedAccount.deposit(amount);
        Account savedAccount = accountRepository.save(updatedLockAccount);

        Transaction transaction = accountTransactionRepository.save(
                Transaction.create(savedAccount, transactionRequestId, amount, TransactionType.DEPOSIT)
        );

        return TransactionResponse.from(transaction);
    }
}
