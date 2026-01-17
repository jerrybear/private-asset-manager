package com.example.assetmanager.service;

import com.example.assetmanager.domain.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class UpbitPriceProviderTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private UpbitPriceProvider upbitPriceProvider;

    @BeforeEach
    void setUp() {
        upbitPriceProvider = new UpbitPriceProvider(webClient);
    }

    @Test
    @DisplayName("업비트 현재가 조회 테스트")
    void getCurrentPrice_Success() {
        // given
        String code = "KRW-BTC";
        Map<String, Object> mockTicker = Map.of("market", "KRW-BTC", "trade_price", 50000000.0);
        List<Map<String, Object>> mockResponse = List.of(mockTicker);

        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(java.net.URI.class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        given(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).willReturn(Mono.just(mockResponse));

        // when
        BigDecimal price = upbitPriceProvider.getCurrentPrice(code);

        // then
        assertThat(price).isEqualByComparingTo("50000000");
    }

    @Test
    @DisplayName("지원하는 자산 타입 확인")
    void supports_ReturnsTrueForCrypto() {
        assertThat(upbitPriceProvider.supports(AssetType.CRYPTO)).isTrue();
        assertThat(upbitPriceProvider.supports(AssetType.STOCK)).isFalse();
    }

    @Test
    @DisplayName("다중 종목 현재가 조회 테스트")
    void getMultiplePrices_Success() {
        // given
        List<String> codes = List.of("KRW-BTC", "KRW-ETH");
        Map<String, Object> btcTicker = Map.of("market", "KRW-BTC", "trade_price", 50000000.0);
        Map<String, Object> ethTicker = Map.of("market", "KRW-ETH", "trade_price", 3000000.0);
        List<Map<String, Object>> mockResponse = List.of(btcTicker, ethTicker);

        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(java.net.URI.class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        given(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).willReturn(Mono.just(mockResponse));

        // when
        Map<String, BigDecimal> prices = upbitPriceProvider.getMultiplePrices(codes);

        // then
        assertThat(prices).hasSize(2);
        assertThat(prices.get("KRW-BTC")).isEqualByComparingTo("50000000");
        assertThat(prices.get("KRW-ETH")).isEqualByComparingTo("3000000");
    }
}
