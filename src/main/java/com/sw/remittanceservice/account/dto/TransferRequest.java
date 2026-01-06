package com.sw.remittanceservice.account.dto;

public record TransferRequest(Long fromAccountId, Long toAccountNo, Long amount, String transactionId) {
}
