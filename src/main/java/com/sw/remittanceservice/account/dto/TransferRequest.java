package com.sw.remittanceservice.account.dto;

public record TransferRequest(String fromAccountNo, String toAccountNo, Long amount, String transactionRequestId) {
}
