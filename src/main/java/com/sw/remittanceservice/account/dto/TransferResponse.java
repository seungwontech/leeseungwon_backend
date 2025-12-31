package com.sw.remittanceservice.account.dto;

import com.sw.remittanceservice.account.entity.AccountTransaction;
import com.sw.remittanceservice.account.entity.enums.TransactionStatus;

public record TransferResponse(Long fromAccountId, Long toAccountId, Long amount, Long fee, Long balanceAfterTransaction, String transactionStatus) {

    public static TransferResponse from(AccountTransaction entity) {
        return new TransferResponse(
                entity.getAccountId(),
                entity.getTargetAccountId(),
                entity.getAmount(),
                entity.getFee(),
                entity.getBalanceAfterTransaction(),
                entity.getTransactionStatus().name()
        );
    }

    public static TransferResponse of(Long fromId, Long toId, Long amount, Long fee, TransactionStatus transactionStatus) {
        return new TransferResponse(
                fromId,
                toId,
                amount,
                fee,
                0L,
                transactionStatus.name()
        );
    }

}
