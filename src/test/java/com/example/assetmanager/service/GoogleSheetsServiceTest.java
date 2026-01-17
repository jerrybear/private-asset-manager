package com.example.assetmanager.service;

import com.example.assetmanager.domain.Account;
import com.example.assetmanager.domain.AccountType;
import com.example.assetmanager.domain.AssetType;
import com.google.api.services.sheets.v4.Sheets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GoogleSheetsServiceTest {

    @InjectMocks
    private GoogleSheetsService googleSheetsService;

    @Mock
    private Sheets sheets;

    @BeforeEach
    void setUp() {
        // @InjectMocks에 의해 sheets가 주입된 googleSheetsService가 생성됨
    }

    @Test
    @DisplayName("계좌 종류 파싱 테스트 - 다양한 한글/영문/설명 매칭")
    void parseAccountTypeTest() {
        // Given & When & Then
        assertThat(invokeParseAccountType("일반주식")).isEqualTo(AccountType.REGULAR);
        assertThat(invokeParseAccountType("REGULAR")).isEqualTo(AccountType.REGULAR);
        assertThat(invokeParseAccountType("ISA 계좌")).isEqualTo(AccountType.ISA);
        assertThat(invokeParseAccountType("연금저축")).isEqualTo(AccountType.PENSION);
        assertThat(invokeParseAccountType("IRP")).isEqualTo(AccountType.IRP);
        assertThat(invokeParseAccountType("특수계좌")).isEqualTo(AccountType.SPECIAL);
        assertThat(invokeParseAccountType("")).isEqualTo(AccountType.REGULAR);
        assertThat(invokeParseAccountType(null)).isEqualTo(AccountType.REGULAR);
    }

    @Test
    @DisplayName("자산 유형 파싱 테스트 - 다양한 한글/영문/설명 매칭")
    void parseAssetTypeTest() {
        // Given & When & Then
        assertThat(invokeParseAssetType("국내주식")).isEqualTo(AssetType.STOCK_KR);
        assertThat(invokeParseAssetType("STOCK_US")).isEqualTo(AssetType.STOCK_US);
        assertThat(invokeParseAssetType("미국주식")).isEqualTo(AssetType.STOCK_US);
        assertThat(invokeParseAssetType("가상화폐")).isEqualTo(AssetType.CRYPTO);
        assertThat(invokeParseAssetType("코인")).isEqualTo(AssetType.CRYPTO);
        assertThat(invokeParseAssetType("현금")).isEqualTo(AssetType.CASH);
        assertThat(invokeParseAssetType("REITS")).isEqualTo(AssetType.REITS);
        assertThat(invokeParseAssetType("리츠")).isEqualTo(AssetType.REITS);
        assertThat(invokeParseAssetType("채권")).isEqualTo(AssetType.BOND);
        assertThat(invokeParseAssetType("국내채권")).isEqualTo(AssetType.BOND_KR);
        assertThat(invokeParseAssetType("금현물")).isEqualTo(AssetType.GOLD_SPOT);
        assertThat(invokeParseAssetType("예적금")).isEqualTo(AssetType.DEPOSIT_SAVINGS);
        assertThat(invokeParseAssetType("UNKNOWN")).isEqualTo(AssetType.STOCK); // Default
    }

    // private 메서드 테스트를 위한 헬퍼 (Reflection 사용)
    private AccountType invokeParseAccountType(String value) {
        return (AccountType) ReflectionTestUtils.invokeMethod(googleSheetsService, "parseAccountType", value);
    }

    private AssetType invokeParseAssetType(String value) {
        return (AssetType) ReflectionTestUtils.invokeMethod(googleSheetsService, "parseAssetType", value);
    }
}
