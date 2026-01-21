package com.aigreentick.services.report.service;


import com.aigreentick.services.report.dto.MessageHistoryDTO.*;
import com.aigreentick.services.report.repository.PhpStyleMessageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhpStyleMessageHistoryService {

    private final PhpStyleMessageHistoryRepository repository;
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
        log.info("=== PHP-Style Service START - User: {}, Page: {}, PerPage: {} ===", userId, page, perPage);

        int offset = (page - 1) * perPage;

        // STEP 1: Get only contact IDs (FAST - minimal data)
        List<Long> contactIds = repository.getLatestContactIds(
                userId, search, filter, fromDate, toDate, perPage, offset
        );

        log.info("Step 1: Got {} contact IDs in {}ms",
                contactIds.size(), System.currentTimeMillis() - startTime);

        if (contactIds.isEmpty()) {
            return buildEmptyResponse(page, perPage, userId);
        }

        // STEP 2: Get full message data for these contacts only (FAST - indexed)
        long step2Start = System.currentTimeMillis();
        List<Map<String, Object>> messages = repository.getMessagesForContacts(userId, contactIds);
        log.info("Step 2: Got messages in {}ms", System.currentTimeMillis() - step2Start);

        // STEP 3: Get unread counts ONLY for these contacts (FAST - small dataset)
        long step3Start = System.currentTimeMillis();
        Map<Long, Integer> unreadCounts = Collections.emptyMap();
        Map<Long, Long> lastChatTimes = Collections.emptyMap();

        if (filter == null || !filter.equals("none")) {
            unreadCounts = repository.getUnreadCountsForContacts(contactIds);
            lastChatTimes = repository.getLastChatTimesForContacts(contactIds);
        }
        log.info("Step 3: Got metadata in {}ms", System.currentTimeMillis() - step3Start);

        // STEP 4: Apply filter AFTER fetching (like PHP does)
        List<Map<String, Object>> filteredMessages = messages;
        if ("unread".equalsIgnoreCase(filter)) {
            Map<Long, Integer> finalUnreadCounts = unreadCounts;
            filteredMessages = messages.stream()
                    .filter(m -> {
                        Long contactId = ((Number) m.get("contact_id")).longValue();
                        return finalUnreadCounts.getOrDefault(contactId, 0) > 0;
                    })
                    .collect(Collectors.toList());
        } else if ("active".equalsIgnoreCase(filter)) {
            long activeSince = (System.currentTimeMillis() / 1000) - 86400;
            Map<Long, Long> finalLastChatTimes = lastChatTimes;
            filteredMessages = messages.stream()
                    .filter(m -> {
                        Long contactId = ((Number) m.get("contact_id")).longValue();
                        Long lastTime = finalLastChatTimes.get(contactId);
                        return lastTime != null && lastTime >= activeSince;
                    })
                    .collect(Collectors.toList());
        }

        // STEP 5: Assemble DTOs
        Map<Long, Integer> finalUnreadCounts = unreadCounts;
        Map<Long, Long> finalLastChatTimes = lastChatTimes;

        List<MessageHistoryContactDTO> dtos = filteredMessages.stream()
                .map(msg -> assembleDTO(msg, finalUnreadCounts, finalLastChatTimes))
                .collect(Collectors.toList());

        // STEP 6: Get total count
        long totalCount = repository.countTotalContacts(userId, search, fromDate, toDate);

        // STEP 7: Get channel info
        List<MessageHistoryWrapperResponse.ChannelInfo> channels = getChannelInfo(userId);

        // STEP 8: Build pagination
        MessageHistoryPageResponse pageResponse = buildPaginationResponse(
                dtos, page, perPage, totalCount
        );

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== PHP-Style Service END - Total time: {}ms ===", totalTime);

        return MessageHistoryWrapperResponse.builder()
                .users(pageResponse)
                .channel(channels)
                .build();
    }

    private MessageHistoryContactDTO assembleDTO(
            Map<String, Object> msg,
            Map<Long, Integer> unreadCounts,
            Map<Long, Long> lastChatTimes
    ) {
        Long contactId = ((Number) msg.get("contact_id")).longValue();

        // Contact info
                ContactInfo contact = ContactInfo.builder()
                .id(contactId)
                .name((String) msg.get("cc_name"))
                .mobile((String) msg.get("cc_mobile"))
                .email((String) msg.get("cc_email"))
                .countryId((String) msg.get("cc_country_id"))
                .build();

        // Chat info
        ChatInfo chat = null;
        if (msg.get("chat_id") != null && msg.get("chat_text") != null) {
            chat =  ChatInfo.builder()
                    .text((String) msg.get("chat_text"))
                    .type((String) msg.get("chat_type"))
                    .time((String) msg.get("chat_time"))
                    .status((String) msg.get("chat_status"))
                    .build();
        }

        // Report info
            ReportInfo report = null;
        if (msg.get("report_id") != null && msg.get("report_status") != null) {
            report = ReportInfo.builder()
                    .id(((Number) msg.get("report_id")).longValue())
                    .status((String) msg.get("report_status"))
                    .build();
        }

        return MessageHistoryContactDTO.builder()
                .id(((Number) msg.get("id")).longValue())
                .contactId(contactId)
                .contact(contact)
                .chat(chat)
                .report(report)
                .unreadCount(unreadCounts.getOrDefault(contactId, 0))
                .lastChatTime(lastChatTimes.get(contactId))
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

        // Page number links
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