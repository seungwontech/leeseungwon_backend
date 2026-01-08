package com.sw.remittanceservice.account.entity;

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
@Table(name = "account_limit_setting")
public class AccountLimitSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_limit_setting_id")
    @Comment("계좌 한도 아이디")
    private Long accountLimitSettingId;

    @Column(name = "account_id")
    @Comment("계좌 아이디")
    private Long accountId;

    @Column(name = "daily_withdraw_limit", nullable = false)
    @Comment("일 출금 한도")
    private Long dailyWithdrawLimit;

    @Column(name = "daily_transfer_limit", nullable = false)
    @Comment("일 이체 한도")
    private Long dailyTransferLimit;

    @Column(name = "created_at", nullable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    public static AccountLimitSetting defaultOf(Long accountId) {
        LocalDateTime now = LocalDateTime.now();
        return new AccountLimitSetting(
                null,
                accountId,
                1_000_000L, // 출금
                3_000_000L, // 이체
                now,
                now
        );
    }

    public void change(Long dailyWithdrawLimit, Long dailyTransferLimit) {
        this.dailyWithdrawLimit = dailyWithdrawLimit;
        this.dailyTransferLimit = dailyTransferLimit;
        this.updatedAt = LocalDateTime.now();
    }
}
