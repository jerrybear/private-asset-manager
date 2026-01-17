package com.example.assetmanager.service;

import com.example.assetmanager.domain.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KisPriceProviderTest {

    private KisPriceProvider kisPriceProvider;

    @BeforeEach
    void setUp() {
        kisPriceProvider = new KisPriceProvider();
    }

    @Test
    @DisplayName("지원하는 자산 타입 확인")
    void supports_ReturnsTrueForSupportedTypes() {
        assertThat(kisPriceProvider.supports(AssetType.STOCK)).isTrue();
        assertThat(kisPriceProvider.supports(AssetType.STOCK_KR)).isTrue();
        assertThat(kisPriceProvider.supports(AssetType.STOCK_US)).isTrue();
        assertThat(kisPriceProvider.supports(AssetType.REITS)).isTrue();
        assertThat(kisPriceProvider.supports(AssetType.COMMODITY)).isTrue();
        assertThat(kisPriceProvider.supports(AssetType.BOND_KR)).isTrue();
        assertThat(kisPriceProvider.supports(AssetType.BOND_US)).isTrue();
        assertThat(kisPriceProvider.supports(AssetType.GOLD_SPOT)).isTrue();
        assertThat(kisPriceProvider.supports(AssetType.CRYPTO)).isFalse();
    }

    @Test
    @DisplayName("현재가 조회 테스트 (Mock 범위 내)")
    void getCurrentPrice_Success() {
        // given
        String code = "005930";

        // when
        BigDecimal price = kisPriceProvider.getCurrentPrice(code);

        // then
        assertThat(price).isBetween(new BigDecimal("50000"), new BigDecimal("100000"));
    }

    @Test
    @DisplayName("다중 종목 현재가 조회 테스트")
    void getMultiplePrices_Success() {
        // given
        List<String> codes = List.of("005930", "000660");

        // when
        Map<String, BigDecimal> prices = kisPriceProvider.getMultiplePrices(codes);

        // then
        assertThat(prices).hasSize(2);
        assertThat(prices.get("005930")).isBetween(new BigDecimal("50000"), new BigDecimal("100000"));
        assertThat(prices.get("000660")).isBetween(new BigDecimal("50000"), new BigDecimal("100000"));
    }

    @Test
    @DisplayName("예상 배당금 조회 테스트")
    void getExpectedDividend_Success() {
        // given
        String code = "005930";

        // when
        BigDecimal dividend = kisPriceProvider.getExpectedDividend(code);

        // then
        assertThat(dividend).isBetween(new BigDecimal("1000"), new BigDecimal("3000"));
    }
}
