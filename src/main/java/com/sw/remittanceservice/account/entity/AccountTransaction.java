package com.sw.remittanceservice.account.entity;

import com.sw.remittanceservice.account.entity.enums.AccountTransactionType;
import com.sw.remittanceservice.account.entity.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "account_transaction")
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_transaction_id")
    @Comment("계좌 내역 아이디")
    private Long accountTransactionId;

    @Column(name = "account_id")
    @Comment("계좌 아이디")
    private Long accountId;

    @Column(name = "transaction_id", nullable = false, unique = true)
    @Comment("거래 아이디")
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_transaction_type", nullable = false)
    @Comment("계좌 거래 유형")
    private AccountTransactionType accountTransactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false)
    @Comment("계좌 거래 상태")
    private TransactionStatus transactionStatus;

    @Column(name = "amount", nullable = false)
    @Comment("거래 금액")
    private Long amount;

    @Column(name = "fee", nullable = false)
    @Comment("수수료")
    private Long fee;

    @Column(name = "target_account_no")
    @Comment("상대방 계좌번호 (이체 시 필요)")
    private Long targetAccountId;

    @Column(name = "balance_after_transaction", nullable = false)
    @Comment("거래 후 잔액")
    private Long balanceAfterTransaction;

    @Column(name = "created_at", nullable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    public static AccountTransaction withdrawPending(
            Long accountId,
            String transactionId,
            Long amount
    ) {
        return new AccountTransaction(
                null,
                accountId,
                transactionId,
                AccountTransactionType.WITHDRAW,
                TransactionStatus.PENDING,
                amount,
                0L,
                null,
                0L,
                LocalDateTime.now()
        );
    }

    public static AccountTransaction depositPending(
            Long accountId,
            String transactionId,
            Long amount
    ) {
        return new AccountTransaction(
                null,
                accountId,
                transactionId,
                AccountTransactionType.DEPOSIT,
                TransactionStatus.PENDING,
                amount,
                0L,
                null,
                0L,
                LocalDateTime.now()
        );
    }



    public static AccountTransaction transferPending(
            Long accountId,
            Long targetAccountId,
            String transactionId,
            Long amount
    ) {
        return new AccountTransaction(
                null,
                accountId,
                transactionId,
                AccountTransactionType.TRANSFER,
                TransactionStatus.PENDING,
                amount,
                0L,
                targetAccountId,
                0L,
                LocalDateTime.now()
        );
    }


    public void success(long balanceAfter, long fee) {
        this.transactionStatus = TransactionStatus.SUCCESS;
        this.balanceAfterTransaction = balanceAfter;
        this.fee = fee;
    }

}
