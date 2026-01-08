package com.sw.remittanceservice.account.repository;

import com.sw.remittanceservice.account.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByAccountIdAndTransactionRequestId(Long accountId, String transactionRequestId);

    @Query(
            value = """
                    select at.transaction_id,
                           at.account_id,
                           at.transaction_request_id,
                           at.transaction_type,
                           at.transaction_status,
                           at.amount,
                           at.fee_policy_type,
                           at.fee,
                           at.fee_rate,
                           at.fee_applied_at,
                           at.target_account_no,
                           at.balance_after_transaction,
                           at.created_at
                    from (
                       select transaction_id from transaction
                       where account_id = :accountId
                       order by created_at desc 
                       limit :limit offset :offset
                    ) t left join transaction at on t.transaction_id = at.transaction_id
                    """,
            nativeQuery = true
    )
    List<Transaction> findAllByAccountId(
            @Param("accountId") Long accountId,
            @Param("offset") Long offset,
            @Param("limit") Long limit
    );


    @Query(
            value = """
                    select count(*) 
                    from (
                        select transaction_id from transaction where account_id = :accountId limit :limit
                    ) t
                    """,
            nativeQuery = true
    )
    Long count(@Param("accountId") Long accountId, @Param("limit") Long limit);
}
