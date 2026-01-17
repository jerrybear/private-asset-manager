package com.example.assetmanager.service;

import com.example.assetmanager.domain.Asset;
import com.example.assetmanager.domain.AssetType;
import com.example.assetmanager.domain.Account;
import com.example.assetmanager.domain.AccountType;
import com.example.assetmanager.repository.AssetRepository;
import com.example.assetmanager.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AccountRepository accountRepository;
    private final AssetRepository assetRepository;
    private final List<PriceProvider> priceProviders;
    private final GoogleSheetsService googleSheetsService;
    private boolean isInitialSyncing = false;

    public boolean isInitialSyncing() {
        return isInitialSyncing;
    }

    public void setInitialSyncing(boolean initialSyncing) {
        isInitialSyncing = initialSyncing;
    }

    public List<String> getSheetNames() throws Exception {
        return googleSheetsService.getSheetNames();
    }

    @Transactional
    public void exportToGoogleSheets(Long accountId) throws Exception {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        googleSheetsService.updateSheetWithAccount(account);
    }

    @Transactional
    public void syncWithGoogleSheets(Long accountId) throws Exception {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // 시트에서 계좌 정보와 자산 목록을 가져옴
        Account accountFromSheet = googleSheetsService.fetchAccountFromSheet(account.getSheetName());

        // 1. 계좌 메타데이터 업데이트
        if (accountFromSheet.getName() != null && !accountFromSheet.getName().isBlank()) {
            account.setName(accountFromSheet.getName());
        }
        account.setOwner(accountFromSheet.getOwner());
        account.setAccountType(accountFromSheet.getAccountType());
        account.setFinancialInstitution(accountFromSheet.getFinancialInstitution());
        account.setAccountNumber(accountFromSheet.getAccountNumber());
        account.setDescription(accountFromSheet.getDescription());
        accountRepository.save(account);

        // 2. 기존 자산 목록 갱신 (clear & addAll 방식)
        account.getAssets().clear();
        for (Asset asset : accountFromSheet.getAssets()) {
            asset.setAccount(account);
            account.getAssets().add(asset);
        }
        accountRepository.save(account);
    }

    @Transactional
    public Account createAccount(String name, String description, String sheetName, String owner,
            AccountType accountType, String financialInstitution, String accountNumber) {
        if (sheetName != null && !sheetName.isBlank() && accountRepository.existsBySheetName(sheetName)) {
            throw new IllegalArgumentException("이미 등록된 시트 탭입니다: " + sheetName);
        }
        return accountRepository.save(
                new Account(name, description, sheetName, owner, accountType, financialInstitution, accountNumber));
    }

    @Transactional
    public Account updateAccount(Long id, String name, String description, String sheetName, String owner,
            AccountType accountType, String financialInstitution, String accountNumber) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (sheetName != null && !sheetName.isBlank() && !sheetName.equals(account.getSheetName())
                && accountRepository.existsBySheetName(sheetName)) {
            throw new IllegalArgumentException("이미 등록된 시트 탭입니다: " + sheetName);
        }

        account.setName(name);
        account.setDescription(description);
        account.setSheetName(sheetName);
        account.setOwner(owner);
        account.setAccountType(accountType);
        account.setFinancialInstitution(financialInstitution);
        account.setAccountNumber(accountNumber);

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        accountRepository.deleteById(accountId);
    }

    @Transactional
    public void deleteAsset(Long assetId) {
        assetRepository.deleteById(assetId);
    }

    @Transactional
    public Asset addAsset(Long accountId, Asset asset) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        asset.setAccount(account);
        return assetRepository.save(asset);
    }

    @Transactional
    public Asset updateAsset(Long assetId, Asset updatedAsset) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        if (updatedAsset.getType() != null)
            asset.setType(updatedAsset.getType());
        if (updatedAsset.getCode() != null)
            asset.setCode(updatedAsset.getCode());
        if (updatedAsset.getName() != null)
            asset.setName(updatedAsset.getName());
        if (updatedAsset.getQuantity() != null)
            asset.setQuantity(updatedAsset.getQuantity());
        if (updatedAsset.getAveragePurchasePrice() != null)
            asset.setAveragePurchasePrice(updatedAsset.getAveragePurchasePrice());
        if (updatedAsset.getDividendCycle() != null)
            asset.setDividendCycle(updatedAsset.getDividendCycle());
        if (updatedAsset.getDividendPerShare() != null)
            asset.setDividendPerShare(updatedAsset.getDividendPerShare());

        return assetRepository.save(asset);
    }

    public Map<String, Object> getAccountSummary(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return calculateAccountSummary(account);
    }

    private Map<String, Object> calculateAccountSummary(Account account) {
        try {
            List<Asset> assets = assetRepository.findByAccountId(account.getId());

            BigDecimal totalPurchaseAmount = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
            BigDecimal totalExpectedDividend = BigDecimal.ZERO;

            List<Map<String, Object>> assetDetails = new java.util.ArrayList<>();

            Map<String, BigDecimal> allPrices = new HashMap<>();
            for (Asset asset : assets) {
                BigDecimal currentPrice = asset.getCurrentPrice();
                if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) == 0) {
                    if (asset.getType() == AssetType.CASH
                            || (asset.getCode() != null && !asset.getCode().startsWith("KRX:"))) {
                        currentPrice = asset.getAveragePurchasePrice() != null ? asset.getAveragePurchasePrice()
                                : BigDecimal.ZERO;
                    } else {
                        currentPrice = BigDecimal.ZERO;
                    }
                }
                allPrices.put(asset.getCode() != null ? asset.getCode() : "", currentPrice);
            }

            for (Asset asset : assets) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", asset.getId());
                AssetType type = asset.getType() != null ? asset.getType() : AssetType.STOCK;
                detail.put("type", type);
                detail.put("code", asset.getCode() != null ? asset.getCode() : "");
                detail.put("name", asset.getName() != null ? asset.getName() : "Unknown Asset");

                BigDecimal quantity = asset.getQuantity() != null ? asset.getQuantity() : BigDecimal.ZERO;
                BigDecimal averagePrice = asset.getAveragePurchasePrice() != null ? asset.getAveragePurchasePrice()
                        : BigDecimal.ZERO;

                detail.put("quantity", quantity);
                detail.put("averagePurchasePrice", averagePrice);

                BigDecimal purchaseAmount = averagePrice.multiply(quantity);
                totalPurchaseAmount = totalPurchaseAmount.add(purchaseAmount);

                BigDecimal currentPrice = allPrices.getOrDefault(asset.getCode() != null ? asset.getCode() : "",
                        BigDecimal.ZERO);

                BigDecimal currentValue = currentPrice.multiply(quantity);
                BigDecimal profitLoss = currentValue.subtract(purchaseAmount);
                BigDecimal returnRate = (purchaseAmount.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO
                        : profitLoss.divide(purchaseAmount, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));

                detail.put("currentPrice", currentPrice);
                detail.put("lastPriceUpdate", asset.getLastPriceUpdate());
                detail.put("currentValue", currentValue);
                detail.put("profitLoss", profitLoss);
                detail.put("returnRate", returnRate);

                totalCurrentValue = totalCurrentValue.add(currentValue);

                BigDecimal dividendPerShare = asset.getDividendPerShare() != null ? asset.getDividendPerShare()
                        : BigDecimal.ZERO;
                String cycle = asset.getDividendCycle();
                BigDecimal annualMultiplier = BigDecimal.ZERO;

                if ("1개월".equals(cycle)) {
                    annualMultiplier = new BigDecimal("12");
                } else if ("3개월".equals(cycle)) {
                    annualMultiplier = new BigDecimal("4");
                } else if ("6개월".equals(cycle)) {
                    annualMultiplier = new BigDecimal("2");
                } else if ("12개월".equals(cycle)) {
                    annualMultiplier = new BigDecimal("1");
                }

                BigDecimal annualExpectedDividend = dividendPerShare.multiply(annualMultiplier).multiply(quantity);
                totalExpectedDividend = totalExpectedDividend.add(annualExpectedDividend);

                assetDetails.add(detail);
            }

            BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalPurchaseAmount);
            BigDecimal totalReturnRate = (totalPurchaseAmount.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO
                    : totalProfitLoss.divide(totalPurchaseAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

            Map<String, Object> summary = new HashMap<>();
            summary.put("accountId", account.getId());
            summary.put("accountName", account.getName());
            summary.put("owner", account.getOwner());
            summary.put("accountType", account.getAccountType() != null ? account.getAccountType().name() : "SPECIAL");
            summary.put("financialInstitution", account.getFinancialInstitution());
            summary.put("accountNumber", account.getAccountNumber());
            summary.put("totalPurchaseAmount", totalPurchaseAmount);
            summary.put("totalCurrentValue", totalCurrentValue);
            summary.put("totalProfitLoss", totalProfitLoss);
            summary.put("totalReturnRate", totalReturnRate);
            summary.put("totalExpectedDividend", totalExpectedDividend);
            summary.put("assets", assetDetails);

            return summary;
        } catch (Exception e) {
            log.error("Error calculating account summary for {}: {}", account.getId(), e.getMessage(), e);
            throw new RuntimeException("Summary calculation failed", e);
        }
    }

    public List<Map<String, Object>> getAllAccountSummaries() {
        List<Account> accounts = accountRepository.findAll();
        return accounts.stream()
                .map(this::calculateAccountSummary)
                .toList();
    }

    @Transactional
    public void refreshAllPrices(Long accountId, boolean force) {
        List<Asset> assets = assetRepository.findByAccountId(accountId);

        // PublicDataPriceProvider 식별
        PriceProvider publicDataProvider = priceProviders.stream()
                .filter(p -> p instanceof PublicDataPriceProvider)
                .findFirst()
                .orElse(null);

        // KRX: 접두사가 있는 자산들 필터링
        java.time.LocalDateTime threeHoursAgo = java.time.LocalDateTime.now().minusHours(3);
        List<Asset> krxAssetsToRefresh = assets.stream()
                .filter(a -> a.getCode() != null && a.getCode().startsWith("KRX:"))
                .filter(a -> force || a.getLastPriceUpdate() == null || a.getLastPriceUpdate().isBefore(threeHoursAgo))
                .collect(Collectors.toList());

        // KRX 자산들은 일괄 조회 (효율성 위해 getMultiplePrices 사용)
        if (publicDataProvider != null && !krxAssetsToRefresh.isEmpty()) {
            List<String> codes = krxAssetsToRefresh.stream()
                    .map(Asset::getCode)
                    .collect(Collectors.toList());

            Map<String, BigDecimal> prices = publicDataProvider.getMultiplePrices(codes);

            for (Asset asset : krxAssetsToRefresh) {
                BigDecimal newPrice = prices.get(asset.getCode());
                if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
                    asset.setCurrentPrice(newPrice);
                    asset.setLastPriceUpdate(java.time.LocalDateTime.now());
                }
            }
        }

        // KRX: 접두사가 없거나 코드가 없는 자산들은 평균단가를 현재가로 설정 (Profit/Loss 0 처리)
        assets.stream()
                .filter(a -> a.getCode() == null || !a.getCode().startsWith("KRX:"))
                .forEach(a -> {
                    a.setCurrentPrice(
                            a.getAveragePurchasePrice() != null ? a.getAveragePurchasePrice() : BigDecimal.ZERO);
                    a.setLastPriceUpdate(java.time.LocalDateTime.now());
                });

        assetRepository.saveAll(assets);
    }

    @Transactional
    public BigDecimal refreshAssetPrice(Long accountId, Long assetId, boolean force) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        if (!asset.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("Asset does not belong to this account");
        }

        // 3시간 이내에 업데이트된 가격이 있다면 재사용 (force=true인 경우 무시)
        java.time.LocalDateTime threeHoursAgo = java.time.LocalDateTime.now().minusHours(3);
        if (!force && asset.getLastPriceUpdate() != null && asset.getLastPriceUpdate().isAfter(threeHoursAgo)
                && asset.getCurrentPrice() != null && asset.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Reusing recent price for asset: {} (updated at {})", asset.getCode(), asset.getLastPriceUpdate());
            return asset.getCurrentPrice();
        }

        String code = asset.getCode();
        // KRX: 접두사가 있는 경우에만 외부 API 조회 (유형 무관)
        if (code != null && code.startsWith("KRX:")) {
            PriceProvider provider = priceProviders.stream()
                    .filter(p -> p instanceof PublicDataPriceProvider)
                    .findFirst()
                    .orElse(null);

            if (provider != null) {
                BigDecimal newPrice = provider.getCurrentPrice(code);
                if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
                    asset.setCurrentPrice(newPrice);
                    asset.setLastPriceUpdate(java.time.LocalDateTime.now());
                    assetRepository.save(asset);
                    return newPrice;
                }
            }
        } else {
            // 그 외의 경우 (현금, 기타 자산 등) 평균단가를 현재가로 고정
            BigDecimal averagePrice = asset.getAveragePurchasePrice() != null ? asset.getAveragePurchasePrice()
                    : BigDecimal.ZERO;
            asset.setCurrentPrice(averagePrice);
            asset.setLastPriceUpdate(java.time.LocalDateTime.now());
            assetRepository.save(asset);
            return averagePrice;
        }
        return asset.getCurrentPrice() != null ? asset.getCurrentPrice() : BigDecimal.ZERO;
    }
}
