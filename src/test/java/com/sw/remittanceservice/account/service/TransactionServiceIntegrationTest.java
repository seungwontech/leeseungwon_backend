package com.sw.remittanceservice.account.service;


import com.sw.remittanceservice.account.dto.TransactionPageResponse;
import com.sw.remittanceservice.account.dto.TransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.entity.Transaction;
import com.sw.remittanceservice.account.entity.enums.TransactionStatus;
import com.sw.remittanceservice.account.entity.enums.TransactionType;
import com.sw.remittanceservice.account.repository.AccountRepository;
import com.sw.remittanceservice.account.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("거래 내역 조회 - 계좌번호로 거래내역을 페이지 단위로 조회한다.")
    void readAll_success_integration() {
        // Given
        String accountNo = UUID.randomUUID().toString();

        Account account = accountRepository.save(Account.create(accountNo));
        Long accountId = account.getAccountId();

        for (int i = 0; i < 15; i++) {
            Transaction tx = new Transaction(
                    null,
                    accountId,
                    "tx-" + i,
                    TransactionType.DEPOSIT,
                    TransactionStatus.SUCCESS,
                    10_000L + i,
                    null,
                    0L,
                    null,
                    null,
                    null,
                    10_000L + i,
                    LocalDateTime.now().plusSeconds(i)
            );
            transactionRepository.save(tx);
        }

        Long page = 1L;
        Long pageSize = 10L;

        // When
        TransactionPageResponse response = transactionService.readAll(accountNo, page, pageSize);

        // Then
        assertThat(response.transactionCount()).isEqualTo(15L);
        assertThat(response.transactions()).hasSize(10);

        TransactionResponse first = response.transactions().get(0);
        TransactionResponse second = response.transactions().get(1);

        assertThat(first.transactionType()).isEqualTo(TransactionType.DEPOSIT.toString());
        assertThat(first.transactionStatus()).isEqualTo(TransactionStatus.SUCCESS.toString());

        assertThat(first.balanceAfterTransaction())
                .isGreaterThanOrEqualTo(second.balanceAfterTransaction());
    }


}
