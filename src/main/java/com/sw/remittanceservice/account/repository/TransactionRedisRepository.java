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

    private static final String TRANSACTION_ID_KEY_FORMAT = "transaction-id-lock::%s";


    private String generateKey(String transactionId) {
        return TRANSACTION_ID_KEY_FORMAT.formatted(transactionId);
    }

    public boolean isLocked(String transactionId, long ttlSeconds) {
        String key = generateKey(transactionId);
        return Boolean.FALSE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, TransactionStatus.PENDING.name(), Duration.ofSeconds(ttlSeconds))
        );
    }
}
