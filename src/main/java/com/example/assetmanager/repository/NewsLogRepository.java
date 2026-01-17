package com.example.assetmanager.repository;

import com.example.assetmanager.domain.NewsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NewsLogRepository extends JpaRepository<NewsLog, Long> {
    List<NewsLog> findByRelatedAssetOrderByFetchedAtDesc(String relatedAsset);

    Optional<NewsLog> findByLink(String link);
}
