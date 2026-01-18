package com.example.assetmanager.repository;

import com.example.assetmanager.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsBySheetName(String sheetName);

    List<Account> findByMemberId(Long memberId);
}
