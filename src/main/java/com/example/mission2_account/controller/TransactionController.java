package com.example.mission2_account.controller;

import com.example.mission2_account.aop.AccountLock;
import com.example.mission2_account.dto.CancelBalance;
import com.example.mission2_account.dto.QueryTransactionResponse;
import com.example.mission2_account.dto.UseBalance;
import com.example.mission2_account.exception.AccountException;
import com.example.mission2_account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    /**
     * 거래 - 계좌 금액 사용
     * */
    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(
            @RequestBody @Valid UseBalance.Request request
    ) throws InterruptedException {
        try {
            Thread.sleep(3000L);
            return UseBalance.Response.from(
                    transactionService.useBalance(
                            request.getUserId(),
                            request.getAccountNumber(),
                            request.getAmount())
            );
        } catch (AccountException e) {
            log.error("Failed to use balance");

            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e;
        }
    }

    /**
     * 거래 취소 - 계좌 금액 사용 취소
     * */
    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(
            @Valid @RequestBody CancelBalance.Request request
    ) {

        try {
            return CancelBalance.Response.from(
                    transactionService.cancelBalance(
                            request.getTransactionId(),
                            request.getAccountNumber(),
                            request.getAmount())
            );
        } catch (AccountException e) {
            log.error("Failed to use balance");

            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    /**
     * 잔액 사용 확인
     * */
    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(
            @PathVariable String transactionId
    ) {
        return QueryTransactionResponse.from(
                transactionService.queryTransaction(transactionId));
    }
}
