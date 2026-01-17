package com.example.assetmanager.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "news_log")
public class NewsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(unique = true, length = 1000)
    private String link;

    private String pubDate;

    private String source;

    private String relatedAsset;

    private LocalDateTime fetchedAt;

    @PrePersist
    public void prePersist() {
        this.fetchedAt = LocalDateTime.now();
    }
}
