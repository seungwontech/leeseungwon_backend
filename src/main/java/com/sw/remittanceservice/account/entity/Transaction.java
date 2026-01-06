package com.sw.remittanceservice.account.entity;

import com.sw.remittanceservice.account.entity.enums.TransactionStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
import com.sw.remittanceservice.account.usecase.policy.dto.FeeResponse;
import com.sw.remittanceservice.account.usecase.policy.dto.enums.FeePolicyType;
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
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    @Comment("거래 아이디")
    private Long transactionId;

    @Column(name = "account_id")
    @Comment("계좌 아이디")
    private Long accountId;

    @Column(name = "transaction_request_id", nullable = false, unique = true)
    @Comment("거래 요청 식별자 (중복 방지용)")
    private String transactionRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    @Comment("거래 유형")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false)
    @Comment("거래 상태")
    private TransactionStatus transactionStatus;

    @Column(name = "amount", nullable = false)
    @Comment("거래 금액")
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_policy_type")
    @Comment("적용된 수수료 정책 타입")
    private FeePolicyType feePolicyType;

    @Column(name = "fee", nullable = false)
    @Comment("수수료(금액)")
    private Long fee;

    @Column(name = "fee_rate")
    @Comment("적용된 수수료율")
    private Double feeRate;

    @Column(name = "fee_applied_at")
    @Comment("수수료 적용 기준 시각")
    private LocalDateTime feeAppliedAt;

    @Column(name = "target_account_no")
    @Comment("상대방 계좌번호")
    private Long targetAccountNo;

    @Column(name = "balance_after_transaction", nullable = false)
    @Comment("거래 후 잔액")
    private Long balanceAfterTransaction;

    @Column(name = "created_at", nullable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    public static Transaction withdrawPending(
            Long accountId,
            String transactionRequestId,
            Long amount
    ) {
        return new Transaction(
                null,
                accountId,
                transactionRequestId,
                TransactionType.WITHDRAW,
                TransactionStatus.PENDING,
                amount,
                null,
                0L,
                null,
                null,
                null,
                0L,
                LocalDateTime.now()
        );
    }

    public static Transaction depositPending(
            Long accountId,
            String transactionRequestId,
            Long amount
    ) {
        return new Transaction(
                null,
                accountId,
                transactionRequestId,
                TransactionType.DEPOSIT,
                TransactionStatus.PENDING,
                amount,
                null,
                0L,
                null,
                null,
                null,
                0L,
                LocalDateTime.now()
        );
    }

    public static Transaction transferPending(
            Long accountId,
            Long targetAccountId,
            String transactionRequestId,
            Long amount
    ) {
        return new Transaction(
                null,
                accountId,
                transactionRequestId,
                TransactionType.TRANSFER,
                TransactionStatus.PENDING,
                amount,
                null,
                0L,
                null,
                null,
                targetAccountId,
                0L,
                LocalDateTime.now()
        );
    }

    public void success(long balanceAfter) {
        this.transactionStatus = TransactionStatus.SUCCESS;
        this.balanceAfterTransaction = balanceAfter;
        this.fee = 0L;
    }

    public void success(long balanceAfter, long fee, FeeResponse feeResponse) {
        this.transactionStatus = TransactionStatus.SUCCESS;
        this.balanceAfterTransaction = balanceAfter;
        this.fee = fee;
        this.feePolicyType = feeResponse.type();
        this.feeRate = feeResponse.rate();
        this.feeAppliedAt = LocalDateTime.now();
    }
}
