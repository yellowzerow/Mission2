package com.example.mission2_account.domain;

import com.example.mission2_account.type.TransactionResultType;
import com.example.mission2_account.type.TransactionType;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Transaction extends BaseEntity{

    //거래 상태(거래 사용, 취소 상태)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    //거래 결과(성공, 실패)
    @Enumerated(EnumType.STRING)
    private TransactionResultType transactionResultType;

    @ManyToOne
    private Account account;                //거래 계좌
    private Long amount;                    //거래 금액
    private Long balanceSnapshot;           //잔액 조회금

    private String transactionId;           //거래 조회시 사용할 id
    private LocalDateTime transactedAt;     //거래 일시
}
