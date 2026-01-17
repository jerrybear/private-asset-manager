package com.example.assetmanager.service;

import com.example.assetmanager.domain.AssetType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Order(2)
public class KisPriceProvider implements PriceProvider {

    private final Random random = new Random();

    @Override
    public boolean supports(AssetType type) {
        return type == AssetType.STOCK ||
                type == AssetType.STOCK_KR ||
                type == AssetType.STOCK_US ||
                type == AssetType.REITS ||
                type == AssetType.COMMODITY ||
                type == AssetType.BOND_KR ||
                type == AssetType.BOND_US ||
                type == AssetType.GOLD_SPOT;
    }

    @Override
    public BigDecimal getCurrentPrice(String code) {
        // 실제로는 KIS API 호출 (OAuth 토큰 필요)
        // PoC를 위해 50,000 ~ 100,000 사이의 랜덤 가격 반환
        return BigDecimal.valueOf(50000 + (random.nextDouble() * 50000)).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, BigDecimal> getMultiplePrices(Iterable<String> codes) {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (String code : codes) {
            prices.put(code, getCurrentPrice(code));
        }
        return prices;
    }

    // KIS API 특화: 배당 정보 조회 (상세 구현은 추후 확장)
    public BigDecimal getExpectedDividend(String code) {
        // PoC용: 주당 1,000 ~ 3,000원 사이 랜덤 배당금
        return BigDecimal.valueOf(1000 + (random.nextDouble() * 2000)).setScale(0, RoundingMode.HALF_UP);
    }
}
