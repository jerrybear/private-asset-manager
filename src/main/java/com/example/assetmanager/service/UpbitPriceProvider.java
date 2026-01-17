package com.example.assetmanager.service;

import com.example.assetmanager.domain.AssetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpbitPriceProvider implements PriceProvider {

    private final WebClient webClient;
    private static final String UPBIT_API_URL = "https://api.upbit.com/v1/ticker";

    @Override
    public boolean supports(AssetType type) {
        return type == AssetType.CRYPTO;
    }

    @Override
    public BigDecimal getCurrentPrice(String code) {
        if (code == null || code.isBlank())
            return BigDecimal.ZERO;

        // 코드는 "KRW-BTC" 형식이어야 함. 접두사 없으면 추가.
        String market = code.contains("-") ? code : "KRW-" + code;

        try {
            List<Map<String, Object>> response = webClient.get()
                    .uri(URI.create(UPBIT_API_URL + "?markets=" + market))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .block();

            if (response != null && !response.isEmpty()) {
                Object price = response.get(0).get("trade_price");
                return price != null ? new BigDecimal(price.toString()) : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            // log field from Slf4j or manually added if not present
            // Let's assume the user might have Slf4j, if not I'll use System.err
            System.err.println("Error fetching price from Upbit for " + market + ": " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Map<String, BigDecimal> getMultiplePrices(Iterable<String> codes) {
        String markets = String.join(",", codes);
        List<Map<String, Object>> response = webClient.get()
                .uri(URI.create(UPBIT_API_URL + "?markets=" + markets))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .block();

        Map<String, BigDecimal> prices = new HashMap<>();
        if (response != null) {
            for (Map<String, Object> ticker : response) {
                Object market = ticker.get("market");
                Object price = ticker.get("trade_price");
                if (market != null && price != null) {
                    prices.put(market.toString(), new BigDecimal(price.toString()));
                }
            }
        }
        return prices;
    }
}
