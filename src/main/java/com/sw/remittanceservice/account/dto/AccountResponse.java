package com.sw.remittanceservice.account.dto;

import com.sw.remittanceservice.account.entity.Account;

public record AccountResponse(
        Long accountId,
        String accountNo,
        Long balance,
        String accountStatus,
        Long dailyWithdrawLimit,
        Long dailyTransferLimit
) {
    public static AccountResponse from(Account entity, Long dailyWithdrawLimit, Long dailyTransferLimit) {
        return new AccountResponse(
                entity.getAccountId(),
                entity.getAccountNo(),
                entity.getBalance(),
                entity.getAccountStatus().name(),
                dailyWithdrawLimit,
                dailyTransferLimit
        );
    }
}
