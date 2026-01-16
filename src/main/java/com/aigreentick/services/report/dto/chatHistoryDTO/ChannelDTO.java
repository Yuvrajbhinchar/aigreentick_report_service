package com.aigreentick.services.report.dto.chatHistoryDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDTO {
    private Integer id;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("created_by")
    private Integer createdBy;

    @JsonProperty("whatsapp_no")
    private String whatsappNo;

    @JsonProperty("whatsapp_no_id")
    private String whatsappNoId;

    @JsonProperty("whatsapp_biz_id")
    private String whatsappBizId;

    @JsonProperty("parmenent_token")
    private String parmenentToken;

    private String token;
    private String status;
    private String response;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("deleted_at")
    private String deletedAt;
}