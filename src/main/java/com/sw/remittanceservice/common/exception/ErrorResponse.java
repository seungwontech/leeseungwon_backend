package com.sw.remittanceservice.common.exception;

public record ErrorResponse(
        ErrorCode errorCode,
        String message,
        Object payload
) {
}