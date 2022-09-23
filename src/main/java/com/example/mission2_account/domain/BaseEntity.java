package com.example.mission2_account.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {
    @Id
    @GeneratedValue
    private Long id;        //계좌, 거래 등의 고유 id

    @CreatedDate
    private LocalDateTime createdAt;    //계좌, 거래 생성 일시
    @LastModifiedDate
    private LocalDateTime updatedAt;    //계좌, 거래 정보 수정 일시
}
