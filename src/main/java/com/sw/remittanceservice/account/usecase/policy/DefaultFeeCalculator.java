package com.sw.remittanceservice.account.usecase.policy;

import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class DefaultFeeCalculator implements FeeCalculator {

    private static final double RATE = 0.01;

    @Override
    public FeePolicyType type() {
        return FeePolicyType.DEFAULT;
    }

    @Override
    public boolean applicable(FeeRequest request) {
        return true;
    }

    @Override
    public FeeResponse calculate(FeeRequest request) {
        long fee = (long) (request.amount() * RATE);
        return new FeeResponse(FeePolicyType.DEFAULT, RATE, fee, LocalDateTime.now());
    }
}
