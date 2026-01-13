package com.aigreentick.services.report.dto.walletDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {
    private Long id;
    private String type;
    private Double amount;
    private String description;
    private BroadcastDTO broadcast;
    private String createdAt;
}