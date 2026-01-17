package com.example.assetmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsItem {
    private String title;
    private String link;
    private String pubDate;
    private String source;
    private String relatedAsset;
}
