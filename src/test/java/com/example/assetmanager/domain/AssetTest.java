package com.example.assetmanager.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AssetTest {

    @Test
    @DisplayName("자산 객체 생성 및 필드 확인")
    void createAsset_Success() {
        // given
        LocalDate now = LocalDate.now();
        Asset asset = Asset.builder()
                .name("삼성전자")
                .code("005930")
                .type(AssetType.STOCK)
                .quantity(new BigDecimal("10"))
                .averagePurchasePrice(new BigDecimal("70000"))
                .purchaseDate(now)
                .build();

        // then
        assertThat(asset.getName()).isEqualTo("삼성전자");
        assertThat(asset.getCode()).isEqualTo("005930");
        assertThat(asset.getType()).isEqualTo(AssetType.STOCK);
        assertThat(asset.getQuantity()).isEqualTo(new BigDecimal("10"));
        assertThat(asset.getAveragePurchasePrice()).isEqualTo(new BigDecimal("70000"));
        assertThat(asset.getPurchaseDate()).isEqualTo(now);
    }
}
