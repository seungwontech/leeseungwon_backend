package com.sw.remittanceservice.account.service;

import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.repository.AccountLimitSettingRepository;
import com.sw.remittanceservice.account.repository.AccountRepository;
import com.sw.remittanceservice.account.repository.AccountTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class AccountServiceConcurrencyTest {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountLimitSettingRepository accountLimitSettingRepository;

    @Autowired
    AccountTransactionRepository accountTransactionRepository;

    @Autowired
    AccountService accountService;

    private Long accountId;
    private final long balance = 1_000_000L;       // 100만원
    private final long withdrawAmount = 10_000L;   // 1만원
    private final int requestCount = 100;          // 100개 요청

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 16, 10, 0);
        String accountNo = UUID.randomUUID().toString();
        Account saved = accountRepository.save(
                new Account(null, accountNo, balance, AccountStatus.ACTIVE, now, now)
        );
        accountLimitSettingRepository.save(
                new AccountLimitSetting(
                        null,
                        saved.getAccountId(),
                        1_000_000L,
                        3_000_000L,
                        now,
                        now
                )
        );
        accountId = saved.getAccountId();
    }


    @Test
    @DisplayName("동시성: transactionId가 모두 다르면 모두 출금되어 0원이 된다.")
    void concurrent_withdraw_test() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);

        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    accountService.withdraw(accountId, withdrawAmount, String.valueOf(UUID.randomUUID()));
                    success.incrementAndGet();
                } catch (Exception e) {
                    System.out.println(e);
                    fail.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        Account result = accountRepository.findById(accountId).orElseThrow();
        long historyCount = accountTransactionRepository.count();

        assertThat(fail.get()).isEqualTo(0);
        assertThat(success.get()).isEqualTo(requestCount);
        assertThat(result.getBalance()).isEqualTo(0L);
        assertThat(accountTransactionRepository.count()).isEqualTo(requestCount);

        System.out.println("========================================");
        System.out.println("[TEST_END]");
        System.out.println("- finalBalance=" + result.getBalance());
        System.out.println("- expectedBalance=" + (balance - requestCount * withdrawAmount));
        System.out.println("- success=" + success.get());
        System.out.println("- fail=" + fail.get());
        System.out.println("- historyCount=" + historyCount);
        System.out.println("========================================");

    }

}
