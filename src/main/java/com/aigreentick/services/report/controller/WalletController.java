package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.walletDTO.WalletHistoryResponse;
import com.aigreentick.services.report.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/transactionHistory")
    public ResponseEntity<WalletHistoryResponse> getWalletHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Long userId = 41L; // replace with auth user

        // Build base path for pagination URLs
        String basePath = request.getRequestURL().toString();

        WalletHistoryResponse response = walletService.getWalletHistory(userId, page, size, basePath);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
