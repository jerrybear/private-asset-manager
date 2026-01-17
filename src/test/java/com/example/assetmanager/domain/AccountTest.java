package com.example.assetmanager.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    @DisplayName("계좌 생성 및 필드 값 검증")
    void createAccountTest() {
        // given
        String name = "테스트 계좌";
        String description = "테스트 설명";
        String sheetName = "Sheet1";
        String owner = "홍길동";
        AccountType accountType = AccountType.REGULAR;
        String financialInstitution = "한국투자증권";
        String accountNumber = "123-456-789";

        // when
        Account account = new Account(name, description, sheetName, owner, accountType, financialInstitution,
                accountNumber);

        // then
        assertThat(account.getName()).isEqualTo(name);
        assertThat(account.getDescription()).isEqualTo(description);
        assertThat(account.getSheetName()).isEqualTo(sheetName);
        assertThat(account.getOwner()).isEqualTo(owner);
        assertThat(account.getAccountType()).isEqualTo(accountType);
        assertThat(account.getFinancialInstitution()).isEqualTo(financialInstitution);
        assertThat(account.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(account.getAssets()).isEmpty();
    }

    @Test
    @DisplayName("계좌 타입 변경 테스트")
    void updateAccountTypeTest() {
        // given
        Account account = new Account();
        account.setAccountType(AccountType.REGULAR);

        // when
        account.setAccountType(AccountType.ISA);

        // then
        assertThat(account.getAccountType()).isEqualTo(AccountType.ISA);
    }
}
