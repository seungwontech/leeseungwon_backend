package com.sw.remittanceservice.account.repository;

import com.sw.remittanceservice.account.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findLockedByAccountNo(String accountNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findLockedByAccountId(Long accountId);

    Optional<Account> findByAccountNo(String accountNo);

    @Query("select a.accountId from Account a where a.accountNo = :accountNo")
    Optional<Long> findIdByAccountNo(@Param("accountNo") String accountNo);
}
