package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.TransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountDailyLimitUsage;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
import com.sw.remittanceservice.account.repository.*;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class WithdrawUseCase {

    private final AccountRepository accountRepository;

    private final TransactionRepository accountTransactionRepository;

    private final TransactionRedisRepository transactionRedisRepository;

    private final AccountLimitSettingRepository accountLimitSettingRepository;

    private final AccountDailyLimitUsageRepository accountDailyLimitUsageRepository;

    @Transactional
    public TransactionResponse execute(String accountNo, Long amount, String transactionRequestId) {

        if (!transactionRedisRepository.tryLock(transactionRequestId, 1)) {
            Transaction transaction = accountTransactionRepository.findByTransactionRequestId(transactionRequestId)
                    .orElse(Transaction.init(transactionRequestId, amount, TransactionType.WITHDRAW));
            return TransactionResponse.from(transaction);
        }

        Account lockedAccount = accountRepository.findLockedByAccountNo(accountNo)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountNo));

        Long accountId = lockedAccount.getAccountId();

        AccountLimitSetting setting = accountLimitSettingRepository.findByAccountId(accountId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_LIMIT_SETTING_NOT_FOUND, accountId));

        AccountDailyLimitUsage usage = getOrCreateUsage(accountId, LocalDate.now());

        if (usage.getWithdrawUsed() + amount > setting.getDailyWithdrawLimit()) {
            throw new CoreException(ErrorType.EXCEED_DAILY_WITHDRAW_LIMIT, accountId);
        }

        usage.addWithdrawUsed(amount);
        Account savedAccount = accountRepository.save(lockedAccount.withdraw(amount));

        Transaction transaction = accountTransactionRepository.save(
                Transaction.create(savedAccount, transactionRequestId, amount, TransactionType.WITHDRAW)
        );

        return TransactionResponse.from(transaction);
    }

    private AccountDailyLimitUsage getOrCreateUsage(Long accountId, LocalDate today) {
        return accountDailyLimitUsageRepository.findByAccountIdAndLimitDate(accountId, today)
                .orElseGet(() -> accountDailyLimitUsageRepository.save(AccountDailyLimitUsage.init(accountId, today)));
    }
}
