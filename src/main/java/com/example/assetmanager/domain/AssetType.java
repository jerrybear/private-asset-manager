package com.example.assetmanager.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssetType {
    STOCK("주식"),
    STOCK_KR("국내주식"),
    STOCK_US("해외주식"),
    ETF_KR("국내ETF"),
    CRYPTO("가상화폐"),
    CASH("현금"),
    RP("RP"),
    ISSUED_NOTE("발행어음"),
    BOND("채권"),
    BOND_KR("국내채권"),
    BOND_US("해외채권"),
    REITS("리츠"),
    COMMODITY("원자재"),
    DEPOSIT_SAVINGS("예적금"),
    GOLD_SPOT("금현물");

    private final String description;
}
