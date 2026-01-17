package com.example.assetmanager.repository;

import com.example.assetmanager.domain.NewsFetchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NewsFetchHistoryRepository extends JpaRepository<NewsFetchHistory, Long> {
    Optional<NewsFetchHistory> findByAssetName(String assetName);
}
