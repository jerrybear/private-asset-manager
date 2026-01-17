package com.example.assetmanager.config;

import com.example.assetmanager.domain.Account;
import com.example.assetmanager.domain.AccountType;
import com.example.assetmanager.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.assetmanager.service.GoogleSheetsService;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final GoogleSheetsService googleSheetsService;

    @Bean
    CommandLineRunner initData(AssetService assetService) {
        return args -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    assetService.setInitialSyncing(true);
                    System.out.println("Starting initial Google Sheets sync...");

                    // [RAWDATA] 프리픽스가 붙은 시트 탭 자동 등록 및 동기화
                    googleSheetsService.getSheetNames().stream()
                            .filter(name -> name.startsWith("[RAWDATA]"))
                            .forEach(sheetName -> {
                                try {
                                    // 계좌가 없으면 생성 (이름은 [RAWDATA] 제외)
                                    String accountName = sheetName.replace("[RAWDATA]", "").trim();
                                    Account account;
                                    try {
                                        account = assetService.createAccount(accountName,
                                                "자동 등록된 계좌 (" + sheetName + ")",
                                                sheetName,
                                                "시스템",
                                                AccountType.REGULAR,
                                                "미지정",
                                                "-");
                                        System.out.println("Auto-registered [RAWDATA] account: " + accountName);
                                    } catch (IllegalArgumentException e) {
                                        account = assetService.getAllAccounts().stream()
                                                .filter(a -> a.getSheetName().equals(sheetName))
                                                .findFirst()
                                                .orElse(null);
                                    }

                                    if (account != null) {
                                        assetService.syncWithGoogleSheets(account.getId());
                                        System.out.println("Auto-synced [RAWDATA] account: " + accountName);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Failed to auto-process [RAWDATA] sheet: " + sheetName);
                                    e.printStackTrace();
                                }
                            });
                } catch (Exception e) {
                    System.err.println("Failed to fetch sheet names during DataLoader execution");
                    e.printStackTrace();
                } finally {
                    assetService.setInitialSyncing(false);
                    System.out.println("Initial DataLoader execution completed.");
                }
            });
        };
    }
}
