package com.aigreentick.services.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletHistoryResponse {
    private String message;
    private Double totalDebit;
    private Double totalCredit;
    private Double balance;
    private TransactionPageData data;
}
