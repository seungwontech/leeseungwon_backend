package com.sw.remittanceservice.account.repository;

import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountLimitSettingRepository extends JpaRepository<AccountLimitSetting, Long> {
    Optional<AccountLimitSetting> findByAccountId(Long accountId);
}
