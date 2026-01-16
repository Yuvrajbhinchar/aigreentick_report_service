package com.aigreentick.services.report.dto.chatHistoryDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryDTO {
    private UsersPageDTO users;
    private List<ChannelDTO> channel;

}

