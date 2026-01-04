package com.sw.remittanceservice.account.entity;

import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.of(2026, 01, 02, 0, 0);
        account = new Account(
                null,
                UUID.randomUUID().toString(),
                10000L,
                AccountStatus.ACTIVE,
                now,
                now
        );
    }

    @Test
    @DisplayName("출금 성공")
    void withdraw_success() {
        // Given
        long amount = 3000L;
        long initialBalance = account.getBalance();
        // When
        Account updatedAccount = account.withdraw(amount);
        // Then
        assertEquals(initialBalance - amount, updatedAccount.getBalance());
        assertTrue(updatedAccount.getUpdatedAt().isAfter(account.getUpdatedAt()));
    }


    @Test
    @DisplayName("출금 실패 - 잔액보다 큰 금액을 출금하려는 경우 예외 발생")
    void withdraw_failure_insufficient() {
        // Given
        long amount = 12000L;
        long initialBalance = account.getBalance();

        // When & Then
        CoreException e = assertThrows(CoreException.class, () -> {
            account.withdraw(amount);
        });

        assertEquals(ErrorType.INSUFFICIENT_BALANCE, e.getErrorType());
        assertEquals("잔액이 부족합니다.", e.getMessage());
        assertEquals(initialBalance, account.getBalance(), "출금 실패 시 잔액은 변하지 않아야 합니다.");
    }


    @Test
    @DisplayName("출금 실패 - 출금 금액이 0원 이하일 때")
    void withdraw_failure_invalidAmount() {
        // Given
        long amount = 0L;
        long initialBalance = account.getBalance();

        // When & Then
        CoreException e = assertThrows(CoreException.class, () -> {
            account.withdraw(amount);
        });

        assertEquals(ErrorType.INVALID_REQUEST, e.getErrorType());
        assertEquals("요청 값이 올바르지 않습니다.", e.getMessage());
        // 잔액이 변하지 않았는지 확인
        assertEquals(initialBalance, account.getBalance(), "출금 실패 시 잔액은 변하지 않아야 합니다.");
    }

    @Test
    @DisplayName("입금 성공")
    void deposit_success() {
        // Given
        long amount = 3000L;
        long initialBalance = account.getBalance();
        // When
        Account updatedAccount = account.deposit(amount);
        // Then
        assertEquals(initialBalance + amount, updatedAccount.getBalance());
        assertTrue(updatedAccount.getUpdatedAt().isAfter(account.getUpdatedAt()));
    }

    @Test
    @DisplayName("입금 실패 - 출금 금액이 0원 이하일 때")
    void deposit_failure_invalidAmount() {
        // Given
        long amount = 0L;
        long initialBalance = account.getBalance();

        // When & Then
        CoreException e = assertThrows(CoreException.class, () -> {
            account.deposit(amount);
        });

        assertEquals(ErrorType.INVALID_REQUEST, e.getErrorType());
        assertEquals("요청 값이 올바르지 않습니다.", e.getMessage());
        // 잔액이 변하지 않았는지 확인
        assertEquals(initialBalance, account.getBalance(), "입금 실패 시 잔액은 변하지 않아야 합니다.");
    }

}
