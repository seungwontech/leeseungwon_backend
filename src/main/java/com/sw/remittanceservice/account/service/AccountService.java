package com.sw.remittanceservice.account.service;

import com.sw.remittanceservice.account.dto.AccountCreateRequest;
import com.sw.remittanceservice.account.dto.AccountResponse;
import com.sw.remittanceservice.account.dto.AccountTransactionResponse;
import com.sw.remittanceservice.account.dto.TransferResponse;
import com.sw.remittanceservice.account.entity.*;
import com.sw.remittanceservice.account.repository.*;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    private final AccountTransactionRepository accountTransactionRepository;

    private final TransactionRedisRepository transactionRedisRepository;

    private final AccountLimitSettingRepository accountLimitSettingRepository;

    private final AccountDailyLimitUsageRepository accountDailyLimitUsageRepository;

    public AccountResponse read(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountId));

        AccountLimitSetting setting = accountLimitSettingRepository.findByAccountId(account.getAccountId())
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_LIMIT_SETTING_NOT_FOUND, accountId));

        return AccountResponse.from(account, setting.getDailyWithdrawLimit(), setting.getDailyTransferLimit());
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest request) {
        String accountNo = UUID.randomUUID().toString();

        Account account = accountRepository.save(
                Account.create(accountNo)
        );

        AccountLimitSetting setting = accountLimitSettingRepository.save(AccountLimitSetting.defaultOf(account.getAccountId()));

        return AccountResponse.from(account, setting.getDailyWithdrawLimit(), setting.getDailyTransferLimit());

    }

    @Transactional
    public void delete(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountId));

        account.close();
    }

    @Transactional
    public AccountTransactionResponse deposit(Long accountId, Long amount, String transactionId) {
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

    @Transactional
    public AccountTransactionResponse withdraw(Long accountId, Long amount, String transactionId) {

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

    public TransferResponse transfer(Long fromAccountId, Long toAccountId, Long amount, String transactionId) {
        if (fromAccountId.equals(toAccountId)) throw new CoreException(ErrorType.SAME_ACCOUNT_TRANSFER, toAccountId);

        AccountTransaction transaction = AccountTransaction.transferPending(fromAccountId, toAccountId, transactionId, amount);

        long fee = (long) (amount * 0.01);
        long totalWithdrawAmount = amount + fee;

        if (transactionRedisRepository.isLocked(transactionId, 1)) {
            AccountTransaction existing = accountTransactionRepository.findByTransactionId(transactionId)
                    .orElse(transaction);
            return TransferResponse.of(fromAccountId, toAccountId, amount, fee, existing.getTransactionStatus());
        }

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

        fromAccount.withdraw(totalWithdrawAmount);
        toAccount.deposit(amount);

        Account savedFromAccount = accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        transaction.success(savedFromAccount.getBalance(),1L);
        return TransferResponse.from(transaction);
    }
}
