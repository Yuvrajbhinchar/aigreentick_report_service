package com.aigreentick.services.report.dto.MessageHistoryDTO;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfo {
    private Long id;
    private String name;
    private String mobile;
    private String email;

    @JsonProperty("country_id")
    private String countryId;
}
