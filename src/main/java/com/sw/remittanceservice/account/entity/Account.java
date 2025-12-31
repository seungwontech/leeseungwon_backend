package com.sw.remittanceservice.account.entity;

import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
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
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    @Comment("계좌 아이디")
    private Long accountId;

    @Column(name = "account_no", nullable = false)
    @Comment("계좌 번호")
    private String accountNo;

    @Column(name = "balance", nullable = false)
    @Comment("잔액")
    private Long balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    @Comment("계좌 상태")
    private AccountStatus accountStatus;

    @Column(name = "created_at", nullable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;


    public Account withdraw(Long amount) {

        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST, amount);
        }

        if (balance < amount) {
            throw new CoreException(ErrorType.INSUFFICIENT_BALANCE, amount);
        }

        return new Account(accountId, accountNo, balance - amount, accountStatus, createdAt, LocalDateTime.now());
    }

    public Account deposit(Long amount) {

        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST, amount);
        }

        return new Account(accountId, accountNo, balance + amount, accountStatus, createdAt, LocalDateTime.now());
    }


    public static Account create(String accountNo) {
        LocalDateTime now = LocalDateTime.now();
        return new Account(null, accountNo, 0L, AccountStatus.ACTIVE, now, now);
    }



    public void close() {
        this.accountStatus = AccountStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }
}
