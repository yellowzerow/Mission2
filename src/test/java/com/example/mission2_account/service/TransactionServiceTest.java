package com.example.mission2_account.service;

import com.example.mission2_account.domain.Account;
import com.example.mission2_account.domain.AccountUser;
import com.example.mission2_account.domain.Transaction;
import com.example.mission2_account.dto.TransactionDto;
import com.example.mission2_account.exception.AccountException;
import com.example.mission2_account.repository.AccountRepository;
import com.example.mission2_account.repository.AccountUserRepository;
import com.example.mission2_account.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.mission2_account.type.AccountStatus.*;
import static com.example.mission2_account.type.ErrorCode.*;
import static com.example.mission2_account.type.TransactionResultType.FAIL;
import static com.example.mission2_account.type.TransactionResultType.SUCCESS;
import static com.example.mission2_account.type.TransactionType.CANCEL;
import static com.example.mission2_account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("거래 성공")
    void successUseBalance() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        Account account = Account.builder()
                .accountUser(yez)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(SUCCESS)
                        .transactionId("transactionId")
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService
                .useBalance(1L, "1000000000", 2000L);

        //then
        verify(transactionRepository, times(1))
                .save(captor.capture());
        assertEquals(2000L, captor.getValue().getAmount());
        assertEquals(8000L, captor.getValue().getBalanceSnapshot());
        assertEquals(SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 유저 없음 - 거래 실패")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "1234567890", 1000L));

        //then
        assertEquals(USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 거래 실패")
    void useBalance_AccountNotFound() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "1234567890", 1000L));

        //then
        assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 거래 실패")
    void useBalance_userUnMatch() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        AccountUser micky = AccountUser.builder()
                .name("micky").build();
        micky.setId(13L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(micky)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "1234567890", 1000L));

        //then
        assertEquals(USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌 - 거래 실패")
    void useBalance_alreadyUnregistered() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(yez)
                        .accountStatus(UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "1234567890", 1000L));

        //then
        assertEquals(ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우 - 거래 실패")
    void useBalance_ExceedAmount() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        Account account = Account.builder()
                .accountUser(yez)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1000000012").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        //then
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "1234567890", 1000L));

        assertEquals(AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("거래 실패 정보 저장")
    void saveFailedUseTransaction() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        Account account = Account.builder()
                .accountUser(yez)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(SUCCESS)
                        .transactionId("transactionId")
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransaction(
                "1000000000", 200L);

        //then
        verify(transactionRepository, times(1))
                .save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(FAIL, captor.getValue().getTransactionResultType());
    }

    @Test
    @DisplayName("거래 취소 성공")
    void successCancelBalance() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        Account account = Account.builder()
                .accountUser(yez)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(2000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResultType(SUCCESS)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(2000L)
                        .balanceSnapshot(10000L)
                        .build());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId", "1000000000", 2000L);

        //then
        verify(transactionRepository, times(1))
                .save(captor.capture());
        assertEquals(2000L, captor.getValue().getAmount());
        assertEquals(12000L, captor.getValue().getBalanceSnapshot());
        assertEquals(SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(2000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 거래 취소 실패")
    void cancelTransaction_AccountNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 1000L));

        //then
        assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("원 사용 거래 없음 - 거래 취소 실패")
    void cancelTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 1000L));

        //then
        assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌 매칭 실패 - 거래 취소 실패")
    void cancelTransaction_TransactionAccountUnMatch() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        Account account = Account.builder()
                .accountUser(yez)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Account accountNotUse = Account.builder()
                .accountUser(yez)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000013").build();
        accountNotUse.setId(2L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(2000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 2000L));

        //then
        assertEquals(TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래금액과 취소 금액이 다름 - 거래 취소 실패")
    void cancelTransaction_TransactionCancelMustFully() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        Account account = Account.builder()
                .accountUser(yez)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(3000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 2000L));

        //then
        assertEquals(CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("취소는 1년 이내에 가능 - 잔액 사용 취소 실패")
    void cancelTransaction_TooOldOrder() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        Account account = Account.builder()
                .accountUser(yez)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusYears(1))
                .amount(2000L)
                .balanceSnapshot(8000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 2000L));

        //then
        assertEquals(TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 조회 성공")
    void successQueryTransaction() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        Account account = Account.builder()
                .accountUser(yez)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusYears(1))
                .amount(2000L)
                .balanceSnapshot(8000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        TransactionDto transactionDto =
                transactionService.queryTransaction("trxId");

        //then
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(2000L, transactionDto.getAmount());
        assertEquals("transactionId", transaction.getTransactionId());
    }

    @Test
    @DisplayName("원 거래 없음 - 거래 조회 실패")
    void queryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        //then
        assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}