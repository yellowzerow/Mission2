package com.example.mission2_account.domain;

import com.example.mission2_account.exception.AccountException;
import com.example.mission2_account.type.AccountStatus;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;

import static com.example.mission2_account.type.ErrorCode.*;
import static com.example.mission2_account.type.ErrorCode.AMOUNT_EXCEED_BALANCE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Account extends BaseEntity{
    @ManyToOne
    private AccountUser accountUser;        //계좌 소유주
    private String accountNumber;           //계좌 번호

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;    //계좌 상태(계좌 가입, 해지 상태)
    private Long balance;                   //계좌에 들어있는 금액

    private LocalDateTime registeredAt;     //계좌 가입 날짜
    private LocalDateTime unRegisteredAt;   //계좌 해지 날짜

    //계좌 금액 사용(계좌 금액보다 사용량이 많으면 오류 출력)
    public void useBalance(Long amount) {
        if (amount > balance) {
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        }

        balance -= amount;
    }

    //계좌 금액 사용 취소(취소 금액이 음수이면 오류 출력)
    public void cancelBalance(Long amount) {
        if (amount < 0) {
            throw new AccountException(INVALID_REQUEST);
        }

        balance += amount;
    }
}
