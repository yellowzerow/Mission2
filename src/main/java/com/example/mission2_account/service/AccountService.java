package com.example.mission2_account.service;

import com.example.mission2_account.domain.Account;
import com.example.mission2_account.domain.AccountUser;
import com.example.mission2_account.dto.AccountDto;
import com.example.mission2_account.exception.AccountException;
import com.example.mission2_account.repository.AccountNumberRepository;
import com.example.mission2_account.repository.AccountRepository;
import com.example.mission2_account.repository.AccountUserRepository;
import com.example.mission2_account.type.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static com.example.mission2_account.type.AccountStatus.IN_USE;
import static com.example.mission2_account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountNumberRepository accountNumberRepository;

    /**
     * 계좌 생성하기
     * 사용자가 있는지 확인
     * 계좌번호 랜덤 생성
     * 계좌 소유 개수 체크
     * 계좌 저장하고 정보 리턴
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = getAccountUser(userId);

        //유효성 검사
        validateCreateAccount(accountUser);

        //랜덤 10자리 계좌 생성하기
        String newAccountNumber = generateRandomAccountNumber();

        return AccountDto.fromEntity(
                accountRepository.save(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountStatus(IN_USE)
                                .accountNumber(newAccountNumber)
                                .balance(initialBalance)
                                .registeredAt(LocalDateTime.now())
                                .build()));
    }

    /**
     * 계좌번호 랜덤 10자리 생성
     * DB에 동일한 계좌번호가 있는지 체크 후
     * 존재하지 않으면 계좌 번호 리턴
     */
    private String generateRandomAccountNumber() {
        String accNum = "";

        while (true) {
            Random random = new Random();
            for (int i = 0; i < 10; i++) {
                accNum += random.nextInt(10);
            }

            boolean isExist = accountNumberRepository
                    .existsAccountNumberByAccountNumber(accNum);
            if (!isExist) {
                break;
            }
        }

        return accNum;
    }

    /**
     * 계좌 생성 시 유효성 검사
     */
    private void validateCreateAccount(AccountUser accountUser) {
        //계좌 10개 이상으로 생성 시 오류 메시지 출력
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }

    /**
     * 계좌 해지하기
     * 계좌 사용자가 있는지 확인
     * 해지할 계좌 번호가 있는지 확인
     * 사용자 아이디와 계좌 소유주가 같은지 확인
     * 계좌 상태 확인
     * 잔액이 있는지 확인 후
     * 계좌를 해지하고 정보 저장
     */
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    /**
     * 계좌 해지 시 유효성 검사
     */
    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        //사용자 아이디와 계좌 소유주가 다르면 오류 출력
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }

        //계좌 상태가 이미 해지 상태면 오류 출력
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        //계좌에 잔액이 남아있다면 오류 출력
        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 계좌 소유주 정보 가져오기
     * */
    private AccountUser getAccountUser(Long userId) {
        return accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
    }
}
