package com.sw.remittanceservice.account.service;

import com.sw.remittanceservice.account.dto.AccountCreateRequest;
import com.sw.remittanceservice.account.dto.AccountResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.repository.AccountLimitSettingRepository;
import com.sw.remittanceservice.account.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Transactional
@SpringBootTest
class AccountServiceIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountLimitSettingRepository accountLimitSettingRepository;

    @Autowired
    private AccountService accountService;


    @Test
    @DisplayName("계좌 개설")
    void create_account() {

        AccountCreateRequest request = new AccountCreateRequest();
        
        // When
        AccountResponse response = accountService.create(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountId()).isNotNull();
        assertThat(response.balance()).isEqualTo(0L);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE.name());
        assertThat(response.dailyWithdrawLimit()).isEqualTo(1_000_000L);
        assertThat(response.dailyTransferLimit()).isEqualTo(3_000_000L);

        Account account = accountRepository.findById(response.accountId()).orElseThrow();
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getBalance()).isEqualTo(0L);
        assertThat(account.getAccountNo()).isNotBlank();

        AccountLimitSetting setting = accountLimitSettingRepository.findByAccountId(account.getAccountId())
                .orElseThrow();
        assertThat(setting.getDailyWithdrawLimit()).isEqualTo(1_000_000L);
        assertThat(setting.getDailyTransferLimit()).isEqualTo(3_000_000L);
    }
}