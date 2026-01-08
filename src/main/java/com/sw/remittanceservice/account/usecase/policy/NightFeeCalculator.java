package com.sw.remittanceservice.account.usecase.policy;

import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
        return isNight(LocalTime.now());
    }

    @Override
    public FeeResponse calculate(FeeRequest request) {
        long fee = (long) (request.amount() * RATE);
        return new FeeResponse(FeePolicyType.NIGHT, RATE, fee, LocalDateTime.now());
    }

    public boolean isNight(LocalTime time) {
        // 23:00 이후이거나 06:00 이전인 경우 (밤 11시 ~ 새벽 6시)
        return !time.isBefore(NIGHT_START) || time.isBefore(NIGHT_END);
    }
}
