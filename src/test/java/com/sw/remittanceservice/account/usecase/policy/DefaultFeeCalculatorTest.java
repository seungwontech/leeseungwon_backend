package com.sw.remittanceservice.account.usecase.policy;

import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFeeCalculatorTest {

    private final DefaultFeeCalculator calculator = new DefaultFeeCalculator();

    @Test
    @DisplayName("기본 수수료 정책은 항상 적용 가능해야 한다")
    void applicable_test() {
        // given
        FeeRequest request = new FeeRequest(10000L);

        // when
        boolean result = calculator.applicable(request);

        // then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "1000, 10",    // 1,000원의 1% = 10원
            "10000, 100",  // 10,000원의 1% = 100원
            "5500, 55",    // 5,500원의 1% = 55원
            "99, 0"        // 99원의 1% = 0.99 -> 0원 (소수점 절삭 확인)
    })
    @DisplayName("금액에 따른 1% 수수료 계산 검증")
    void calculate_test(long amount, long expectedFee) {
        // given
        FeeRequest request = new FeeRequest(amount);

        // when
        FeeResponse response = calculator.calculate(request);

        // then
        assertThat(response.type()).isEqualTo(FeePolicyType.DEFAULT);
        assertThat(response.rate()).isEqualTo(0.01);
        assertThat(response.feeAmount()).isEqualTo(expectedFee);
    }
}