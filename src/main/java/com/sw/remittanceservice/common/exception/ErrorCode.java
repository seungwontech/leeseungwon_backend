package com.sw.remittanceservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "404: 리소스를 찾을 수 없음"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500: 예상치 못한 오류 발생"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "400: 클라이언트 요청 오류"),
    CONFLICT(HttpStatus.CONFLICT, "409: 요청 충돌 발생");
    private final HttpStatus status;
    private final String desc;


}
