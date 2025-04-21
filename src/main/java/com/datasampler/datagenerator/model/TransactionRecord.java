package com.datasampler.datagenerator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord {
    private String accountUid;
    private String productCd;
    private LocalDate txnPostedDate;
    private LocalDate txnDate;
    private String txnType;
    private BigDecimal amount;
    private String category;
    private String subCategory;
    private int txnUid;
    private String tokenizedPan;
    private String last4digitNbr;
    private String primaryKey; // Composite key: accountUid_productCd_txnPostedDate_txnUid
}
