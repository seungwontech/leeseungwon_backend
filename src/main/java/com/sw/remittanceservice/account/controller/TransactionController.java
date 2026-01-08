package com.sw.remittanceservice.account.controller;

import com.sw.remittanceservice.account.dto.TransactionPageResponse;
import com.sw.remittanceservice.account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class TransactionController {

    private final TransactionService accountTransactionService;

    @GetMapping("/api/accounts/{accountNo}/transactions")
    public ResponseEntity<TransactionPageResponse> readAll(
            @PathVariable String accountNo,
            @RequestParam("page") Long page,
            @RequestParam("pageSize") Long pageSize
    ) {
        return ResponseEntity.ok(accountTransactionService.readAll(accountNo, page, pageSize));
    }
}
