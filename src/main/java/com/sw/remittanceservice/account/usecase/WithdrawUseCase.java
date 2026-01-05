package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.dto.AccountTransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountDailyLimitUsage;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.AccountTransaction;
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

    private final AccountTransactionRepository accountTransactionRepository;

    private final TransactionRedisRepository transactionRedisRepository;

    private final AccountLimitSettingRepository accountLimitSettingRepository;

    private final AccountDailyLimitUsageRepository accountDailyLimitUsageRepository;

    @Transactional
    public AccountTransactionResponse execute(Long accountId, Long amount, String transactionId) {

        AccountTransaction transaction = AccountTransaction.withdrawPending(accountId, transactionId, amount);

        if (transactionRedisRepository.isLocked(transactionId, 1)) {
            AccountTransaction existing = accountTransactionRepository.findByTransactionId(transactionId)
                    .orElse(transaction);
            return AccountTransactionResponse.from(existing);
        }

        accountTransactionRepository.save(transaction);

        AccountLimitSetting setting = accountLimitSettingRepository.findByAccountId(accountId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_LIMIT_SETTING_NOT_FOUND, accountId));

        Account account = accountRepository.findLockedByAccountId(accountId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountId));

        AccountDailyLimitUsage usage = getOrCreateUsage(accountId, LocalDate.now());

        if (usage.getWithdrawUsed() + amount > setting.getDailyWithdrawLimit()) {
            throw new CoreException(ErrorType.EXCEED_DAILY_WITHDRAW_LIMIT, accountId);
        }

        usage.addWithdrawUsed(amount);

        Account savedAccount = accountRepository.save(
                account.withdraw(amount)
        );

        transaction.success(savedAccount.getBalance(), 0L);

        return AccountTransactionResponse.from(transaction);
    }

    private AccountDailyLimitUsage getOrCreateUsage(Long accountId, LocalDate today) {
        return accountDailyLimitUsageRepository.findByAccountIdAndLimitDate(accountId, today)
                .orElseGet(() -> accountDailyLimitUsageRepository.save(AccountDailyLimitUsage.init(accountId, today)));
    }

}
