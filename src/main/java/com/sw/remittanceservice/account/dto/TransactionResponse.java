package com.sw.remittanceservice.account.dto;


import com.sw.remittanceservice.account.entity.Transaction;

import java.time.LocalDateTime;

public record TransactionResponse(
        Long balanceAfterTransaction,
        Long amount,
        String transactionType,
        String transactionStatus,
        Long targetAccountNo,
        Double feeRate,
        LocalDateTime feeAppliedAt,
        LocalDateTime createdAt) {

    public static TransactionResponse from(Transaction entity) {
        return new TransactionResponse(
                entity.getBalanceAfterTransaction(),
                entity.getAmount(),
                entity.getTransactionType().name(),
                entity.getTransactionStatus().name(),
                entity.getTargetAccountNo(),
                entity.getFeeRate(),
                entity.getFeeAppliedAt(),
                entity.getCreatedAt()
        );
    }
}
