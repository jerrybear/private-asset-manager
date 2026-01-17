package com.example.assetmanager.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DividendHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code; // 종목코드
    private BigDecimal amountPerShare; // 주당 배당금
    private LocalDate paymentDate; // 지급일
    private String period; // 월/분기/연

    public DividendHistory(String code, BigDecimal amountPerShare, LocalDate paymentDate, String period) {
        this.code = code;
        this.amountPerShare = amountPerShare;
        this.paymentDate = paymentDate;
        this.period = period;
    }
}
