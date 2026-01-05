package com.sw.remittanceservice.account.usecase.policy.dto;

import java.time.LocalDateTime;

public record FeeRequest(long amount, LocalDateTime requestedAt) {
}
