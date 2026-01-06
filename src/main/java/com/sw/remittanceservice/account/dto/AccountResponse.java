package com.sw.remittanceservice.account.dto;

import com.sw.remittanceservice.account.entity.Account;

public record AccountResponse(
        String accountNo,
        Long balance,
        String accountStatus,
        Long dailyWithdrawLimit,
        Long dailyTransferLimit
) {
    public static AccountResponse from(Account entity, Long dailyWithdrawLimit, Long dailyTransferLimit) {
        return new AccountResponse(
                entity.getAccountNo(),
                entity.getBalance(),
                entity.getAccountStatus().toString(),
                dailyWithdrawLimit,
                dailyTransferLimit
        );
    }
}
