package com.example.assetmanager.service;

import com.example.assetmanager.domain.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicDataPriceProviderTest {

    private PublicDataPriceProvider publicDataPriceProvider;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        publicDataPriceProvider = new PublicDataPriceProvider(webClient);
        ReflectionTestUtils.setField(publicDataPriceProvider, "serviceKey", "test-key");
    }

    @Test
    @DisplayName("지원하는 자산 타입 확인")
    void supports_ReturnsTrueForKrxAssets() {
        assertThat(publicDataPriceProvider.supports(AssetType.STOCK)).isTrue();
        assertThat(publicDataPriceProvider.supports(AssetType.STOCK_KR)).isTrue();
        assertThat(publicDataPriceProvider.supports(AssetType.REITS)).isTrue();
        assertThat(publicDataPriceProvider.supports(AssetType.BOND_KR)).isTrue();
        assertThat(publicDataPriceProvider.supports(AssetType.ETF_KR)).isTrue();
        assertThat(publicDataPriceProvider.supports(AssetType.STOCK_US)).isFalse();
    }

    @Test
    @DisplayName("KRX: 접두사 제거 및 API 호출 테스트")
    void getCurrentPrice_CleansCodeAndCallsApi() {
        // given
        String code = "KRX:005930";
        BigDecimal expectedPrice = new BigDecimal("70000");

        Map<String, Object> mockResponse = createMockApiResponse(expectedPrice);

        setupWebClientMock(mockResponse);

        // when
        BigDecimal price = publicDataPriceProvider.getCurrentPrice(code);

        // then
        assertThat(price).isEqualTo(expectedPrice);
    }

    @Test
    @DisplayName("첫 번째 API(주식) 실패 시 두 번째 API(ETF) 호출 확인")
    void getCurrentPrice_FallsBackToEtfApi() {
        // given
        String code = "069500"; // KODEX 200 (ETF)
        BigDecimal expectedPrice = new BigDecimal("35000");

        // mockResponse for STOCK_API (empty result)
        Map<String, Object> emptyResponse = createMockEmptyResponse();
        // mockResponse for ETF_API
        Map<String, Object> etfResponse = createMockApiResponse(expectedPrice);

        // WebClient Mock을 순차적으로 다르게 반환하도록 설정해야 함
        // 여기서는 단순화를 위해 STOCK_API 호출 결과가 0일 때 ETF_API를 호출하는 로직을 검증
        // 실제 코드에서는 fetchFromApi를 두 번 호출함

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(emptyResponse)) // first call
                .thenReturn(Mono.just(etfResponse)); // second call

        // when
        BigDecimal price = publicDataPriceProvider.getCurrentPrice(code);

        // then
        assertThat(price).isEqualTo(expectedPrice);
    }

    private Map<String, Object> createMockApiResponse(BigDecimal price) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> innerResponse = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> items = new HashMap<>();
        Map<String, Object> item = new HashMap<>();

        item.put("clpr", price.toString());
        items.put("item", List.of(item));
        body.put("items", items);
        innerResponse.put("body", body);
        response.put("response", innerResponse);

        return response;
    }

    private Map<String, Object> createMockEmptyResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> innerResponse = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> items = new HashMap<>();

        items.put("item", List.of());
        body.put("items", items);
        innerResponse.put("body", body);
        response.put("response", innerResponse);

        return response;
    }

    private void setupWebClientMock(Map<String, Object> response) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
    }
}
