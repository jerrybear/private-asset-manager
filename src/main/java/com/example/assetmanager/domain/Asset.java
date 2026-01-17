package com.example.assetmanager.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Account account;

    @Enumerated(EnumType.STRING)
    private AssetType type;

    private String code; // 종목코드 (예: 005930) or 코인 심볼 (예: BTC)
    private String name;

    private BigDecimal quantity;
    private BigDecimal averagePurchasePrice;
    private BigDecimal currentPrice;
    private LocalDate purchaseDate;
    private java.time.LocalDateTime lastPriceUpdate;

    private String dividendCycle; // 배당 주기 (1개월, 3개월, 6개월, 12개월, 없음)
    private BigDecimal dividendPerShare; // 1회당 배당금

    public Asset(Account account, AssetType type, String code, String name, BigDecimal quantity,
            BigDecimal averagePurchasePrice, BigDecimal currentPrice, java.time.LocalDateTime lastPriceUpdate,
            LocalDate purchaseDate) {
        this.account = account;
        this.type = type;
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.currentPrice = currentPrice;
        this.lastPriceUpdate = lastPriceUpdate;
        this.purchaseDate = purchaseDate;
    }

    public Asset(Account account, AssetType type, String code, String name, BigDecimal quantity,
            BigDecimal averagePurchasePrice, BigDecimal currentPrice, java.time.LocalDateTime lastPriceUpdate,
            LocalDate purchaseDate, String dividendCycle, BigDecimal dividendPerShare) {
        this.account = account;
        this.type = type;
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.currentPrice = currentPrice;
        this.lastPriceUpdate = lastPriceUpdate;
        this.purchaseDate = purchaseDate;
        this.dividendCycle = dividendCycle;
        this.dividendPerShare = dividendPerShare;
    }

    public Asset(Account account, AssetType type, String code, String name, BigDecimal quantity,
            BigDecimal averagePurchasePrice, LocalDate purchaseDate) {
        this.account = account;
        this.type = type;
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.purchaseDate = purchaseDate;
    }
}
