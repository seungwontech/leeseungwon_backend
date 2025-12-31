package com.sw.remittanceservice.account.controller;

import com.sw.remittanceservice.account.dto.*;
import com.sw.remittanceservice.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/api/accounts/{accountId}")
    public ResponseEntity<AccountResponse> read(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.read(accountId));
    }

    @PostMapping("/api/accounts")
    public ResponseEntity<AccountResponse> create(@RequestBody AccountCreateRequest request) {
        return ResponseEntity.ok(accountService.create(request));
    }

    @DeleteMapping("/api/accounts/{accountId}")
    public ResponseEntity<Void> delete(@PathVariable Long accountId) {
        accountService.delete(accountId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/api/accounts/{accountId}/withdraw")
    public ResponseEntity<AccountTransactionResponse> withdraw(
            @PathVariable Long accountId,
            @RequestBody AccountAmountRequest request
    ) {
        return ResponseEntity.ok(accountService.withdraw(accountId, request.amount(), request.transactionId()));
    }

    @PostMapping("/api/accounts/{accountId}/deposit")
    public ResponseEntity<AccountTransactionResponse> deposit(
            @PathVariable Long accountId,
            @RequestBody AccountAmountRequest request)
    {
        return ResponseEntity.ok(accountService.deposit(accountId, request.amount(), request.transactionId()));
    }


    @PostMapping("/api/transfers")
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(accountService.transfer(request.fromAccountId(), request.toAccountId(), request.amount(), request.transactionId()));
    }

}
