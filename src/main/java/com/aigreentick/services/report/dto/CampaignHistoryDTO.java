package com.aigreentick.services.report.dto;

import com.aigreentick.services.report.entity.Broadcast;
import com.aigreentick.services.report.entity.Template;
import com.aigreentick.services.report.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignHistoryDTO {

    private Boolean status;
    private Page<BroadcastReportDTO> data;
    private String error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BroadcastReportDTO {

        private Long id;
        private String source;

        @JsonProperty("user_id")
        private Long userId;

        @JsonProperty("wallet_id")
        private Integer walletId;

        @JsonProperty("template_id")
        private Long templateId;

        private Integer whatsapp;

        @JsonProperty("country_id")
        private Long countryId;

        private String campname;

        @JsonProperty("is_media")
        private char isMedia;

        private Map<String, Object> data;

        private Integer total;

        @JsonProperty("schedule_at")
        private LocalDateTime scheduleAt;

        private char status;


        private String numbers;
        private String requests;

        // Status counts
        @JsonProperty("sent_count")
        private Long sentCount;

        @JsonProperty("read_count")
        private Long readCount;

        @JsonProperty("dlvd_count")
        private Long dlvdCount;

        @JsonProperty("failed_count")
        private Long failedCount;

        @JsonProperty("other_count")
        private Long otherCount;

        @JsonProperty("process")
        private Long processCount;

        @JsonProperty("pending_count")
        private Long pendingCount;

        // Timestamps
        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;

        @JsonProperty("deleted_at")
        private LocalDateTime deletedAt;

        // Relationships (only include necessary fields)
        private User user;
        private Template template;
    }
}