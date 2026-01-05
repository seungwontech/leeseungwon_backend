package com.sw.remittanceservice.account.usecase.policy.dto;

import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;

public record FeeResponse(FeePolicyType type, double rate, long feeAmount) {
}
