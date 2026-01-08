package com.sw.remittanceservice.account.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;


@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(
        name = "account_daily_limit_usage",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_account_date", columnNames = {"account_id","limit_date"})
        }
)
public class AccountDailyLimitUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_daily_limit_usage_id")
    @Comment("계좌 한도 아이디")
    private Long id;

    @Column(name = "account_id")
    @Comment("계좌 아이디")
    private Long accountId;

    @Column(name = "limit_date", nullable = false)
    @Comment("한도 기준 일자(YYYY-MM-DD)")
    private LocalDate limitDate;

    @Column(name = "withdraw_used", nullable = false)
    @Comment("오늘 출금 누적")
    private Long withdrawUsed;

    @Column(name = "transfer_used", nullable = false)
    @Comment("오늘 이체 누적")
    private Long transferUsed;

    @Column(name = "created_at", nullable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    public static AccountDailyLimitUsage init(Long accountId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        return new AccountDailyLimitUsage(null, accountId, date, 0L, 0L, now, now);
    }

    public void addWithdrawUsed(long amount) {
        this.withdrawUsed += amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void addTransferUsed(long amount) {
        this.transferUsed += amount;
        this.updatedAt = LocalDateTime.now();
    }
}
