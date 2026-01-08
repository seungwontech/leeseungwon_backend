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
            Transaction transaction = accountTransactionRepository.findByTransactionRequestId(transactionRequestId)
                    .orElse(Transaction.init(transactionRequestId, amount, TransactionType.TRANSFER));
            return TransferResponse.from(transaction);
        }

        Account fromAccountInfo = accountRepository.findByAccountNo(fromAccountNo)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, fromAccountNo));

        Account toAccountInfo = accountRepository.findByAccountNo(toAccountNo)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, toAccountNo));


        Long firstId = Math.min(fromAccountInfo.getAccountId(), toAccountInfo.getAccountId());
        Long secondId = Math.max(fromAccountInfo.getAccountId(), toAccountInfo.getAccountId());

        Account firstLockedAccount = accountRepository.findLockedByAccountId(firstId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, firstId));

        Account secondLockedAccount = accountRepository.findLockedByAccountId(secondId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, secondId));

        Account fromAccount = fromAccountInfo.getAccountId().equals(firstId) ? firstLockedAccount : secondLockedAccount;
        Account toAccount = toAccountInfo.getAccountId().equals(firstId) ? firstLockedAccount : secondLockedAccount;


        AccountDailyLimitUsage usage = getOrCreateUsage(fromAccount.getAccountId(), LocalDate.now());

        AccountLimitSetting setting = accountLimitSettingRepository.findByAccountId(fromAccount.getAccountId())
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_LIMIT_SETTING_NOT_FOUND, fromAccount.getAccountId()));

        if (usage.getTransferUsed() + amount > setting.getDailyTransferLimit()) {
            throw new CoreException(ErrorType.EXCEED_DAILY_TRANSFER_LIMIT, usage.getTransferUsed() + amount);
        }

        FeeResponse feeResponse = feeCalculatorFinder.calculate(new FeeRequest(amount));

        fromAccount.withdraw(amount + feeResponse.feeAmount());
        toAccount.deposit(amount);

        usage.addTransferUsed(amount);

        Account savedFrom = accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = accountTransactionRepository.save(
                Transaction.createTransfer(savedFrom, toAccount.getAccountNo(), transactionRequestId, amount, feeResponse)
        );

        return TransferResponse.from(transaction);
    }

    private AccountDailyLimitUsage getOrCreateUsage(Long accountId, LocalDate today) {
        return accountDailyLimitUsageRepository.findByAccountIdAndLimitDate(accountId, today)
                .orElseGet(() -> accountDailyLimitUsageRepository.save(AccountDailyLimitUsage.init(accountId, today)));
    }

}
