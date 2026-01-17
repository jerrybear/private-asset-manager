package com.example.assetmanager.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    @Column(unique = true)
    private String sheetName;

    private String owner;
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    private String financialInstitution;
    private String accountNumber;

    @Builder.Default
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Asset> assets = new ArrayList<>();

    public Account(String name, String description, String sheetName, String owner, AccountType accountType,
            String financialInstitution, String accountNumber) {
        this.name = name;
        this.description = description;
        this.sheetName = sheetName;
        this.owner = owner;
        this.accountType = accountType;
        this.financialInstitution = financialInstitution;
        this.accountNumber = accountNumber;
        this.assets = new ArrayList<>();
    }
}
