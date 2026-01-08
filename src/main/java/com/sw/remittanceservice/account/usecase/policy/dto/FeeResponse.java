package com.sw.remittanceservice.account.usecase.policy.dto;

import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;

import java.time.LocalDateTime;

public record FeeResponse(FeePolicyType type, double rate, long feeAmount, LocalDateTime requestedAt) {
}
