package com.example.assetmanager.controller;

import com.example.assetmanager.dto.NewsItem;
import com.example.assetmanager.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/search")
    public List<NewsItem> searchNews(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(defaultValue = "false") boolean force) {
        if (keywords != null && !keywords.isEmpty()) {
            return newsService.searchNewsForAssets(keywords, force);
        }
        return newsService.searchNews(query);
    }
}
