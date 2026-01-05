package com.sw.remittanceservice.common.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    TX_NOT_FOUND(ErrorCode.NOT_FOUND, "트랜잭션을 찾을 수 없습니다."),
    ACCOUNT_NOT_FOUND(ErrorCode.NOT_FOUND, "계좌을 찾을 수 없습니다."),
    ACCOUNT_LIMIT_SETTING_NOT_FOUND(ErrorCode.NOT_FOUND, "계좌 한도 설정을 찾을 수 없습니다."),
    INVALID_REQUEST(ErrorCode.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    SAME_ACCOUNT_TRANSFER(ErrorCode.BAD_REQUEST, "출금계좌와 동일한 계좌입니다."),
    INSUFFICIENT_BALANCE(ErrorCode.BAD_REQUEST, "잔액이 부족합니다."),
    EXCEED_DAILY_WITHDRAW_LIMIT(ErrorCode.BAD_REQUEST, "일일 출금 한도 초과했습니다."),
    EXCEED_DAILY_TRANSFER_LIMIT(ErrorCode.BAD_REQUEST, "일일 이체 한도 초과했습니다."),
    CALCULATOR_NOT_FOUND(ErrorCode.NOT_FOUND, "수수료 계산기를 찾을 수 없습니다.");

    private final ErrorCode errorCode;
    private final String message;
}