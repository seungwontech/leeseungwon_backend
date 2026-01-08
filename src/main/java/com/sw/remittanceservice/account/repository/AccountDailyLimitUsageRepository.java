package com.sw.remittanceservice.account.repository;

import com.sw.remittanceservice.account.entity.AccountDailyLimitUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AccountDailyLimitUsageRepository extends JpaRepository<AccountDailyLimitUsage, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountDailyLimitUsage> findLockedByAccountIdAndLimitDate(Long accountId, LocalDate limitDate);

    Optional<AccountDailyLimitUsage> findByAccountIdAndLimitDate(Long accountId, LocalDate limitDate);

    @Query(
            value = "update account_daily_limit_usage set withdraw_used = :withdrawUsed " +
                    "where account_id = :articleId and limit_date =: limitDate",
            nativeQuery = true
    )
    @Modifying
    int updateWithdrawUsed(
            @Param("accountId") Long accountId,
            @Param("limitDate") Long limitDate,
            @Param("withdrawUsed") Long withdrawUsed
    );
}
