package com.sw.remittanceservice.account.repository;

import com.sw.remittanceservice.account.entity.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
    Optional<AccountTransaction> findByTransactionId(String transactionId);
}
