package com.aigreentick.services.report.service;

import com.aigreentick.services.report.repository.ChatPaginationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ULTRA-OPTIMIZED Chat Pagination Service
 * - Single query execution (data + count in one query)
 * - Matches PHP response format exactly (Laravel pagination)
 * - Sub-second response time
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPaginationService {

    private final ChatPaginationRepository repository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Cache for channel info (rarely changes)
    private static final Map<Long, CacheEntry<List<Map<String, Object>>>> channelCache = new ConcurrentHashMap<>();
    private static final long CHANNEL_CACHE_TTL = 5 * 60 * 1000; // 5 minutes

    /**
     * Get chats with pagination matching PHP response structure
     * Returns Laravel-style pagination + channel info
     */
    public Map<String, Object> getChats(
            Long userId,
            Long contactId,
            int page,
            int perPage,
            String search,
            String fromDate,
            String toDate
    ) {
        long startTime = System.currentTimeMillis();
        log.info("=== ChatPagination API START - User: {}, Contact: {}, Page: {} ===", userId, contactId, page);

        // Parse date strings
        LocalDateTime fromDateTime = parseDate(fromDate);
        LocalDateTime toDateTime = parseDate(toDate);

        int offset = page * perPage;

        // SINGLE QUERY: Get messages + total count
        ChatPaginationRepository.ChatMessageResult result = repository.getChatsOptimized(
                userId, contactId, search, fromDateTime, toDateTime, perPage, offset
        );

        List<Map<String, Object>> messages = result.getData();
        long totalElements = result.getTotalCount();

        // Process messages (decode JSON fields, clean arrays)
        List<Map<String, Object>> processedMessages = processMessages(messages);

        // Build Laravel pagination response matching PHP exactly
        Map<String, Object> pageData = buildLaravelPageResponse(
                processedMessages, page, perPage, totalElements
        );

        // Get cached channel info
        List<Map<String, Object>> channels = getCachedChannelInfo(userId);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== ChatPagination API END - Time: {}ms, Records: {}, Total: {} ===",
                totalTime, messages.size(), totalElements);

        return Map.of(
                "messages", pageData,
                "channel", channels
        );
    }

    /**
     * Process messages: decode JSON, clean arrays
     */
    private List<Map<String, Object>> processMessages(List<Map<String, Object>> messages) {
        List<Map<String, Object>> processed = new ArrayList<>(messages.size());

        for (Map<String, Object> msg : messages) {
            Map<String, Object> processedMsg = new LinkedHashMap<>(msg);

            // Decode payload
            processedMsg.put("payload", decodeJson(msg.get("payload")));

            // Decode response
            processedMsg.put("response", decodeJson(msg.get("response")));

            // Decode and clean buttons
            processedMsg.put("buttons", cleanJsonArray(msg.get("buttons")));

            // Decode and clean chat_buttons
            processedMsg.put("chat_buttons", cleanJsonArray(msg.get("chat_buttons")));

            // Decode and clean carousel_cards
            List<Map<String, Object>> carouselCards = cleanJsonArray(msg.get("carousel_cards"));
            if (carouselCards != null && !carouselCards.isEmpty()) {
                // Clean buttons inside each carousel card
                for (Map<String, Object> card : carouselCards) {
                    if (card.get("buttons") != null) {
                        List<Map<String, Object>> cardButtons = cleanJsonArray(card.get("buttons"));
                        card.put("buttons", cardButtons != null ? cardButtons : Collections.emptyList());
                    } else {
                        card.put("buttons", Collections.emptyList());
                    }
                }
            }
            processedMsg.put("carousel_cards", carouselCards != null ? carouselCards : Collections.emptyList());

            // Format created_at
            if (msg.get("created_at") instanceof LocalDateTime) {
                processedMsg.put("created_at", ((LocalDateTime) msg.get("created_at")).format(FORMATTER));
            }

            processed.add(processedMsg);
        }

        return processed;
    }

    /**
     * Decode JSON string to object
     */
    private Object decodeJson(Object json) {
        if (json == null || !(json instanceof String)) {
            return null;
        }

        try {
            return objectMapper.readValue((String) json, Object.class);
        } catch (Exception e) {
            log.debug("Failed to decode JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Clean JSON array: decode and filter empty objects
     */
    private List<Map<String, Object>> cleanJsonArray(Object json) {
        if (json == null) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> array;
            if (json instanceof String) {
                array = objectMapper.readValue((String) json, new TypeReference<>() {});
            } else {
                return Collections.emptyList();
            }

            if (array == null) {
                return Collections.emptyList();
            }

            // Filter out empty objects (all values are null)
            List<Map<String, Object>> cleaned = new ArrayList<>();
            for (Map<String, Object> item : array) {
                boolean hasNonNullValue = item.values().stream().anyMatch(Objects::nonNull);
                if (hasNonNullValue) {
                    cleaned.add(item);
                }
            }

            return cleaned;
        } catch (Exception e) {
            log.debug("Failed to decode JSON array: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Build Laravel pagination response structure matching PHP exactly
     */
    private Map<String, Object> buildLaravelPageResponse(
            List<Map<String, Object>> content,
            int page,
            int size,
            long totalElements
    ) {
        int currentPage = page + 1; // Convert 0-based to 1-based for display
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int from = content.isEmpty() ? 0 : (page * size) + 1;
        int to = content.isEmpty() ? 0 : (page * size) + content.size();

        String baseUrl = "https://aigreentick.com/api/v1/get-chat-with-pagination";

        Map<String, Object> pageData = new LinkedHashMap<>();

        // Laravel pagination structure
        pageData.put("current_page", currentPage);
        pageData.put("data", content);
        pageData.put("first_page_url", baseUrl + "?page=1");
        pageData.put("from", from > 0 ? from : null);
        pageData.put("last_page", totalPages);
        pageData.put("last_page_url", baseUrl + "?page=" + totalPages);
        pageData.put("links", buildLaravelPaginationLinks(currentPage, totalPages, baseUrl));
        pageData.put("next_page_url", currentPage < totalPages ? baseUrl + "?page=" + (currentPage + 1) : null);
        pageData.put("path", baseUrl);
        pageData.put("per_page", size);
        pageData.put("prev_page_url", currentPage > 1 ? baseUrl + "?page=" + (currentPage - 1) : null);
        pageData.put("to", to > 0 ? to : null);
        pageData.put("total", totalElements);

        return pageData;
    }

    /**
     * Build Laravel-style pagination links
     */
    private List<Map<String, Object>> buildLaravelPaginationLinks(int currentPage, int totalPages, String baseUrl) {
        List<Map<String, Object>> links = new ArrayList<>();

        // Previous link
        Map<String, Object> prevLink = new LinkedHashMap<>();
        prevLink.put("url", currentPage > 1 ? baseUrl + "?page=" + (currentPage - 1) : null);
        prevLink.put("label", "&laquo; Previous");
        prevLink.put("active", false);
        links.add(prevLink);

        // Determine which page numbers to show
        int maxVisiblePages = 10;
        int startPage = Math.max(1, currentPage - 4);
        int endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

        // Adjust start if we're near the end
        if (endPage - startPage < maxVisiblePages - 1) {
            startPage = Math.max(1, endPage - maxVisiblePages + 1);
        }

        // First pages (1 to startPage-1)
        for (int i = 1; i < startPage; i++) {
            Map<String, Object> pageLink = new LinkedHashMap<>();
            pageLink.put("url", baseUrl + "?page=" + i);
            pageLink.put("label", String.valueOf(i));
            pageLink.put("active", i == currentPage);
            links.add(pageLink);
        }

        // Ellipsis before if needed
        if (startPage > 1 && endPage < totalPages) {
            // Show ellipsis only if there are hidden pages
            if (startPage > 2) {
                Map<String, Object> ellipsis = new LinkedHashMap<>();
                ellipsis.put("url", null);
                ellipsis.put("label", "...");
                ellipsis.put("active", false);
                links.add(ellipsis);
            }
        }

        // Visible page range
        for (int i = startPage; i <= endPage; i++) {
            Map<String, Object> pageLink = new LinkedHashMap<>();
            pageLink.put("url", baseUrl + "?page=" + i);
            pageLink.put("label", String.valueOf(i));
            pageLink.put("active", i == currentPage);
            links.add(pageLink);
        }

        // Ellipsis after if needed
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                Map<String, Object> ellipsis = new LinkedHashMap<>();
                ellipsis.put("url", null);
                ellipsis.put("label", "...");
                ellipsis.put("active", false);
                links.add(ellipsis);
            }

            // Last page
            if (endPage < totalPages) {
                Map<String, Object> lastPageLink = new LinkedHashMap<>();
                lastPageLink.put("url", baseUrl + "?page=" + totalPages);
                lastPageLink.put("label", String.valueOf(totalPages));
                lastPageLink.put("active", totalPages == currentPage);
                links.add(lastPageLink);
            }
        }

        // Next link
        Map<String, Object> nextLink = new LinkedHashMap<>();
        nextLink.put("url", currentPage < totalPages ? baseUrl + "?page=" + (currentPage + 1) : null);
        nextLink.put("label", "Next &raquo;");
        nextLink.put("active", false);
        links.add(nextLink);

        return links;
    }

    /**
     * Parse date string to LocalDateTime
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            // Try parsing as LocalDateTime first
            return LocalDateTime.parse(dateStr);
        } catch (Exception e1) {
            try {
                // Try parsing as LocalDate and convert to start of day
                return java.time.LocalDate.parse(dateStr).atStartOfDay();
            } catch (Exception e2) {
                log.warn("Failed to parse date: {}", dateStr);
                return null;
            }
        }
    }

    /**
     * OPTIMIZATION: Cached channel info with TTL
     */
    private List<Map<String, Object>> getCachedChannelInfo(Long userId) {
        CacheEntry<List<Map<String, Object>>> cached = channelCache.get(userId);

        if (cached != null && !cached.isExpired()) {
            log.debug("Channel info retrieved from cache for user: {}", userId);
            return cached.value;
        }

        // Cache miss or expired - fetch fresh data
        log.debug("Channel cache miss - fetching from database");
        List<Map<String, Object>> channels = fetchChannelInfo(userId);
        channelCache.put(userId, new CacheEntry<>(channels, CHANNEL_CACHE_TTL));

        return channels;
    }

    /**
     * Fetch channel info from database with user details
     */
    private List<Map<String, Object>> fetchChannelInfo(Long userId) {
        String sql = """
            SELECT
                wa.id, wa.user_id, wa.created_by, wa.whatsapp_no, wa.whatsapp_no_id,
                wa.whatsapp_biz_id, wa.parmenent_token, wa.token, wa.status, wa.response,
                wa.created_at, wa.updated_at, wa.deleted_at,
                u.id as user_id, u.role_id, u.created_by as user_created_by, u.country_id,
                u.name, u.mobile, u.profile_photo, u.rememberToken, u.api_token, u.email,
                u.company_name, u.city, u.market_msg_charge, u.utilty_msg_charge, u.auth_msg_charge,
                u.balance, u.balance_enabled, u.online_status, u.agent_id, u.credit, u.debit,
                u.status as user_status, u.domain, u.logo, u.is_demo, u.demo_end,
                u.created_at as user_created_at, u.updated_at as user_updated_at, u.deleted_at as user_deleted_at,
                u.webhook_token, u.fcmAndroidToken, u.fcmIosToken, u.reset_token, u.reset_token_expires,
                u.account_admin_id
            FROM whatsapp_accounts wa
            INNER JOIN users u ON u.id = wa.user_id
            WHERE wa.user_id = ?
        """;

        return jdbcTemplate.query(sql, new Object[]{userId}, (rs, rowNum) -> {
            Map<String, Object> channel = new LinkedHashMap<>();
            channel.put("id", rs.getLong("id"));
            channel.put("user_id", rs.getLong("user_id"));
            channel.put("created_by", rs.getLong("created_by"));
            channel.put("whatsapp_no", rs.getString("whatsapp_no"));
            channel.put("whatsapp_no_id", rs.getString("whatsapp_no_id"));
            channel.put("whatsapp_biz_id", rs.getString("whatsapp_biz_id"));
            channel.put("parmenent_token", rs.getString("parmenent_token"));
            channel.put("token", rs.getString("token"));
            channel.put("status", rs.getString("status"));
            channel.put("response", rs.getString("response"));
            channel.put("created_at", formatTimestamp(rs.getTimestamp("created_at")));
            channel.put("updated_at", formatTimestamp(rs.getTimestamp("updated_at")));
            channel.put("deleted_at", formatTimestamp(rs.getTimestamp("deleted_at")));

            // User object
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", rs.getLong("user_id"));
            user.put("role_id", rs.getLong("role_id"));
            user.put("created_by", rs.getObject("user_created_by"));
            user.put("country_id", rs.getObject("country_id"));
            user.put("name", rs.getString("name"));
            user.put("mobile", rs.getString("mobile"));
            user.put("profile_photo", rs.getString("profile_photo"));
            user.put("rememberToken", rs.getString("rememberToken"));
            user.put("api_token", rs.getString("api_token"));
            user.put("email", rs.getString("email"));
            user.put("company_name", rs.getString("company_name"));
            user.put("city", rs.getString("city"));
            user.put("market_msg_charge", rs.getDouble("market_msg_charge"));
            user.put("utilty_msg_charge", rs.getDouble("utilty_msg_charge"));
            user.put("auth_msg_charge", rs.getDouble("auth_msg_charge"));
            user.put("balance", rs.getDouble("balance"));
            user.put("balance_enabled", rs.getInt("balance_enabled"));
            user.put("online_status", rs.getString("online_status"));
            user.put("agent_id", rs.getObject("agent_id"));
            user.put("credit", rs.getDouble("credit"));
            user.put("debit", rs.getDouble("debit"));
            user.put("status", rs.getString("user_status"));
            user.put("domain", rs.getString("domain"));
            user.put("logo", rs.getString("logo"));
            user.put("is_demo", rs.getString("is_demo"));
            user.put("demo_end", formatTimestamp(rs.getTimestamp("demo_end")));
            user.put("created_at", formatTimestamp(rs.getTimestamp("user_created_at")));
            user.put("updated_at", formatTimestamp(rs.getTimestamp("user_updated_at")));
            user.put("deleted_at", formatTimestamp(rs.getTimestamp("user_deleted_at")));
            user.put("webhook_token", rs.getString("webhook_token"));
            user.put("fcmAndroidToken", rs.getString("fcmAndroidToken"));
            user.put("fcmIosToken", rs.getString("fcmIosToken"));
            user.put("reset_token", rs.getString("reset_token"));
            user.put("reset_token_expires", formatTimestamp(rs.getTimestamp("reset_token_expires")));
            user.put("account_admin_id", rs.getObject("account_admin_id"));

            channel.put("user", user);

            return channel;
        });
    }

    /**
     * Format timestamp to ISO string with Z suffix
     */
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        LocalDateTime ldt = timestamp.toLocalDateTime();
        return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
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