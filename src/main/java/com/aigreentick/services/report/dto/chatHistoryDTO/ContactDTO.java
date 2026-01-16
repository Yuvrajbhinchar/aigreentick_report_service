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
public class ContactDTO {
    private Integer id;
    private String name;
    private String mobile;
    private String email;

    @JsonProperty("country_id")
    private String countryId;
}