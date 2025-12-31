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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountLimitSettingRepository accountLimitSettingRepository;

    @InjectMocks
    private AccountService accountService;


    @Test
    @DisplayName("계좌 개설")
    void create_account() {

        // Given
        LocalDateTime now = LocalDateTime.of(2025, 12, 30, 0, 0);
        Account savedAccount = new Account(
                1L,
                UUID.randomUUID().toString(),
                0L,
                AccountStatus.ACTIVE,
                now,
                now
        );

        AccountLimitSetting savedSetting = new AccountLimitSetting(
                1L,
                1L,
                1_000_000L,
                3_000_000L,
                now,
                now
        );
        given(accountRepository.save(any(Account.class)))
                .willReturn(savedAccount);

        given(accountLimitSettingRepository.save(any(AccountLimitSetting.class)))
                .willReturn(savedSetting);

        AccountCreateRequest request = new AccountCreateRequest();

        // When
        AccountResponse response = accountService.create(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.balance()).isEqualTo(0L);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE.name());
        assertThat(response.dailyWithdrawLimit()).isEqualTo(1_000_000L);
        assertThat(response.dailyTransferLimit()).isEqualTo(3_000_000L);

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(accountLimitSettingRepository, times(1)).save(any(AccountLimitSetting.class));
        verifyNoMoreInteractions(accountRepository, accountLimitSettingRepository);
    }
}