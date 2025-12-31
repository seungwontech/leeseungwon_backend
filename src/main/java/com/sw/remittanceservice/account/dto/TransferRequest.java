package com.sw.remittanceservice.account.dto;

public record TransferRequest(Long fromAccountId, Long toAccountId, Long amount, String transactionId) {
}
