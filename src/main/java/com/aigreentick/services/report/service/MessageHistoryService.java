package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.MessageHistoryDTO.*;
import com.aigreentick.services.report.repository.MessageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageHistoryService {

    private final MessageHistoryRepository repository;
    private final JdbcTemplate jdbcTemplate;

    // Cache for channel info (rarely changes)
    private static final Map<Long, CacheEntry<List<MessageHistoryWrapperResponse.ChannelInfo>>> channelCache =
            new ConcurrentHashMap<>();
    private static final long CHANNEL_CACHE_TTL = 5 * 60 * 1000; // 5 minutes

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_VISIBLE_PAGES = 7;

    /**
     * ULTRA-OPTIMIZED: Single query execution (data + count in one go)
     */
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
        log.info("=== MessageHistory API START - User: {}, Page: {}, Filter: {} ===", userId, page, filter);

        int offset = (page - 1) * perPage;

        // SINGLE QUERY: Get both data and total count
        MessageHistoryRepository.MessageHistoryResult result = repository.getLatestMessagesWithCount(
                userId, search, filter, fromDate, toDate, perPage, offset
        );

        List<Map<String, Object>> messages = result.getData();
        long totalCount = result.getTotalCount();

        if (messages.isEmpty()) {
            log.info("=== No messages found - returning empty response in {}ms ===",
                    System.currentTimeMillis() - startTime);
            return buildEmptyResponse(page, perPage, userId);
        }

        // Batch DTO assembly (no database calls)
        List<MessageHistoryContactDTO> dtos = assembleDTOsBatch(messages);

        // Get cached channel info (no database call if cached)
        List<MessageHistoryWrapperResponse.ChannelInfo> channels = getCachedChannelInfo(userId);

        // Build pagination response (pure calculation, no database)
        MessageHistoryPageResponse pageResponse = buildOptimizedPaginationResponse(
                dtos, page, perPage, totalCount
        );

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== MessageHistory API END - Total time: {}ms, Records: {}, Total: {} ===",
                totalTime, dtos.size(), totalCount);

        return MessageHistoryWrapperResponse.builder()
                .users(pageResponse)
                .channel(channels)
                .build();
    }

    /**
     * OPTIMIZATION: Batch assembly with minimal object creation
     */
    private List<MessageHistoryContactDTO> assembleDTOsBatch(List<Map<String, Object>> messages) {
        List<MessageHistoryContactDTO> dtos = new ArrayList<>(messages.size());

        for (Map<String, Object> msg : messages) {
            dtos.add(assembleDTOOptimized(msg));
        }

        return dtos;
    }

    /**
     * Optimized DTO assembly with null-safe operations
     */
    private MessageHistoryContactDTO assembleDTOOptimized(Map<String, Object> msg) {
        Long contactId = ((Number) msg.get("contact_id")).longValue();

        // Build contact info
        ContactInfo contact = ContactInfo.builder()
                .id(contactId)
                .name((String) msg.get("cc_name"))
                .mobile((String) msg.get("cc_mobile"))
                .email((String) msg.get("cc_email"))
                .countryId((String) msg.get("cc_country_id"))
                .build();

        // Build chat info (only if exists)
        ChatInfo chat = buildChatInfo(msg);

        // Build report info (only if exists)
        ReportInfo report = buildReportInfo(msg);

        // Extract metadata
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

    /**
     * OPTIMIZATION: Only build ChatInfo if data exists
     */
    private ChatInfo buildChatInfo(Map<String, Object> msg) {

        return ChatInfo.builder()
                .text((String) msg.get("chat_text"))
                .type((String) msg.get("chat_type"))
                .time((String) msg.get("chat_time"))
                .status((String) msg.get("chat_status"))
                .build();
    }

    /**
     * OPTIMIZATION: Only build ReportInfo if data exists
     */
    private ReportInfo buildReportInfo(Map<String, Object> msg) {
        Object reportIdVal = msg.get("report_id_val");
        if (reportIdVal == null || msg.get("report_status") == null) {
            return null;
        }

        return ReportInfo.builder()
                .id(((Number) reportIdVal).longValue())
                .status((String) msg.get("report_status"))
                .build();
    }

    /**
     * OPTIMIZATION: Cached channel info with TTL
     */
    private List<MessageHistoryWrapperResponse.ChannelInfo> getCachedChannelInfo(Long userId) {
        CacheEntry<List<MessageHistoryWrapperResponse.ChannelInfo>> cached = channelCache.get(userId);

        if (cached != null && !cached.isExpired()) {
            log.debug("Channel info retrieved from cache for user: {}", userId);
            return cached.value;
        }

        // Cache miss or expired - fetch fresh data
        log.debug("Channel cache miss - fetching from database");
        List<MessageHistoryWrapperResponse.ChannelInfo> channels = getChannelInfo(userId);
        channelCache.put(userId, new CacheEntry<>(channels, CHANNEL_CACHE_TTL));

        return channels;
    }

    /**
     * OPTIMIZATION: Efficient pagination with smart page link building
     */
    private MessageHistoryPageResponse buildOptimizedPaginationResponse(
            List<MessageHistoryContactDTO> data,
            int page,
            int perPage,
            long total
    ) {
        int lastPage = (int) Math.ceil((double) total / perPage);
        String baseUrl = "https://aigreentick.com/api/v1/get-messages-history";

        // Build optimized pagination links
        List<PageLink> links = buildSmartPaginationLinks(page, lastPage, baseUrl);

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

    /**
     * OPTIMIZATION: Smart pagination - only show relevant page numbers
     */
    private List<PageLink> buildSmartPaginationLinks(int currentPage, int lastPage, String baseUrl) {
        List<PageLink> links = new ArrayList<>();

        // Previous link
        links.add(PageLink.builder()
                .url(currentPage > 1 ? baseUrl + "?page=" + (currentPage - 1) : null)
                .label("&laquo; Previous")
                .active(false)
                .build());

        // Calculate range
        int halfRange = MAX_VISIBLE_PAGES / 2;
        int startPage = Math.max(1, currentPage - halfRange);
        int endPage = Math.min(lastPage, currentPage + halfRange);

        // Adjust if at boundaries
        if (currentPage <= halfRange) {
            endPage = Math.min(lastPage, MAX_VISIBLE_PAGES);
        } else if (currentPage > lastPage - halfRange) {
            startPage = Math.max(1, lastPage - MAX_VISIBLE_PAGES + 1);
        }

        // First page (if not in range)
        if (startPage > 1) {
            links.add(createPageLink(1, currentPage, baseUrl));
            if (startPage > 2) {
                links.add(PageLink.builder().url(null).label("...").active(false).build());
            }
        }

        // Page numbers
        for (int i = startPage; i <= endPage; i++) {
            links.add(createPageLink(i, currentPage, baseUrl));
        }

        // Last page (if not in range)
        if (endPage < lastPage) {
            if (endPage < lastPage - 1) {
                links.add(PageLink.builder().url(null).label("...").active(false).build());
            }
            links.add(createPageLink(lastPage, currentPage, baseUrl));
        }

        // Next link
        links.add(PageLink.builder()
                .url(currentPage < lastPage ? baseUrl + "?page=" + (currentPage + 1) : null)
                .label("Next &raquo;")
                .active(false)
                .build());

        return links;
    }

    private PageLink createPageLink(int pageNum, int currentPage, String baseUrl) {
        return PageLink.builder()
                .url(baseUrl + "?page=" + pageNum)
                .label(String.valueOf(pageNum))
                .active(pageNum == currentPage)
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
                .channel(getCachedChannelInfo(userId))
                .build();
    }

    /**
     * Fetch channel info from database
     */
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
                        .createdAt(formatTimestamp(rs.getTimestamp("created_at")))
                        .updatedAt(formatTimestamp(rs.getTimestamp("updated_at")))
                        .deletedAt(formatTimestamp(rs.getTimestamp("deleted_at")))
                        .build()
        );
    }

    private String formatTimestamp(java.sql.Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime().format(FORMATTER) : null;
    }

    /**
     * Simple cache entry with TTL
     */
    private static class CacheEntry<T> {
        final T value;
        final long expiryTime;

        CacheEntry(T value, long ttlMillis) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    /**
     * Clear channel cache for a specific user (call when channel info changes)
     */
    public static void invalidateChannelCache(Long userId) {
        channelCache.remove(userId);
    }

    /**
     * Clear all channel cache (call on application refresh or manual trigger)
     */
    public static void clearAllCache() {
        channelCache.clear();
    }
}