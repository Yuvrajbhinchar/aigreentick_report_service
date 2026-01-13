package com.aigreentick.services.report.dto.campaignHistoryDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("broadcast_id")
    private Long broadcastId;

    @JsonProperty("campaign_id")
    private Long campaignId;

    @JsonProperty("group_send_id")
    private Long groupSendId;

    @JsonProperty("tag_log_id")
    private Long tagLogId;

    private String mobile;

    private String type;

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("wa_id")
    private String waId;

    @JsonProperty("message_status")
    private String messageStatus;

    private String status;

    private String payload;

    private String response;

    private String contact;

    @JsonProperty("payment_status")
    @Builder.Default
    private Integer paymentStatus = 0;

    private String platform;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;
}
