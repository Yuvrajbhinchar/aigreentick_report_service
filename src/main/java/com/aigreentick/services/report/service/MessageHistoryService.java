package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.MessageHistoryDTO.MessageHistoryPageResponse;
import com.aigreentick.services.report.dto.MessageHistoryDTO.MessageHistoryWrapperResponse;
import com.aigreentick.services.report.dto.MessageHistoryDTO.PageLink;
import com.aigreentick.services.report.dto.MessageHistoryDTO.*;
import com.aigreentick.services.report.repository.MessageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageHistoryService {

    private final MessageHistoryRepository repository;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MessageHistoryWrapperResponse getMessagesHistory(
            Long userId,
            int perPage,
            int page,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        long startTime = System.currentTimeMillis();
        log.info("=== OPTIMIZED MessageHistoryService START - User: {}, Page: {}, PerPage: {}, Filter: {} ===",
                userId, page, perPage, filter);

        int offset = (page - 1) * perPage;

        // SINGLE QUERY: Get all data at once (messages + metadata)
        List<Map<String, Object>> messages = repository.getLatestMessagesOptimized(
                userId, search, filter, fromDate, toDate, perPage, offset
        );

        log.info("Step 1: Got {} messages with metadata in {}ms",
                messages.size(), System.currentTimeMillis() - startTime);

        if (messages.isEmpty()) {
            return buildEmptyResponse(page, perPage, userId);
        }

        // Assemble DTOs (data is already joined)
        List<MessageHistoryContactDTO> dtos = messages.stream()
                .map(this::assembleDTOOptimized)
                .toList();

        // Get total count
        long totalCount = repository.countTotalContactsOptimized(
                userId, search, filter, fromDate, toDate
        );

        log.info("Step 2: Count query completed in {}ms",
                System.currentTimeMillis() - startTime);

        // Get channel info
        List<MessageHistoryWrapperResponse.ChannelInfo> channels = getChannelInfo(userId);

        // Build pagination
        MessageHistoryPageResponse pageResponse = buildPaginationResponse(
                dtos, page, perPage, totalCount
        );

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== OPTIMIZED MessageHistoryService END - Total time: {}ms (improvement vs old approach) ===",
                totalTime);

        return MessageHistoryWrapperResponse.builder()
                .users(pageResponse)
                .channel(channels)
                .build();
    }

    /**
     * Assemble DTO from single row (all data already joined)
     */
    private MessageHistoryContactDTO assembleDTOOptimized(Map<String, Object> msg) {
        Long contactId = ((Number) msg.get("contact_id")).longValue();

        // Contact info
        ContactInfo contact = ContactInfo.builder()
                .id(contactId)
                .name((String) msg.get("cc_name"))
                .mobile((String) msg.get("cc_mobile"))
                .email((String) msg.get("cc_email"))
                .countryId((String) msg.get("cc_country_id"))
                .build();

        // Chat info (if exists)
        ChatInfo chat = null;
        if (msg.get("chat_id") != null && msg.get("chat_text") != null) {
            chat = ChatInfo.builder()
                    .text((String) msg.get("chat_text"))
                    .type((String) msg.get("chat_type"))
                    .time((String) msg.get("chat_time"))
                    .status((String) msg.get("chat_status"))
                    .build();
        }

        // Report info (if exists)
        ReportInfo report = null;
        if (msg.get("report_id_val") != null && msg.get("report_status") != null) {
            report = ReportInfo.builder()
                    .id(((Number) msg.get("report_id_val")).longValue())
                    .status((String) msg.get("report_status"))
                    .build();
        }

        // Extract metadata (already in the row)
        Integer unreadCount = ((Number) msg.get("unread_count")).intValue();
        Object lastChatTimeObj = msg.get("last_chat_time");
        Long lastChatTime = lastChatTimeObj != null ? ((Number) lastChatTimeObj).longValue() : null;

        return MessageHistoryContactDTO.builder()
                .id(((Number) msg.get("id")).longValue())
                .contactId(contactId)
                .contact(contact)
                .chat(chat)
                .report(report)
                .unreadCount(unreadCount)
                .lastChatTime(lastChatTime)
                .createdAt((LocalDateTime) msg.get("created_at"))
                .build();
    }

    private MessageHistoryWrapperResponse buildEmptyResponse(int page, int perPage, Long userId) {
        MessageHistoryPageResponse pageResponse = MessageHistoryPageResponse.builder()
                .currentPage(page)
                .data(Collections.emptyList())
                .firstPageUrl("https://aigreentick.com/api/v1/get-messages-history?page=1")
                .from(null)
                .lastPage(0)
                .lastPageUrl("https://aigreentick.com/api/v1/get-messages-history?page=0")
                .links(Collections.emptyList())
                .nextPageUrl(null)
                .path("https://aigreentick.com/api/v1/get-messages-history")
                .perPage(perPage)
                .prevPageUrl(null)
                .to(null)
                .total(0L)
                .build();

        return MessageHistoryWrapperResponse.builder()
                .users(pageResponse)
                .channel(getChannelInfo(userId))
                .build();
    }

    private MessageHistoryPageResponse buildPaginationResponse(
            List<MessageHistoryContactDTO> data,
            int page,
            int perPage,
            long total
    ) {
        int lastPage = (int) Math.ceil((double) total / perPage);
        String baseUrl = "https://aigreentick.com/api/v1/get-messages-history";

        List<PageLink> links = new ArrayList<>();

        // Previous link
        links.add(PageLink.builder()
                .url(page > 1 ? baseUrl + "?page=" + (page - 1) : null)
                .label("&laquo; Previous")
                .active(false)
                .build());

        // Page number links (show up to 10 pages)
        int startPage = Math.max(1, page - 4);
        int endPage = Math.min(lastPage, page + 5);

        if (startPage > 1) {
            links.add(PageLink.builder()
                    .url(baseUrl + "?page=1")
                    .label("1")
                    .active(false)
                    .build());

            if (startPage > 2) {
                links.add(PageLink.builder()
                        .url(null)
                        .label("...")
                        .active(false)
                        .build());
            }
        }

        for (int i = startPage; i <= endPage; i++) {
            links.add(PageLink.builder()
                    .url(baseUrl + "?page=" + i)
                    .label(String.valueOf(i))
                    .active(i == page)
                    .build());
        }

        if (endPage < lastPage) {
            if (endPage < lastPage - 1) {
                links.add(PageLink.builder()
                        .url(null)
                        .label("...")
                        .active(false)
                        .build());
            }

            links.add(PageLink.builder()
                    .url(baseUrl + "?page=" + lastPage)
                    .label(String.valueOf(lastPage))
                    .active(false)
                    .build());
        }

        // Next link
        links.add(PageLink.builder()
                .url(page < lastPage ? baseUrl + "?page=" + (page + 1) : null)
                .label("Next &raquo;")
                .active(false)
                .build());

        return MessageHistoryPageResponse.builder()
                .currentPage(page)
                .data(data)
                .firstPageUrl(baseUrl + "?page=1")
                .from(data.isEmpty() ? null : (page - 1) * perPage + 1)
                .lastPage(lastPage)
                .lastPageUrl(baseUrl + "?page=" + lastPage)
                .links(links)
                .nextPageUrl(page < lastPage ? baseUrl + "?page=" + (page + 1) : null)
                .path(baseUrl)
                .perPage(perPage)
                .prevPageUrl(page > 1 ? baseUrl + "?page=" + (page - 1) : null)
                .to(data.isEmpty() ? null : (page - 1) * perPage + data.size())
                .total(total)
                .build();
    }

    private List<MessageHistoryWrapperResponse.ChannelInfo> getChannelInfo(Long userId) {
        String sql = """
            SELECT
                id, user_id, created_by, whatsapp_no, whatsapp_no_id,
                whatsapp_biz_id, parmenent_token, token, status, response,
                created_at, updated_at, deleted_at
            FROM whatsapp_accounts
            WHERE user_id = ?
        """;

        return jdbcTemplate.query(sql, new Object[]{userId}, (rs, rowNum) ->
                MessageHistoryWrapperResponse.ChannelInfo.builder()
                        .id(rs.getLong("id"))
                        .userId(rs.getLong("user_id"))
                        .createdBy(rs.getLong("created_by"))
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
                        .build()
        );
    }
}