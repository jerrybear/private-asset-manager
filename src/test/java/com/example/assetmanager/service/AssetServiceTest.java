package com.example.assetmanager.service;

import com.example.assetmanager.domain.Account;
import com.example.assetmanager.domain.AccountType;
import com.example.assetmanager.domain.Asset;
import com.example.assetmanager.domain.AssetType;
import com.example.assetmanager.repository.AccountRepository;
import com.example.assetmanager.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

        @Mock
        private AccountRepository accountRepository;

        @Mock
        private AssetRepository assetRepository;

        @Mock
        private GoogleSheetsService googleSheetsService;

        @Mock
        private PublicDataPriceProvider publicDataPriceProvider;

        @Mock
        private PriceProvider priceProvider;

        private AssetService assetService;

        @BeforeEach
        void setUp() {
                // List<PriceProvider> 주입을 위해 수동 생성
                assetService = new AssetService(accountRepository, assetRepository,
                                List.of(publicDataPriceProvider, priceProvider),
                                googleSheetsService);
        }

        @Test
        @DisplayName("계좌 생성 성공 테스트")
        void createAccount_Success() {
                // given
                String name = "Test Account";
                String description = "Test Description";
                String sheetName = "Sheet1";
                given(accountRepository.existsBySheetName(anyString())).willReturn(false);
                given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                Account account = assetService.createAccount(name, description, sheetName, "Owner", AccountType.REGULAR,
                                "Bank",
                                "123-456");

                // then
                assertThat(account.getName()).isEqualTo(name);
                assertThat(account.getSheetName()).isEqualTo(sheetName);
                verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("계좌 요약 정보 조회 테스트")
        void getAccountSummary_Success() {
                // given
                Long accountId = 1L;
                Account account = new Account("Test Account", "Desc", "Sheet1", "Owner", AccountType.REGULAR, "Bank",
                                "123-456");
                account.setId(accountId);
                Asset asset = Asset.builder()
                                .type(AssetType.STOCK)
                                .code("KRX:005930")
                                .name("삼성전자")
                                .quantity(new BigDecimal("10"))
                                .averagePurchasePrice(new BigDecimal("70000"))
                                .currentPrice(new BigDecimal("75000"))
                                .build();

                given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
                given(assetRepository.findByAccountId(accountId)).willReturn(List.of(asset));
                // when
                Map<String, Object> summary = assetService.getAccountSummary(accountId);

                // then
                assertThat(summary.get("accountName")).isEqualTo("Test Account");
                assertThat(summary.get("totalPurchaseAmount")).isEqualTo(new BigDecimal("700000"));
                assertThat(summary.get("totalCurrentValue")).isEqualTo(new BigDecimal("750000"));
                assertThat(summary.get("totalProfitLoss")).isEqualTo(new BigDecimal("50000"));

                List<Map<String, Object>> assets = (List<Map<String, Object>>) summary.get("assets");
                assertThat(assets).hasSize(1);
                assertThat(assets.get(0).get("name")).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("구글 시트와 동기화 테스트")
        void syncWithGoogleSheets_Success() throws Exception {
                // given
                Long accountId = 1L;
                Account account = new Account("Test Account", "Desc", "Sheet1", "Owner", AccountType.REGULAR, "Bank",
                                "123-456");

                Account accountFromSheet = new Account("Updated Name", "Updated Desc", "Sheet1", "New Owner",
                                AccountType.ISA, "New Bank", "999-999");
                Asset assetFromSheet = Asset.builder()
                                .type(AssetType.STOCK)
                                .code("005930")
                                .name("삼성전자")
                                .quantity(new BigDecimal("10"))
                                .averagePurchasePrice(new BigDecimal("70000"))
                                .build();
                accountFromSheet.setAssets(new java.util.ArrayList<>(List.of(assetFromSheet)));

                given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
                given(googleSheetsService.fetchAccountFromSheet("Sheet1")).willReturn(accountFromSheet);

                // when
                assetService.syncWithGoogleSheets(accountId);

                // then
                assertThat(account.getName()).isEqualTo("Updated Name");
                assertThat(account.getOwner()).isEqualTo("New Owner");
                assertThat(account.getAccountType()).isEqualTo(AccountType.ISA);
                verify(accountRepository, times(2)).save(any(Account.class));
        }

        @Test
        @DisplayName("계좌 생성 실패 테스트 - 중복된 시트 이름")
        void createAccount_DuplicateSheetName_ThrowsException() {
                // given
                given(accountRepository.existsBySheetName("DuplicateSheet")).willReturn(true);

                // when & then
                org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
                        assetService.createAccount("Name", "Desc", "DuplicateSheet", "Owner", AccountType.REGULAR,
                                        "Bank",
                                        "123-456");
                });
        }

        @Test
        @DisplayName("현금(CASH) 자산 수익률 계산 테스트")
        void getAccountSummary_CashAsset_Success() {
                // given
                Long accountId = 1L;
                Account account = new Account("Cash Account", "Desc", "Sheet1", "Owner", AccountType.REGULAR, "Bank",
                                "123-456");
                account.setId(accountId);
                Asset cash = Asset.builder()
                                .type(AssetType.CASH)
                                .name("현금")
                                .quantity(new BigDecimal("100000"))
                                .averagePurchasePrice(BigDecimal.ONE)
                                .build();

                given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
                given(assetRepository.findByAccountId(accountId)).willReturn(List.of(cash));
                // CASH는 PriceProvider를 거치지 않고 내부적으로 1.0으로 처리됨

                // when
                Map<String, Object> summary = assetService.getAccountSummary(accountId);

                // then
                assertThat(summary.get("totalPurchaseAmount")).isEqualTo(new BigDecimal("100000"));
                assertThat(summary.get("totalCurrentValue")).isEqualTo(new BigDecimal("100000"));
                assertThat(summary.get("totalProfitLoss")).isEqualTo(BigDecimal.ZERO);
                assertThat(summary.get("totalReturnRate")).isEqualTo(BigDecimal.ZERO.setScale(4));
        }

        @Test
        @DisplayName("KRX 접두사 자산 가격 갱신 테스트")
        void refreshAssetPrice_KrxAsset_Success() {
                // given
                Long accountId = 1L;
                Long assetId = 10L;
                Asset krxAsset = Asset.builder()
                                .id(assetId)
                                .code("KRX:005930")
                                .averagePurchasePrice(new BigDecimal("70000"))
                                .account(Account.builder().id(accountId).build())
                                .build();

                given(assetRepository.findById(assetId)).willReturn(Optional.of(krxAsset));
                given(publicDataPriceProvider.getCurrentPrice("KRX:005930")).willReturn(new BigDecimal("72000"));

                // when
                BigDecimal newPrice = assetService.refreshAssetPrice(accountId, assetId, false);

                // then
                assertThat(newPrice).isEqualTo(new BigDecimal("72000"));
                assertThat(krxAsset.getCurrentPrice()).isEqualTo(new BigDecimal("72000"));
                verify(assetRepository).save(krxAsset);
        }

        @Test
        @DisplayName("현금(CASH) 자산 가격 갱신 테스트 - 평균단가로 고정")
        void refreshAssetPrice_CashAsset_FixedToAveragePrice() {
                // given
                Long accountId = 1L;
                Long assetId = 11L;
                Asset cashAsset = Asset.builder()
                                .id(assetId)
                                .type(AssetType.CASH)
                                .averagePurchasePrice(new BigDecimal("100000"))
                                .account(Account.builder().id(accountId).build())
                                .build();

                given(assetRepository.findById(assetId)).willReturn(Optional.of(cashAsset));

                // when
                BigDecimal newPrice = assetService.refreshAssetPrice(accountId, assetId, false);

                // then
                assertThat(newPrice).isEqualTo(new BigDecimal("100000"));
                assertThat(cashAsset.getCurrentPrice()).isEqualTo(new BigDecimal("100000"));
                verify(assetRepository).save(cashAsset);
                verify(publicDataPriceProvider, never()).getCurrentPrice(anyString());
        }

        @Test
        @DisplayName("전체 자산 가격 일괄 갱신 테스트")
        void refreshAllPrices_Success() {
                // given
                Long accountId = 1L;
                Asset krxAsset = Asset.builder().code("KRX:005930").build();
                Asset otherAsset = Asset.builder().code("000660").averagePurchasePrice(new BigDecimal("100000"))
                                .build();
                List<Asset> assets = List.of(krxAsset, otherAsset);

                given(assetRepository.findByAccountId(accountId)).willReturn(assets);
                given(publicDataPriceProvider.getMultiplePrices(anyList()))
                                .willReturn(Map.of("KRX:005930", new BigDecimal("75000")));

                // when
                assetService.refreshAllPrices(accountId, false);

                // then
                assertThat(krxAsset.getCurrentPrice()).isEqualTo(new BigDecimal("75000"));
                assertThat(otherAsset.getCurrentPrice()).isEqualTo(new BigDecimal("100000"));
                verify(assetRepository).saveAll(assets);
        }

        @Test
        @DisplayName("배당금 계산 테스트 (1개월, 3개월, 6개월, 12개월)")
        void calculateAccountSummary_Dividend_Success() {
                // given
                Long accountId = 1L;
                Account account = new Account("Div Account", "Desc", "Sheet1", "Owner", AccountType.REGULAR, "Bank",
                                "123-456");
                account.setId(accountId);

                Asset assetMo = Asset.builder()
                                .type(AssetType.STOCK)
                                .quantity(new BigDecimal("10"))
                                .averagePurchasePrice(new BigDecimal("1000"))
                                .dividendCycle("1개월")
                                .dividendPerShare(new BigDecimal("10"))
                                .build(); // 10 * 12 * 10 = 1200

                Asset assetQt = Asset.builder()
                                .type(AssetType.STOCK)
                                .quantity(new BigDecimal("10"))
                                .averagePurchasePrice(new BigDecimal("1000"))
                                .dividendCycle("3개월")
                                .dividendPerShare(new BigDecimal("100"))
                                .build(); // 100 * 4 * 10 = 4000

                Asset assetSa = Asset.builder()
                                .type(AssetType.STOCK)
                                .quantity(new BigDecimal("10"))
                                .averagePurchasePrice(new BigDecimal("1000"))
                                .dividendCycle("6개월")
                                .dividendPerShare(new BigDecimal("500"))
                                .build(); // 500 * 2 * 10 = 10000

                Asset assetAn = Asset.builder()
                                .type(AssetType.STOCK)
                                .quantity(new BigDecimal("10"))
                                .averagePurchasePrice(new BigDecimal("1000"))
                                .dividendCycle("12개월")
                                .dividendPerShare(new BigDecimal("1000"))
                                .build(); // 1000 * 1 * 10 = 10000

                Asset assetNone = Asset.builder()
                                .type(AssetType.STOCK)
                                .quantity(new BigDecimal("10"))
                                .averagePurchasePrice(new BigDecimal("1000"))
                                .dividendCycle("없음")
                                .dividendPerShare(new BigDecimal("1000"))
                                .build(); // 0

                given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
                given(assetRepository.findByAccountId(accountId))
                                .willReturn(List.of(assetMo, assetQt, assetSa, assetAn, assetNone));

                // when
                Map<String, Object> summary = assetService.getAccountSummary(accountId);

                // then
                // 1200 + 4000 + 10000 + 10000 + 0 = 25200
                assertThat(summary.get("totalExpectedDividend")).isEqualTo(new BigDecimal("25200"));
        }
}
