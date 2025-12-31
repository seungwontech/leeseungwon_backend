package com.sw.remittanceservice.common;

import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    /**
     * CoreException을 처리하는 메서드
     */
    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ErrorResponse> handleCoreException(CoreException e) {

        ErrorResponse errorResponse = new ErrorResponse(
                e.getErrorType().getErrorCode(), e.getErrorType().getMessage(), e.getPayload()
        );
        return new ResponseEntity<>(errorResponse, e.getErrorType().getErrorCode().getStatus());
    }
}