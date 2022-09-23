package com.example.mission2_account.domain;

import lombok.*;

import javax.persistence.Entity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class AccountUser extends BaseEntity{
    private String name;        //계좌 소유주 이름
}
