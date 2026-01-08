package com.sw.remittanceservice.account.service;

import com.sw.remittanceservice.account.dto.AccountResponse;
import com.sw.remittanceservice.account.entity.*;
import com.sw.remittanceservice.account.repository.*;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    private final AccountLimitSettingRepository accountLimitSettingRepository;

    public AccountResponse read(String accountNo) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountNo));

        AccountLimitSetting setting = accountLimitSettingRepository.findByAccountId(account.getAccountId())
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_LIMIT_SETTING_NOT_FOUND, accountNo));

        return AccountResponse.from(account, setting.getDailyWithdrawLimit(), setting.getDailyTransferLimit());
    }

    @Transactional
    public AccountResponse create() {
        String accountNo = UUID.randomUUID().toString();

        Account account = accountRepository.save(
                Account.create(accountNo)
        );

        AccountLimitSetting setting = accountLimitSettingRepository.save(AccountLimitSetting.defaultOf(account.getAccountId()));

        return AccountResponse.from(account, setting.getDailyWithdrawLimit(), setting.getDailyTransferLimit());

    }

    @Transactional
    public void delete(String accountNo) {

        Account account = accountRepository.findLockedByAccountNo(accountNo)
                .orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountNo));

        account.validateActive();
        account.close();
    }
}
