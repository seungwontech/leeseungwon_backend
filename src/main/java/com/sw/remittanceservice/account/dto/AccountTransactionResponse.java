package com.sw.remittanceservice.account.dto;


import com.sw.remittanceservice.account.entity.AccountTransaction;

import java.time.LocalDateTime;

public record AccountTransactionResponse(Long accountId, Long balanceAfterTransaction, Long amount, String getAccountTransactionType, String getTransactionStatus, LocalDateTime createdAt) {

    public static AccountTransactionResponse from(AccountTransaction entity) {
        return new AccountTransactionResponse(
                entity.getAccountId(),
                entity.getBalanceAfterTransaction(),
                entity.getAmount(),
                entity.getAccountTransactionType().name(),
                entity.getTransactionStatus().name(),
                entity.getCreatedAt()
        );
    }
}
