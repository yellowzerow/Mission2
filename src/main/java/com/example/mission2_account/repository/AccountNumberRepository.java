package com.example.mission2_account.repository;

import com.example.mission2_account.domain.AccountNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountNumberRepository extends JpaRepository<AccountNumber, Long> {
    boolean existsAccountNumberByAccountNumber(String accountNumber);
}
