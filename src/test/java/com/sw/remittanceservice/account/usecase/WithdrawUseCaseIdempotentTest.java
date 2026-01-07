package com.sw.remittanceservice.account.usecase;

import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.AccountLimitSetting;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.AccountStatus;
import com.sw.remittanceservice.account.repository.AccountLimitSettingRepository;
import com.sw.remittanceservice.account.repository.AccountRepository;
import com.sw.remittanceservice.account.repository.TransactionRepository;
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
public class WithdrawUseCaseIdempotentTest {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountLimitSettingRepository accountLimitSettingRepository;

    @Autowired
    TransactionRepository accountTransactionRepository;

    @Autowired
    WithdrawUseCase withdrawUseCase;

    private String accountNo;
    private Long accountId;
    private final long balance = 100_000;       // 10만원
    private final long withdrawAmount = 5_000L;   // 5천원
    private final int requestCount = 10;          // 10개 요청


    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 16, 10, 0);
        accountNo = UUID.randomUUID().toString();
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
    @DisplayName("멱등성: transactionId가 같으면 한번만 출금한다.")
    void concurrent_idempotent_test() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);

        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        String sameTransactionId = "a0e12a15-46c8-4bd4-ac9b-2482b9f2e3ed";

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    withdrawUseCase.execute(accountNo, withdrawAmount, sameTransactionId);
                    success.incrementAndGet();
                } catch (Exception e) {
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
        Transaction transaction = accountTransactionRepository.findByTransactionRequestId(sameTransactionId).orElseThrow();

        assertThat(result.getBalance()).isEqualTo(balance - withdrawAmount);

        assertThat(fail.get()).isEqualTo(0);
        assertThat(accountTransactionRepository.count()).isEqualTo(1);
        assertThat(accountTransactionRepository.findByTransactionRequestId(sameTransactionId)).isPresent();

        System.out.println("========================================");
        System.out.println("[TEST_END]");
        System.out.println("- finalBalance=" + result.getBalance());
        System.out.println("- expectedBalance=" + (balance - withdrawAmount));
        System.out.println("- success=" + success.get());
        System.out.println("- fail=" + fail.get());
        System.out.println("- historyCount=" + historyCount);
        System.out.println("- status=" + transaction.getTransactionStatus().name());
        System.out.println("========================================");

    }

}
