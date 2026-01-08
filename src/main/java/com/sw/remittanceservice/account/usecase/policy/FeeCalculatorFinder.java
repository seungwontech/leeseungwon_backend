package com.sw.remittanceservice.account.usecase.policy;

import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@RequiredArgsConstructor
@Component
public class FeeCalculatorFinder {

    private final List<FeeCalculator> calculators;

    public FeeResponse calculate(FeeRequest request) {
        FeeCalculator defaultCalc = null;

        for (FeeCalculator c : calculators) {
            if (c.type() == FeePolicyType.DEFAULT) {
                defaultCalc = c;
                continue;
            }
            if (c.applicable(request)) {
                return c.calculate(request);
            }
        }

        if (defaultCalc == null) {
            throw new CoreException(ErrorType.CALCULATOR_NOT_FOUND, request);
        }
        return defaultCalc.calculate(request);
    }
}
