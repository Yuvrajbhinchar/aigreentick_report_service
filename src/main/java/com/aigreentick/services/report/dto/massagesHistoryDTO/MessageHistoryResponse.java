package com.aigreentick.services.report.dto.massagesHistoryDTO;

import com.aigreentick.services.report.dto.walletDTO.ChatDTO;
import com.aigreentick.services.report.dto.walletDTO.ContactDTO;
import com.aigreentick.services.report.dto.walletDTO.ReportDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageHistoryResponse {

    private Long id;
    private Long contactId;
    private ContactDTO contact;
    private ChatDTO chat;
    private ReportDTO report;
    private Integer unreadCount;
    private Long lastChatTime;
    private LocalDateTime createdAt;


}



