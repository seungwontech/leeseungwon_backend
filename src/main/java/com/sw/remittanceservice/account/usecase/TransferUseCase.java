package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransferResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountDailyLimitUsage;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.Transaction;
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
import java.time.LocalDateTime;


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
    public TransferResponse execute(Long fromAccountId, Long toAccountId, Long amount, String transactionRequestId) {

        if (fromAccountId.equals(toAccountId)) {
            throw new CoreException(ErrorType.SAME_ACCOUNT_TRANSFER, toAccountId);
        }

        Transaction transaction = Transaction.transferPending(fromAccountId, toAccountId, transactionRequestId, amount);

        if (!transactionRedisRepository.tryLock(transactionRequestId, 1)) {
            return TransferResponse.from(
                    accountTransactionRepository.findByTransactionRequestId(transactionRequestId).orElse(transaction)
            );
        }

        accountTransactionRepository.save(transaction);

        AccountLimitSetting setting = accountLimitSettingRepository.findByAccountId(fromAccountId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_LIMIT_SETTING_NOT_FOUND, fromAccountId));

        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);

        Account firstAccount = accountRepository.findLockedByAccountId(firstId).orElseThrow();
        Account secondAccount = accountRepository.findLockedByAccountId(secondId).orElseThrow();

        Account fromAccount = fromAccountId.equals(firstId) ? firstAccount : secondAccount;
        Account toAccount = toAccountId.equals(firstId) ? firstAccount : secondAccount;

        AccountDailyLimitUsage usage = getOrCreateUsage(fromAccountId, LocalDate.now());

        if (usage.getTransferUsed() + amount > setting.getDailyTransferLimit()) {
            throw new CoreException(ErrorType.EXCEED_DAILY_TRANSFER_LIMIT, usage.getTransferUsed() + amount);
        }

        FeeResponse feeResponse = feeCalculatorFinder.calculate(
                new FeeRequest(amount, LocalDateTime.now())
        );

        long fee = feeResponse.feeAmount();
        long totalWithdrawAmount = amount + fee;

        fromAccount.withdraw(totalWithdrawAmount);
        toAccount.deposit(amount);

        usage.addTransferUsed(amount);

        Account savedFromAccount = accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        transaction.success(savedFromAccount.getBalance(), fee, feeResponse);
        return TransferResponse.from(transaction);
    }

    private AccountDailyLimitUsage getOrCreateUsage(Long accountId, LocalDate today) {
        return accountDailyLimitUsageRepository.findByAccountIdAndLimitDate(accountId, today)
                .orElseGet(() -> accountDailyLimitUsageRepository.save(AccountDailyLimitUsage.init(accountId, today)));
    }
}
