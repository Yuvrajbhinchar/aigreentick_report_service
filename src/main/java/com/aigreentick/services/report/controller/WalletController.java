package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.WalletHistoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    @GetMapping("/transactionHistory")
    public WalletHistoryResponse getWalletHistory(@PathVariable int page) {
        return null;
    }
}
