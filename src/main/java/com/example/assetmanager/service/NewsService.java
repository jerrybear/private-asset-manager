package com.example.assetmanager.service;

import com.example.assetmanager.domain.NewsFetchHistory;
import com.example.assetmanager.domain.NewsLog;
import com.example.assetmanager.dto.NewsItem;
import com.example.assetmanager.repository.NewsFetchHistoryRepository;
import com.example.assetmanager.repository.NewsLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final WebClient.Builder webClientBuilder;
    private final XmlMapper xmlMapper = new XmlMapper();
    private final NewsLogRepository newsLogRepository;
    private final NewsFetchHistoryRepository newsFetchHistoryRepository;

    private static final DateTimeFormatter RFC_1123_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final int CACHE_MINUTES = 10;

    @Transactional
    public List<NewsItem> searchNewsForAssets(List<String> assets, boolean force) {
        if (assets == null || assets.isEmpty()) {
            return List.of();
        }

        List<NewsItem> allNews = new ArrayList<>();
        List<String> distinctAssets = assets.stream().distinct().limit(10).toList();

        for (String asset : distinctAssets) {
            if (force || shouldFetchFromApi(asset)) {
                List<NewsItem> fetched = searchNews(asset, asset).stream().limit(5).toList();
                saveNewsToDb(asset, fetched);
                updateFetchHistory(asset);
                allNews.addAll(fetched);
            } else {
                List<NewsItem> cached = getNewsFromDb(asset);
                allNews.addAll(cached);
            }
        }

        // 1. 중복 뉴스 필터링 (제목 기준) 및 정렬
        Set<String> seenTitles = new HashSet<>();
        return allNews.stream()
                .filter(item -> {
                    // "제목 - 언론사" 형태에서 제목만 추출하여 중복 체크
                    String coreTitle = item.getTitle().split(" - ")[0].replaceAll("\\s", "");
                    return seenTitles.add(coreTitle);
                })
                .sorted((a, b) -> {
                    try {
                        ZonedDateTime dateA = parseDateTime(a.getPubDate());
                        ZonedDateTime dateB = parseDateTime(b.getPubDate());
                        return dateB.compareTo(dateA); // 최신순
                    } catch (Exception e) {
                        return b.getPubDate().compareTo(a.getPubDate());
                    }
                })
                .limit(30)
                .toList();
    }

    private ZonedDateTime parseDateTime(String pubDate) {
        try {
            return ZonedDateTime.parse(pubDate, RFC_1123_FORMATTER);
        } catch (Exception e) {
            // ISO 8601 등 다른 형식 대응이 필요할 경우 추가
            return ZonedDateTime.now();
        }
    }

    private boolean shouldFetchFromApi(String assetName) {
        return newsFetchHistoryRepository.findByAssetName(assetName)
                .map(history -> history.getLastFetchedAt().plusMinutes(CACHE_MINUTES).isBefore(LocalDateTime.now()))
                .orElse(true);
    }

    private void updateFetchHistory(String assetName) {
        NewsFetchHistory history = newsFetchHistoryRepository.findByAssetName(assetName)
                .orElse(NewsFetchHistory.builder().assetName(assetName).build());
        history.setLastFetchedAt(LocalDateTime.now());
        newsFetchHistoryRepository.save(history);
    }

    private void saveNewsToDb(String asset, List<NewsItem> newsItems) {
        for (NewsItem item : newsItems) {
            if (newsLogRepository.findByLink(item.getLink()).isEmpty()) {
                NewsLog logEntry = NewsLog.builder()
                        .title(item.getTitle())
                        .link(item.getLink())
                        .pubDate(item.getPubDate())
                        .source(item.getSource())
                        .relatedAsset(asset)
                        .build();
                newsLogRepository.save(logEntry);
            }
        }
    }

    private List<NewsItem> getNewsFromDb(String asset) {
        return newsLogRepository.findByRelatedAssetOrderByFetchedAtDesc(asset).stream()
                .limit(10)
                .map(log -> NewsItem.builder()
                        .title(log.getTitle())
                        .link(log.getLink())
                        .pubDate(log.getPubDate())
                        .source(log.getSource())
                        .relatedAsset(log.getRelatedAsset())
                        .build())
                .collect(Collectors.toList());
    }

    public List<NewsItem> searchNews(String query) {
        return searchNews(query, null);
    }

    private List<NewsItem> searchNews(String query, String relatedAsset) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String url = UriComponentsBuilder.fromHttpUrl("https://news.google.com/rss/search")
                .queryParam("q", query + " when:7d") // 최근 7일 뉴스
                .queryParam("hl", "ko")
                .queryParam("gl", "KR")
                .queryParam("ceid", "KR:ko")
                .build()
                .toUriString();

        try {
            String xml = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xml == null)
                return List.of();

            JsonNode root = xmlMapper.readTree(xml.getBytes());
            JsonNode items = root.path("channel").path("item");

            List<NewsItem> newsItems = new ArrayList<>();
            if (items.isArray()) {
                for (JsonNode item : items) {
                    newsItems.add(parseItem(item, relatedAsset));
                }
            } else if (!items.isMissingNode()) {
                newsItems.add(parseItem(items, relatedAsset));
            }

            return newsItems;
        } catch (Exception e) {
            log.error("Error searching news for query: {}", query, e);
            return List.of();
        }
    }

    private NewsItem parseItem(JsonNode item, String relatedAsset) {
        return NewsItem.builder()
                .title(item.path("title").asText())
                .link(item.path("link").asText())
                .pubDate(item.path("pubDate").asText())
                .source(item.path("source").path("").asText()) // source는 객체일 수 있음
                .relatedAsset(relatedAsset)
                .build();
    }
}
