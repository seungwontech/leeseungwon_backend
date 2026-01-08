package com.sw.remittanceservice.account.usecase.policy;

import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;

public interface FeeCalculator {

    FeePolicyType type();

    boolean applicable(FeeRequest request);

    FeeResponse calculate(FeeRequest request);
}
