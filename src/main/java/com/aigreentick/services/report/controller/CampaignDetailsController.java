package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.campaignHistoryDTO.ApiResponse;
import com.aigreentick.services.report.service.CampaignDetailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class CampaignDetailsController {

    private final CampaignDetailService campaignDetailService;

    @GetMapping("/campaignDetails")
    public ResponseEntity<ApiResponse> getCampaignDetails(@RequestParam Long campaignId,
                                                          @RequestParam (defaultValue = "1")int page,
                                                          @RequestParam (defaultValue = "10")int perPage,
                                                          HttpServletRequest request
    ) {

        String basePath = request.getRequestURL().toString();

        ApiResponse response =  campaignDetailService.getReportsByBroadcastId(campaignId,page,perPage);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
