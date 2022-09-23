package com.example.mission2_account.domain;

import lombok.*;

import javax.persistence.Entity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class AccountNumber extends BaseEntity{
    //전체 사용자들의 계좌를 검사해서 중복이 있는지 확인할 테이블
    private String accountNumber;
}
