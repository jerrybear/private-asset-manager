package com.example.assetmanager.service;

import com.example.assetmanager.domain.AssetType;
import java.math.BigDecimal;
import java.util.Map;

public interface PriceProvider {
    boolean supports(AssetType type);

    BigDecimal getCurrentPrice(String code);

    Map<String, BigDecimal> getMultiplePrices(Iterable<String> codes);
}
