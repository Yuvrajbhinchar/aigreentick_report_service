package com.aigreentick.services.report.dto.massagesHistoryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactDTO {
    private Long id;
    private String name;
    private String mobile;
    private String email;
    private String countryId;
}

