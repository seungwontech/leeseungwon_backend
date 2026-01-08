package com.sw.remittanceservice.account.dto;

import com.sw.remittanceservice.account.entity.Transaction;

public record TransferResponse(
        String targetAccountNo,
        Long amount,
        Long fee,
        Double feeRate,
        Long balanceAfterTransaction,
        String transactionStatus
) {
    public static TransferResponse from(Transaction entity) {
        return new TransferResponse(
                entity.getTargetAccountNo(),
                entity.getAmount(),
                entity.getFee(),
                entity.getFeeRate(),
                entity.getBalanceAfterTransaction(),
                entity.getTransactionStatus().name()
        );
    }
}
