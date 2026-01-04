package com.sw.remittanceservice.account.service;

import com.sw.remittanceservice.account.dto.AccountCreateRequest;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    }


    @Test
    @DisplayName("계좌 조회 성공")
    void read_account_success() {

        // Give
        Long accountId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);


        Account account = new Account(
                accountId,
                UUID.randomUUID().toString(),
                10_000L,
                AccountStatus.ACTIVE,
                now,
                now
        );


        AccountLimitSetting setting = new AccountLimitSetting(
                1L,
                accountId,
                1_000_000L,
                3_000_000L,
                now,
                now
        );
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(accountLimitSettingRepository.findByAccountId(accountId)).willReturn(Optional.of(setting));


        // When
        AccountResponse response = accountService.read(accountId);

        // Then
        assertThat(response.accountId()).isEqualTo(accountId);
        assertThat(response.balance()).isEqualTo(10_000L);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE.name());
        assertThat(response.dailyWithdrawLimit()).isEqualTo(1_000_000L);
        assertThat(response.dailyTransferLimit()).isEqualTo(3_000_000L);

        verify(accountRepository, times(1)).findById(accountId);
        verify(accountLimitSettingRepository, times(1)).findByAccountId(accountId);
    }

    @Test
    @DisplayName("계좌 조회 실패 - 계좌 없음")
    void read_account_fail_account_not_found() {
        // Given
        Long accountId = 999L;
        given(accountRepository.findById(accountId)).willReturn(Optional.empty());


        // When/Then
        CoreException e = assertThrows(CoreException.class, () -> {
            accountService.read(accountId);
        });

        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_FOUND);
        verify(accountRepository, times(1)).findById(accountId);
        verify(accountLimitSettingRepository, never()).findByAccountId(any());
    }

    @Test
    @DisplayName("계좌 조회 실패 - 한도 설정 없음")
    void read_account_fail_limit_setting_not_found() {
        // Given
        Long accountId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);


        Account account = new Account(
                accountId,
                UUID.randomUUID().toString(),
                10_000L,
                AccountStatus.ACTIVE,
                now,
                now
        );

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(accountLimitSettingRepository.findByAccountId(accountId)).willReturn(Optional.empty());

        // When/Then
        CoreException e = assertThrows(CoreException.class, () -> accountService.read(accountId));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_LIMIT_SETTING_NOT_FOUND);
        verify(accountRepository, times(1)).findById(accountId);
        verify(accountLimitSettingRepository, times(1)).findByAccountId(accountId);
    }

    @Test
    @DisplayName("계좌 삭제 성공")
    void delete_account_success() {
        // Given
        Long accountId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        Account account = spy(new Account(
                accountId,
                UUID.randomUUID().toString(),
                10_000L,
                AccountStatus.ACTIVE,
                now,
                now
        ));

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        // When
        accountService.delete(accountId);

        // Then
        verify(accountRepository, times(1)).findById(accountId);
        verify(account, times(1)).close();
    }

    @Test
    @DisplayName("계좌 삭제 실패 - 계좌 없음")
    void delete_account_fail_account_not_found() {
        // Given
        Long accountId = 999L;
        given(accountRepository.findById(accountId)).willReturn(Optional.empty());

        // When/Then
        CoreException e = assertThrows(CoreException.class, () -> accountService.delete(accountId));
        assertThat(e.getErrorType()).isEqualTo(ErrorType.ACCOUNT_NOT_FOUND);

        verify(accountRepository, times(1)).findById(accountId);
    }
}