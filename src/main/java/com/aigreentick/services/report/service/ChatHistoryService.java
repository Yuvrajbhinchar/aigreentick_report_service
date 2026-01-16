package com.aigreentick.services.report.service;


import com.aigreentick.services.report.dto.chatHistoryDTO.*;
import com.aigreentick.services.report.mapper.ChatHistoryMapper;
import com.aigreentick.services.report.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ContactMessageRepository contactMessageRepository;
    private final ChatHistoryMapper chatHistoryMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.base-url:https://aigreentick.com}")
    private String baseUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public ChatHistoryDTO getMessagesHistory(
            Integer userId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String search,
            Integer page,
            Integer perPage
    ) {
        Pageable pageable = PageRequest.of(page - 1, perPage);

        Page<Map<String, Object>> resultPage = contactMessageRepository.findConversationsWithAllData(
                fromDate, toDate, search, pageable
        );

        List<ConversationDTO> conversations = resultPage.getContent().stream()
                .map(chatHistoryMapper::mapToConversationDTO)
                .collect(Collectors.toList());

        List<ChannelDTO> channelDTOs = fetchChannelData(userId);

        String apiPath = baseUrl + "/api/v1/get-messages-history";

//        UsersPageDTO usersPage = UsersPageDTO.builder()
//                .currentPage(page)
//                .data(conversations)
//                .firstPageUrl(apiPath + "?page=1")
//                .from((page - 1) * perPage + 1)
//                .lastPage(resultPage.getTotalPages())
//                .lastPageUrl(apiPath + "?page=" + resultPage.getTotalPages())
//                .links(buildLinks(apiPath, page, resultPage.getTotalPages()))
//                .nextPageUrl(resultPage.hasNext() ? apiPath + "?page=" + (page + 1) : null)
//                .path(apiPath)
//                .perPage(perPage)
//                .prevPageUrl(page > 1 ? apiPath + "?page=" + (page - 1) : null)
//                .to(Math.min(page * perPage, (int) resultPage.getTotalElements()))
//                .total((int) resultPage.getTotalElements())
//                .build();

//        return ChatHistoryDTO.builder()
//                .users(usersPage)
//                .channel(channelDTOs)
//                .build();

        return null;
    }

    private List<ChannelDTO> fetchChannelData(Integer userId) {
        String sql = """
            SELECT id, user_id, created_by, whatsapp_no, whatsapp_no_id, 
                   whatsapp_biz_id, parmenent_token, token, status, response,
                   created_at, updated_at, deleted_at
            FROM whatsapp_numbers 
            WHERE user_id = ? AND deleted_at IS NULL
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        ChannelDTO.builder()
                                .id(rs.getInt("id"))
                                .userId(rs.getInt("user_id"))
                                .createdBy(rs.getObject("created_by") != null ? rs.getInt("created_by") : null)
                                .whatsappNo(rs.getString("whatsapp_no"))
                                .whatsappNoId(rs.getString("whatsapp_no_id"))
                                .whatsappBizId(rs.getString("whatsapp_biz_id"))
                                .parmenentToken(rs.getString("parmenent_token"))
                                .token(rs.getString("token"))
                                .status(rs.getString("status"))
                                .response(rs.getString("response"))
                                .createdAt(rs.getTimestamp("created_at") != null ?
                                        rs.getTimestamp("created_at").toLocalDateTime().format(FORMATTER) : null)
                                .updatedAt(rs.getTimestamp("updated_at") != null ?
                                        rs.getTimestamp("updated_at").toLocalDateTime().format(FORMATTER) : null)
                                .deletedAt(rs.getTimestamp("deleted_at") != null ?
                                        rs.getTimestamp("deleted_at").toLocalDateTime().format(FORMATTER) : null)
                                .build(),
                userId
        );
    }

    private List<LinkDTO> buildLinks(String baseUrl, int currentPage, int totalPages) {
        List<LinkDTO> links = new ArrayList<>();

        links.add(LinkDTO.builder()
                .url(currentPage > 1 ? baseUrl + "?page=" + (currentPage - 1) : null)
                .label("&laquo; Previous")
                .active(false)
                .build());

        int startPage = Math.max(1, currentPage - 4);
        int endPage = Math.min(totalPages, currentPage + 5);

        if (startPage > 1) {
            links.add(LinkDTO.builder()
                    .url(baseUrl + "?page=1")
                    .label("1")
                    .active(currentPage == 1)
                    .build());

            if (startPage > 2) {
                links.add(LinkDTO.builder()
                        .url(null)
                        .label("...")
                        .active(false)
                        .build());
            }
        }

        for (int i = startPage; i <= endPage; i++) {
            links.add(LinkDTO.builder()
                    .url(baseUrl + "?page=" + i)
                    .label(String.valueOf(i))
                    .active(i == currentPage)
                    .build());
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                links.add(LinkDTO.builder()
                        .url(null)
                        .label("...")
                        .active(false)
                        .build());
            }

            links.add(LinkDTO.builder()
                    .url(baseUrl + "?page=" + totalPages)
                    .label(String.valueOf(totalPages))
                    .active(currentPage == totalPages)
                    .build());
        }

        links.add(LinkDTO.builder()
                .url(currentPage < totalPages ? baseUrl + "?page=" + (currentPage + 1) : null)
                .label("Next &raquo;")
                .active(false)
                .build());

        return links;
    }
}