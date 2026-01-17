package com.example.assetmanager.controller;

import com.example.assetmanager.domain.Account;
import com.example.assetmanager.domain.AccountType;
import com.example.assetmanager.service.AssetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssetController.class)
class AssetControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AssetService assetService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("모든 계좌 조회 API 테스트")
        void getAllAccounts_Success() throws Exception {
                // given
                Account account = new Account("Test Account", "Desc", "Sheet1", "Owner", AccountType.REGULAR, "Bank",
                                "123-456");
                given(assetService.getAllAccounts()).willReturn(List.of(account));

                // when & then
                mockMvc.perform(get("/api/accounts"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Test Account"));
        }

        @Test
        @DisplayName("계좌 생성 API 테스트")
        void createAccount_Success() throws Exception {
                // given
                Map<String, String> request = Map.of(
                                "name", "New Account",
                                "description", "New Desc",
                                "sheetName", "Sheet2",
                                "owner", "Owner",
                                "accountType", "REGULAR",
                                "financialInstitution", "Bank",
                                "accountNumber", "123-456");
                Account account = new Account("New Account", "New Desc", "Sheet2", "Owner", AccountType.REGULAR, "Bank",
                                "123-456");
                given(assetService.createAccount(anyString(), anyString(), anyString(), anyString(),
                                any(AccountType.class),
                                anyString(), anyString())).willReturn(account);

                // when & then
                mockMvc.perform(post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("New Account"));
        }

        @Test
        @DisplayName("계좌 요약 정보 조회 API 테스트")
        void getAccountSummary_Success() throws Exception {
                // given
                Long accountId = 1L;
                Map<String, Object> summary = Map.of("accountName", "Test Account", "totalValue", 1000000);
                given(assetService.getAccountSummary(accountId)).willReturn(summary);

                // when & then
                mockMvc.perform(get("/api/accounts/{accountId}/summary", accountId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accountName").value("Test Account"));
        }

        @Test
        @DisplayName("계좌 동기화 API 테스트")
        void syncAccount_Success() throws Exception {
                // given
                Long accountId = 1L;
                // void return이므로 특별한 return 설정 불필요

                // when & then
                mockMvc.perform(post("/api/accounts/{accountId}/sync", accountId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.message").value("Synced with Google Sheets"));
        }

        @Test
        @DisplayName("계좌 내보내기 API 테스트")
        void exportAccount_Success() throws Exception {
                // given
                Long accountId = 1L;

                // when & then
                mockMvc.perform(post("/api/accounts/{accountId}/export", accountId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.message").value("Exported to Google Sheets"));
        }

        @Test
        @DisplayName("동기화 실패 시 500 에러와 메시지 반환 테스트")
        void syncAccount_Failure() throws Exception {
                // given
                Long accountId = 1L;
                org.mockito.BDDMockito.willThrow(new RuntimeException("Sync error")).given(assetService)
                                .syncWithGoogleSheets(accountId);

                // when & then
                mockMvc.perform(post("/api/accounts/{accountId}/sync", accountId))
                                .andExpect(status().isInternalServerError());
                // GlobalExceptionHandler가 설정되어 있다면 적절한 JSON이 올 것이고,
                // 아니라면 스프링 기본 에러 처리에 따라 500이 올 것임.
        }
}
