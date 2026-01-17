package com.example.assetmanager.service;

import com.example.assetmanager.domain.AssetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
@Order(1)
public class PublicDataPriceProvider implements PriceProvider {

    private final WebClient webClient;

    @Value("${public-data.api.key}")
    private String serviceKey;

    private static final String STOCK_API_PATH = "/1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo";
    private static final String ETF_API_PATH = "/1160100/service/GetSecuritiesProductInfoService/getETFPriceInfo";

    @Override
    public boolean supports(AssetType type) {
        // 국내 주식 관련 타입들 지원 (STOCK, STOCK_KR, REITS, BOND_KR, ETF_KR)
        return type == AssetType.STOCK || type == AssetType.STOCK_KR || type == AssetType.REITS
                || type == AssetType.BOND_KR || type == AssetType.ETF_KR;
    }

    @Override
    public BigDecimal getCurrentPrice(String code) {
        if (serviceKey == null || serviceKey.equals("YOUR_SERVICE_KEY_HERE") || code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }

        // 구글 시트 등에서 사용하는 Prefix 제거 (예: KRX:0082V0 -> 0082V0)
        String cleanCode = code;
        if (code.contains(":")) {
            cleanCode = code.substring(code.lastIndexOf(":") + 1);
        }

        // 1. 주식 시세 정보 API 호출 시도
        BigDecimal price = fetchFromApi(STOCK_API_PATH, cleanCode);

        // 2. 결과가 없으면 ETF 시세 정보 API 호출 시도
        if (price.compareTo(BigDecimal.ZERO) == 0) {
            price = fetchFromApi(ETF_API_PATH, cleanCode);
        }

        return price;
    }

    private BigDecimal fetchFromApi(String path, String code) {
        try {
            // 사용자가 관리하는 단축코드(srtnCd)를 기반으로 likeSrtnCd 파라미터만 사용
            return callApi(path, "likeSrtnCd", code);
        } catch (Exception e) {
            log.error("Error calling Public Data API [{}] for {}: {}", path, code, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal callApi(String path, String paramName, String paramValue) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apis.data.go.kr")
                            .path(path)
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("resultType", "json")
                            .queryParam(paramName, paramValue)
                            .queryParam("numOfRows", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            log.info("API Request Path: {}, Param: {}={}, Result: {}", path, paramName, paramValue, response);
            return extractPrice(response);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal extractPrice(Map<String, Object> response) {
        try {
            if (response != null && response.get("response") instanceof Map) {
                Map<String, Object> res = (Map<String, Object>) response.get("response");
                if (res.get("body") instanceof Map) {
                    Map<String, Object> body = (Map<String, Object>) res.get("body");
                    if (body.get("items") instanceof Map) {
                        Map<String, Object> items = (Map<String, Object>) body.get("items");
                        if (items.get("item") instanceof List) {
                            List<Map<String, Object>> itemList = (List<Map<String, Object>>) items.get("item");
                            if (!itemList.isEmpty()) {
                                Map<String, Object> firstItem = itemList.get(0);
                                Object clpr = firstItem.get("clpr"); // 종가
                                if (clpr == null) {
                                    clpr = firstItem.get("trdpr"); // 거래가 (ETF)
                                }
                                return clpr != null ? new BigDecimal(clpr.toString()) : BigDecimal.ZERO;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // parsing error
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Map<String, BigDecimal> getMultiplePrices(Iterable<String> codes) {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (String code : codes) {
            prices.put(code, getCurrentPrice(code));
        }
        return prices;
    }
}
