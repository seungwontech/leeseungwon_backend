package com.sw.remittanceservice.account.usecase.policy;

import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class NightFeeCalculator implements FeeCalculator {

    private static final double RATE = 0.02;
    private static final LocalTime NIGHT_START = LocalTime.of(23, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(6, 0);

    @Override
    public FeePolicyType type() {
        return FeePolicyType.NIGHT;
    }

    @Override
    public boolean applicable(FeeRequest request) {
        LocalTime t = request.requestedAt().toLocalTime();
        return !t.isBefore(NIGHT_START) || t.isBefore(NIGHT_END);
    }

    @Override
    public FeeResponse calculate(FeeRequest request) {
        long fee = (long) (request.amount() * RATE);
        return new FeeResponse(FeePolicyType.NIGHT, RATE, fee, request.requestedAt());
    }
}
