package com.example.assetmanager.repository;

import com.example.assetmanager.domain.DividendHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DividendHistoryRepository extends JpaRepository<DividendHistory, Long> {
    List<DividendHistory> findByCode(String code);
}
