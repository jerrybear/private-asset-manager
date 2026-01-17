package com.example.assetmanager.controller;

import com.example.assetmanager.domain.Asset;
import com.example.assetmanager.domain.Account;
import com.example.assetmanager.domain.AccountType;
import com.example.assetmanager.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public List<Account> getAllAccounts() {
        return assetService.getAllAccounts();
    }

    @GetMapping("/sync-status")
    public Map<String, Boolean> getSyncStatus() {
        return Map.of("isInitialSyncing", assetService.isInitialSyncing());
    }

    @PostMapping
    public Account createAccount(@RequestBody Map<String, String> request) {
        return assetService.createAccount(
                request.get("name"),
                request.get("description"),
                request.get("sheetName"),
                request.get("owner"),
                AccountType.valueOf(request.getOrDefault("accountType", "REGULAR")),
                request.get("financialInstitution"),
                request.get("accountNumber"));
    }

    @PutMapping("/{accountId}")
    public Account updateAccount(@PathVariable Long accountId, @RequestBody Map<String, String> request) {
        return assetService.updateAccount(
                accountId,
                request.get("name"),
                request.get("description"),
                request.get("sheetName"),
                request.get("owner"),
                AccountType.valueOf(request.getOrDefault("accountType", "REGULAR")),
                request.get("financialInstitution"),
                request.get("accountNumber"));
    }

    @DeleteMapping("/{accountId}")
    public void deleteAccount(@PathVariable Long accountId) {
        assetService.deleteAccount(accountId);
    }

    @PostMapping("/{accountId}/assets")
    public Asset addAsset(@PathVariable Long accountId, @RequestBody Asset asset) {
        return assetService.addAsset(accountId, asset);
    }

    @PutMapping("/{accountId}/assets/{assetId}")
    public Asset updateAsset(@PathVariable Long accountId, @PathVariable Long assetId, @RequestBody Asset asset) {
        return assetService.updateAsset(assetId, asset);
    }

    @DeleteMapping("/{accountId}/assets/{assetId}")
    public void deleteAsset(@PathVariable Long accountId, @PathVariable Long assetId) {
        assetService.deleteAsset(assetId);
    }

    @GetMapping("/{accountId}/summary")
    public Map<String, Object> getAccountSummary(@PathVariable Long accountId) {
        return assetService.getAccountSummary(accountId);
    }

    @GetMapping("/summary")
    public List<Map<String, Object>> getAllAccountSummaries() {
        return assetService.getAllAccountSummaries();
    }

    @GetMapping("/sheet-names")
    public List<String> getSheetNames() throws Exception {
        return assetService.getSheetNames();
    }

    @PostMapping("/{accountId}/sync")
    public Map<String, String> syncAccount(@PathVariable Long accountId) {
        try {
            assetService.syncWithGoogleSheets(accountId);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Synced with Google Sheets");
            return response;
        } catch (Exception e) {
            log.error("Error during sync for account {}: {}", accountId, e.getMessage(), e);
            throw new RuntimeException("Sync failed: " + e.getMessage());
        }
    }

    @PostMapping("/{accountId}/export")
    public Map<String, String> exportAccount(@PathVariable Long accountId) {
        try {
            assetService.exportToGoogleSheets(accountId);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Exported to Google Sheets");
            return response;
        } catch (Exception e) {
            log.error("Error during export for account {}: {}", accountId, e.getMessage(), e);
            throw new RuntimeException("Export failed: " + e.getMessage());
        }
    }

    @PostMapping("/{accountId}/refresh-prices")
    public Map<String, String> refreshAllPrices(@PathVariable Long accountId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        assetService.refreshAllPrices(accountId, force);
        return Map.of("status", "success", "message", "All asset prices refreshed" + (force ? " (forced)" : ""));
    }

    @PostMapping("/{accountId}/assets/{assetId}/refresh-price")
    public Map<String, Object> refreshAssetPrice(@PathVariable Long accountId, @PathVariable Long assetId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        BigDecimal newPrice = assetService.refreshAssetPrice(accountId, assetId, force);
        return Map.of("status", "success", "newPrice", newPrice, "forced", force);
    }
}
