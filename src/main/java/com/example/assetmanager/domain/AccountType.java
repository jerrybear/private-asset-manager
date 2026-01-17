package com.example.assetmanager.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountType {
    REGULAR("일반"),
    PENSION("연금"),
    ISA("ISA"),
    IRP("IRP"),
    SPECIAL("기타특수");

    private final String description;
}
