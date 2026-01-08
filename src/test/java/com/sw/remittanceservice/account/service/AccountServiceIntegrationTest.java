package com.sw.remittanceservice.account.service;

import com.sw.remittanceservice.account.dto.AccountResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.repository.AccountLimitSettingRepository;
import com.sw.remittanceservice.account.repository.AccountRepository;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
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
        // When
        AccountResponse response = accountService.create();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountNo()).isNotNull();
        assertThat(response.balance()).isEqualTo(0L);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE.name());
        assertThat(response.dailyWithdrawLimit()).isEqualTo(1_000_000L);
        assertThat(response.dailyTransferLimit()).isEqualTo(3_000_000L);

        Account account = accountRepository.findByAccountNo(response.accountNo()).orElseThrow();
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getBalance()).isEqualTo(0L);
        assertThat(account.getAccountNo()).isNotBlank();

        AccountLimitSetting setting = accountLimitSettingRepository.findByAccountId(account.getAccountId())
                .orElseThrow();
        assertThat(setting.getDailyWithdrawLimit()).isEqualTo(1_000_000L);
        assertThat(setting.getDailyTransferLimit()).isEqualTo(3_000_000L);
    }

    @Test
    @DisplayName("계좌 삭제 실패 - 이미 해지된 계좌를 삭제 시도하면 예외가 발생하고 데이터는 유지된다")
    void delete_account_fail_already_closed_integration() {
        // Given
        AccountResponse created = accountService.create();
        String accountNo = created.accountNo();

        Account account = accountRepository.findByAccountNo(accountNo).orElseThrow();
        account.close();
        accountRepository.saveAndFlush(account);

        // When & Then
        CoreException e = org.junit.jupiter.api.Assertions.assertThrows(CoreException.class,
                () -> accountService.delete(accountNo));

        // 4. 에러 타입 검증
        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_ACTIVE);

        // 5. DB 상태가 여전히 CLOSED이고, 다른 데이터가 오염되지 않았는지 검증
        Account after = accountRepository.findByAccountNo(accountNo).orElseThrow();
        assertThat(after.getAccountStatus()).isEqualTo(AccountStatus.CLOSED);
    }
}