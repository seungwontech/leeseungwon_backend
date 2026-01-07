package com.sw.remittanceservice.account.repository;

import com.sw.remittanceservice.account.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findLockedByAccountNo(String accountNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findLockedByAccountId(Long accountId);

    Optional<Account> findByAccountNo(String accountNo);
}
