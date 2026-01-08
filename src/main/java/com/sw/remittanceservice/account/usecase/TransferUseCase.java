package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransferResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountDailyLimitUsage;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
import com.sw.remittanceservice.account.repository.*;
import com.sw.remittanceservice.account.usecase.policy.FeeCalculatorFinder;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@RequiredArgsConstructor
@Component
public class TransferUseCase {

    private final AccountRepository accountRepository;

    private final TransactionRepository accountTransactionRepository;

    private final TransactionRedisRepository transactionRedisRepository;

    private final AccountLimitSettingRepository accountLimitSettingRepository;

    private final AccountDailyLimitUsageRepository accountDailyLimitUsageRepository;

    private final FeeCalculatorFinder feeCalculatorFinder;

    @Transactional
    public TransferResponse execute(String fromAccountNo, String toAccountNo, Long amount, String transactionRequestId) {

        if (fromAccountNo.equals(toAccountNo)) {
            throw new CoreException(ErrorType.SAME_ACCOUNT_TRANSFER, toAccountNo);
        }

        if (!transactionRedisRepository.tryLock(transactionRequestId, 1)) {
            Account account = accountRepository.findByAccountNo(fromAccountNo).orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, fromAccountNo));

            Transaction transaction = accountTransactionRepository
                    .findByAccountIdAndTransactionRequestId(account.getAccountId(), transactionRequestId)
                    .orElse(Transaction.init(transactionRequestId, amount, TransactionType.WITHDRAW));
            return TransferResponse.from(transaction);
        }

        Long fromAccountId = accountRepository.findIdByAccountNo(fromAccountNo)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, fromAccountNo));

        Long toAccountId = accountRepository.findIdByAccountNo(toAccountNo)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, toAccountNo));

        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);

        Account firstLockedAccount = accountRepository.findLockedByAccountId(firstId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, firstId));

        Account secondLockedAccount = accountRepository.findLockedByAccountId(secondId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, secondId));

        Account fromAccount = fromAccountId.equals(firstId) ? firstLockedAccount : secondLockedAccount;
        Account toAccount = toAccountId.equals(firstId) ? firstLockedAccount : secondLockedAccount;

        fromAccount.validateActive();
        toAccount.validateActive();

        AccountDailyLimitUsage usage = getOrCreateUsage(fromAccount.getAccountId(), LocalDate.now());
        AccountLimitSetting setting = accountLimitSettingRepository.findByAccountId(fromAccount.getAccountId())
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_LIMIT_SETTING_NOT_FOUND, fromAccount.getAccountId()));

        if (usage.getTransferUsed() + amount > setting.getDailyTransferLimit()) {
            throw new CoreException(ErrorType.EXCEED_DAILY_TRANSFER_LIMIT, usage.getTransferUsed() + amount);
        }

        FeeResponse feeResponse = feeCalculatorFinder.calculate(new FeeRequest(amount));


        usage.addTransferUsed(amount);

        Account updatedLockFromAccount = fromAccount.withdraw(amount + feeResponse.feeAmount());
        Account updatedLockToAccount = toAccount.deposit(amount);

        Account savedFromAccount = accountRepository.save(updatedLockFromAccount);
        Account savedToAccount = accountRepository.save(updatedLockToAccount);

        Transaction transactionFromAccount = accountTransactionRepository.save(
                Transaction.createTransferWithdraw(savedFromAccount, savedToAccount.getAccountNo(), transactionRequestId, amount, feeResponse)
        );

        accountTransactionRepository.save(
                Transaction.createTransferDeposit(savedToAccount, savedFromAccount.getAccountNo(), transactionRequestId, amount)
        );

        return TransferResponse.from(transactionFromAccount);
    }

    private AccountDailyLimitUsage getOrCreateUsage(Long accountId, LocalDate today) {
        return accountDailyLimitUsageRepository.findLockedByAccountIdAndLimitDate(accountId, today)
                .orElseGet(() -> accountDailyLimitUsageRepository.save(AccountDailyLimitUsage.init(accountId, today)));
    }

}
