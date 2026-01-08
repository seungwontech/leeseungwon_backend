package com.sw.remittanceservice.account.usecase.policy;

import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class NightFeeCalculatorTest {

    private final NightFeeCalculator calculator = new NightFeeCalculator();

    @ParameterizedTest
    @CsvSource({
            "23:00, true",  // 야간 시작 시각
            "23:01, true",  // 야간
            "00:00, true",  // 자정
            "05:59, true",  // 야간 종료 직전
            "06:00, false", // 야간 종료 (주간 시작)
            "12:00, false", // 낮
            "22:59, false"  // 야간 시작 직전
    })
    @DisplayName("시간별 야간 수수료 적용 여부 검증 (경계값 포함)")
    void night_time_check(String timeStr, boolean expected) {
        // Given
        LocalTime time = LocalTime.parse(timeStr);

        // When
        boolean result = calculator.isNight(time);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("야간 수수료 계산 로직 검증 (금액의 2%)")
    void calculate_fee_amount() {
        // Given
        long amount = 100_000L;
        FeeRequest request = new FeeRequest(amount);

        // When
        FeeResponse response = calculator.calculate(request);

        // Then
        assertThat(response.type()).isEqualTo(FeePolicyType.NIGHT);
        assertThat(response.rate()).isEqualTo(0.02);
        assertThat(response.feeAmount()).isEqualTo(2000L); // 100,000 * 0.02
    }
}