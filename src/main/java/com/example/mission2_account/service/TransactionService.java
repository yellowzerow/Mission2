package com.example.mission2_account.service;

import com.example.mission2_account.domain.Account;
import com.example.mission2_account.domain.AccountUser;
import com.example.mission2_account.domain.Transaction;
import com.example.mission2_account.dto.TransactionDto;
import com.example.mission2_account.exception.AccountException;
import com.example.mission2_account.repository.AccountRepository;
import com.example.mission2_account.repository.AccountUserRepository;
import com.example.mission2_account.repository.TransactionRepository;
import com.example.mission2_account.type.AccountStatus;
import com.example.mission2_account.type.TransactionResultType;
import com.example.mission2_account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.mission2_account.type.ErrorCode.*;
import static com.example.mission2_account.type.TransactionResultType.FAIL;
import static com.example.mission2_account.type.TransactionResultType.SUCCESS;
import static com.example.mission2_account.type.TransactionType.CANCEL;
import static com.example.mission2_account.type.TransactionType.USE;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    /**
     * 거래 - 계좌 금액 사용하기
     * 사용자 찾기, 사용자 아이디와 계좌 소유주 동일 검사
     * 계좌 상태 확인, 계좌 금액과 사용금액 비교
     * 거래 금액 확인 후 성공 시 거래 금액만큼 계좌 금액 차감
     * 거래 정보 저장
     */
    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
        AccountUser user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateUseBalance(user, account, amount);

        account.useBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(USE, SUCCESS, account, amount));
    }

    /**
     * 거래 시 유효성 검사
     * */
    private void validateUseBalance(AccountUser user, Account account, Long amount) {
        //사용자와 계좌 소유주 정보 불일치
        if (!Objects.equals(user.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }

        //해지된 계좌
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        //계좌 금액보다 사용 금액이 더 큰 경우
        if (account.getBalance() < amount) {
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        }
    }

    /**
     * 거래 요청을 했을 때 사용중인 계좌여서
     * 거래 요청이 취소 되었을때
     * 정보를 저장
     * */
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(USE, FAIL, account, amount);
    }

    /**
     * 거래 취소 - 계좌 금액 사용 취소
     * 거래 아이디에 해당 거래가 있는지 확인
     * 계좌 존재 여부 확인, 거래와 계좌 일치 여부 확인
     * 거래 금액과 거래 취소 금액이 같은지 확인
     * 1년이 지난 거래인지 확인 후 정보 저장
     * */
    public TransactionDto cancelBalance(String transactionId,
                                        String accountNumber, Long amount
    ) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateCancelBalance(transaction, account, amount);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(CANCEL, SUCCESS, account, amount));
    }

    /**
     * 거래 취소 시 유효성 검사
     * */
    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        //사용자와 계좌 소유주 정보 불일치
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())){
            throw new AccountException(TRANSACTION_ACCOUNT_UN_MATCH);
        }

        //거래 금액과 거래 취소 금액 불일치
        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(CANCEL_MUST_FULLY);
        }

        //1년이 지난 거래 취소
        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))){
            throw new AccountException(TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    /**
     * 거래 취소 요청 시 계좌가 사용중이여서
     * 요청 실패할 때 정보 저장
     */
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(CANCEL, FAIL, account, amount);
    }


    /**
     * 거래 상태(성공, 실패)에 따라
     * 계좌 금액을 차감하거나 증감 시키고
     * 정보 저장
     */
    private Transaction saveAndGetTransaction(
            TransactionType transactionType,
            TransactionResultType transactionResultType,
            Account account,
            Long amount) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * 잔액 사용 확인
     * 해당 거래 아이디의 거래가 있는 지 확인
     * 실패한 거래도 확인할 수 있음
     */
    public TransactionDto queryTransaction(String transactionId) {
        return TransactionDto.fromEntity(transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND)));
    }
}
