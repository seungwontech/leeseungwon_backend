package com.sw.remittanceservice.account.dto;

import java.util.List;

public record TransactionPageResponse(List<TransactionResponse> transactions, Long transactionCount) {
    public static TransactionPageResponse of(List<TransactionResponse> transactions, Long transactionCount) {
        return new TransactionPageResponse(transactions, transactionCount);
    }
}
