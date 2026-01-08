package com.sw.remittanceservice.account.repository;

import com.sw.remittanceservice.account.entity.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class TransactionRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String TRANSACTION_REQUEST_ID_KEY_FORMAT = "transaction-request-id-lock::%s";


    private String generateKey(String transactionRequestId) {
        return TRANSACTION_REQUEST_ID_KEY_FORMAT.formatted(transactionRequestId);
    }

    public boolean tryLock(String transactionRequestId, long ttlSeconds) {
        String key = generateKey(transactionRequestId);
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, TransactionStatus.PENDING.name(), Duration.ofSeconds(ttlSeconds))
        );
    }
}
