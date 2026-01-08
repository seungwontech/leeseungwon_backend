package com.sw.remittanceservice.account.usecase.policy;

import com.sw.remittanceservice.account.usecase.policy.dto.FeeRequest;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeCalculatorFinderTest {

    @Mock
    private FeeCalculator nightCalculator;

    @Mock
    private FeeCalculator defaultCalculator;

    private FeeCalculatorFinder finder;

    @BeforeEach
    void setUp() {
        finder = new FeeCalculatorFinder(List.of(nightCalculator, defaultCalculator));
    }


    @Test
    @DisplayName("적용 가능한 특수 정책(야간)이 있으면 해당 계산기를 실행한다")
    void should_return_night_fee_when_applicable() {
        // Given
        given(nightCalculator.type()).willReturn(FeePolicyType.NIGHT);
        FeeRequest request = new FeeRequest(10000L);
        FeeResponse expectedResponse = new FeeResponse(FeePolicyType.NIGHT, 0.02, 200L, LocalDateTime.now());

        given(nightCalculator.applicable(request)).willReturn(true);
        given(nightCalculator.calculate(request)).willReturn(expectedResponse);

        // When
        FeeResponse result = finder.calculate(request);

        // Then
        assertThat(result.type()).isEqualTo(FeePolicyType.NIGHT);
        assertThat(result.feeAmount()).isEqualTo(200L);
        then(nightCalculator).should(times(1)).calculate(request);
        then(defaultCalculator).should(never()).calculate(any());
    }

    @Test
    @DisplayName("특수 정책이 적용 불가능하면 기본(DEFAULT) 정책을 실행한다")
    void should_return_default_fee_when_no_others_applicable() {
        // Given
        given(defaultCalculator.type()).willReturn(FeePolicyType.DEFAULT);
        FeeRequest request = new FeeRequest(10000L);
        FeeResponse defaultResponse = new FeeResponse(FeePolicyType.DEFAULT, 0.01, 100L, LocalDateTime.now());

        given(nightCalculator.applicable(request)).willReturn(false);
        given(defaultCalculator.calculate(request)).willReturn(defaultResponse);

        // When
        FeeResponse result = finder.calculate(request);

        // Then
        assertThat(result.type()).isEqualTo(FeePolicyType.DEFAULT);
        assertThat(result.feeAmount()).isEqualTo(100L);

        then(defaultCalculator).should(times(1)).calculate(request);
    }

    @Test
    @DisplayName("기본 정책(DEFAULT)이 리스트에 없으면 예외가 발생한다")
    void should_throw_exception_when_default_calculator_is_missing() {
        // Given
        given(nightCalculator.type()).willReturn(FeePolicyType.NIGHT);
        FeeCalculatorFinder brokenFinder = new FeeCalculatorFinder(List.of(nightCalculator));

        FeeRequest request = new FeeRequest(10000L);
        given(nightCalculator.applicable(request)).willReturn(false);

        // When & Then
        CoreException e = assertThrows(CoreException.class,
                () -> brokenFinder.calculate(request));
        assertThat(e.getErrorType()).isEqualTo(ErrorType.CALCULATOR_NOT_FOUND);
    }


    @Test
    @DisplayName("여러 특수 정책 중 첫 번째가 불가능하면 다음 정책을 탐색한다")
    void should_try_next_policy_when_first_one_is_not_applicable() {
        // Given: 또 다른 특수 정책(예: VIP)이 있다고 가정 (Mock 하나 더 추가 필요)
        FeeCalculator vipCalculator = mock(FeeCalculator.class);
        given(vipCalculator.type()).willReturn(FeePolicyType.NIGHT); // 편의상 NIGHT 타입으로 설정

        FeeCalculatorFinder multiFinder = new FeeCalculatorFinder(List.of(vipCalculator, nightCalculator, defaultCalculator));

        FeeRequest request = new FeeRequest(10000L);
        given(vipCalculator.applicable(request)).willReturn(false);
        given(nightCalculator.applicable(request)).willReturn(true);
        given(nightCalculator.calculate(request)).willReturn(new FeeResponse(FeePolicyType.NIGHT, 0.02, 200L, LocalDateTime.now()));

        // When
        FeeResponse result = multiFinder.calculate(request);

        // Then
        assertThat(result.type()).isEqualTo(FeePolicyType.NIGHT);
        then(vipCalculator).should().applicable(request); // 첫 번째 시도함
        then(nightCalculator).should().calculate(request); // 두 번째에서 성공함
    }
}