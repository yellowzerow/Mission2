package com.example.mission2_account.service;

import com.example.mission2_account.domain.Account;
import com.example.mission2_account.domain.AccountUser;
import com.example.mission2_account.dto.AccountDto;
import com.example.mission2_account.exception.AccountException;
import com.example.mission2_account.repository.AccountNumberRepository;
import com.example.mission2_account.repository.AccountRepository;
import com.example.mission2_account.repository.AccountUserRepository;
import com.example.mission2_account.type.AccountStatus;
import com.example.mission2_account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private AccountNumberRepository accountNumberRepository;

    @InjectMocks
    private AccountService accountService;


    @Test
    @DisplayName("계좌 생성 성공")
    void createAccountExist() {
        //given
        AccountUser yez = AccountUser.builder()
                            .name("yez").build();
        yez.setId(92L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountNumberRepository.existsAccountNumberByAccountNumber(anyString()))
                .willReturn(true)   //중복이 생겨도 다시 재생성하기 때문에 성공한다
                .willReturn(false); //중복 아님

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(yez)
                        .accountNumber("0987654321")
                        .build());

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountNumberRepository, times(2))
                .existsAccountNumberByAccountNumber(any());
        assertEquals(92L, accountDto.getUserId());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("10개 이상의 계좌 - 계좌 생성 실패")
    void createAccount_maxAccountIs10() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 성공")
    void deleteAccountSuccess() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(yez)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(92L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccount_AccountNotFound() {
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
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 계좌 해지 실패")
    void deleteAccountFailed_userUnMatch() {
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
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌에 잔액이 있음 - 계좌 해지 실패")
    void deleteAccountFailed_balanceNotEmpty() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(yez)
                        .balance(100L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 된 계좌 - 계좌 해지 실패")
    void deleteAccountFailed_alreadyUnregistered() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(yez)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("사용자 계좌 찾기 성공")
    void successGetAccountsByUserId() {
        //given
        AccountUser yez = AccountUser.builder()
                .name("yez").build();
        yez.setId(92L);
        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountUser(yez)
                        .accountNumber("1111111111")
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .accountNumber("2222222222")
                        .accountUser(yez)
                        .balance(2000L)
                        .build(),
                Account.builder()
                        .accountNumber("3333333333")
                        .accountUser(yez)
                        .balance(3000L)
                        .build()
        );

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(yez));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        //when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

        //then
        assertEquals(3, accountDtos.size());
        assertEquals("1111111111", accountDtos.get(0).getAccountNumber());
        assertEquals(1000, accountDtos.get(0).getBalance());
        assertEquals("2222222222", accountDtos.get(1).getAccountNumber());
        assertEquals(2000, accountDtos.get(1).getBalance());
        assertEquals("3333333333", accountDtos.get(2).getAccountNumber());
        assertEquals(3000, accountDtos.get(2).getBalance());
    }

    @Test
    @DisplayName("사용자 없음 - 계좌 찾기 실패")
    void failedToGetAccounts() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
