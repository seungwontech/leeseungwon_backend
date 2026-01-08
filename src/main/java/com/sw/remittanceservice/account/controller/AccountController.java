package com.sw.remittanceservice.account.controller;

import com.sw.remittanceservice.account.dto.*;
import com.sw.remittanceservice.account.service.AccountService;
import com.sw.remittanceservice.account.usecase.DepositUseCase;
import com.sw.remittanceservice.account.usecase.TransferUseCase;
import com.sw.remittanceservice.account.usecase.WithdrawUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    private final DepositUseCase depositUseCase;

    private final TransferUseCase transferUseCase;

    private final WithdrawUseCase withdrawUseCase;


    @GetMapping("/api/accounts/{accountNo}")
    public ResponseEntity<AccountResponse> read(@PathVariable String accountNo) {
        return ResponseEntity.ok(accountService.read(accountNo));
    }

    @PostMapping("/api/accounts")
    public ResponseEntity<AccountResponse> create() {
        return ResponseEntity.ok(accountService.create());
    }

    @DeleteMapping("/api/accounts/{accountNo}")
    public ResponseEntity<Void> delete(@PathVariable String accountNo) {
        accountService.delete(accountNo);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/api/accounts/{accountNo}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable String accountNo,
            @RequestBody AccountAmountRequest request
    ) {
        return ResponseEntity.ok(withdrawUseCase.execute(accountNo, request.amount(), request.transactionRequestId()));
    }

    @PostMapping("/api/accounts/{accountNo}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable String accountNo,
            @RequestBody AccountAmountRequest request)
    {
        return ResponseEntity.ok(depositUseCase.execute(accountNo, request.amount(), request.transactionRequestId()));
    }


    @PostMapping("/api/transfers")
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferUseCase.execute(request.fromAccountNo(), request.toAccountNo(), request.amount(), request.transactionRequestId()));
    }

}
