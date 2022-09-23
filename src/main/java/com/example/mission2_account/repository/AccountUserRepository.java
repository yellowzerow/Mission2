package com.example.mission2_account.repository;

import com.example.mission2_account.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountUserRepository
        extends JpaRepository<AccountUser, Long> {

}
