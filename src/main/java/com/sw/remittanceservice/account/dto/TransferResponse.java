package com.sw.remittanceservice.account.dto;

import com.sw.remittanceservice.account.entity.AccountTransaction;

public record TransferResponse(
        Long fromAccountId,
        Long toAccountId,
        Long amount,
        Long fee,
        Double feeRate,
        Long balanceAfterTransaction,
        String transactionStatus) {

    public static TransferResponse from(AccountTransaction entity) {
        return new TransferResponse(
                entity.getAccountId(),
                entity.getTargetAccountNo(),
                entity.getAmount(),
                entity.getFee(),
                entity.getFeeRate(),
                entity.getBalanceAfterTransaction(),
                entity.getTransactionStatus().name()
        );
    }
}
