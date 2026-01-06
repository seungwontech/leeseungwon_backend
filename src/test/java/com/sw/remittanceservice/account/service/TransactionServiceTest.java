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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("거래 내역 조회 - 계좌번호로 거래내역을 페이지 단위로 조회한다.")
    void readAll_success() {
        // given
        String accountNo = UUID.randomUUID().toString();
        Long page = 1L;
        Long pageSize = 10L;

        Long accountId = 1L;
        Long offset = (page - 1) * pageSize;
        Long countLimit = PageLimitCalculator.calculatePageLimit(page, pageSize, 10L);


        Account account = mock(Account.class);
        given(account.getAccountId()).willReturn(accountId);
        given(accountRepository.findByAccountNo(accountNo)).willReturn(Optional.of(account));

        // 트랜잭션 더미 데이터 2건
        Transaction tx1 = new Transaction(
                100L,
                accountId,
                "tx-1",
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                10_000L,
                null,
                0L,
                null,
                null,
                null,
                10_000L,
                LocalDateTime.now()
        );

        Transaction tx2 = new Transaction(
                101L,
                accountId,
                "tx-2",
                TransactionType.WITHDRAW,
                TransactionStatus.SUCCESS,
                5_000L,
                null,
                0L,
                null,
                null,
                null,
                5_000L,
                LocalDateTime.now()
        );

        when(transactionRepository.findAllByAccountId(accountId, offset, pageSize))
                .thenReturn(List.of(tx1, tx2));

        when(transactionRepository.count(accountId, countLimit))
                .thenReturn(2L);

        // when
        TransactionPageResponse response = transactionService.readAll(accountNo, page, pageSize);

        // then
        assertThat(response.transactionCount()).isEqualTo(2L);
        assertThat(response.transactions()).hasSize(2);

        TransactionResponse first = response.transactions().get(0);
        assertThat(first.transactionType()).isEqualTo(TransactionType.DEPOSIT.toString());
        assertThat(first.transactionStatus()).isEqualTo(TransactionStatus.SUCCESS.toString());
        assertThat(first.amount()).isEqualTo(10_000L);

        // 상호작용 검증
        verify(accountRepository, times(1)).findByAccountNo(accountNo);
        verify(transactionRepository, times(1))
                .findAllByAccountId(accountId, offset, pageSize);
        verify(transactionRepository, times(1))
                .count(accountId, countLimit);
    }
}