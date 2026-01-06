package com.sw.remittanceservice.account.service;

import com.sw.remittanceservice.account.dto.TransactionPageResponse;
import com.sw.remittanceservice.account.dto.TransactionResponse;
import com.sw.remittanceservice.account.entity.Account;
import com.sw.remittanceservice.account.repository.AccountRepository;
import com.sw.remittanceservice.account.repository.TransactionRepository;
import com.sw.remittanceservice.common.exception.CoreException;
import com.sw.remittanceservice.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    private final AccountRepository accountRepository;

    public TransactionPageResponse readAll(String accountNo, Long page, Long pageSize) {

        Account account = accountRepository.findByAccountNo(accountNo).orElseThrow(() -> new CoreException(ErrorType.ACCOUNT_NOT_FOUND, accountNo));

        Long accountId = account.getAccountId();

        List<TransactionResponse> transactions = transactionRepository.findAllByAccountId(accountId, (page - 1) * pageSize, pageSize).stream()
                .map(TransactionResponse::from)
                .toList();

        Long transactionCount = transactionRepository.count(accountId, PageLimitCalculator.calculatePageLimit(page, pageSize, 10L));

        return TransactionPageResponse.of(transactions, transactionCount);
    }
}
