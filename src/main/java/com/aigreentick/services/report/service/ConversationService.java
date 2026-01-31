package com.aigreentick.services.report.service;

import com.aigreentick.services.report.repository.ConversationRepository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ULTRA-OPTIMIZED V2:
 * - Separate count query (no window function blocking)
 * - Connection pool monitoring
 * - Cached channel info with TTL
 * - Complete pagination matching PHP format
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final HikariDataSource dataSource;

    // Cache for channel info (rarely changes)
    private static final Map<Integer, CacheEntry<List<Map<String, Object>>>> channelCache = new ConcurrentHashMap<>();
    private static final long CHANNEL_CACHE_TTL = 5 * 60 * 1000; // 5 minutes

    /**
     * MAIN API: Get inbox with complete pagination and channel info
     * Matches PHP response structure exactly
     */
    public Map<String, Object> getInbox(
            Integer userId,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size
    ) {
        long startTime = System.currentTimeMillis();

        // Log connection pool status BEFORE query
        logConnectionPoolStatus("INBOX_START", userId);

        log.info("=== ConversationService getInbox START - User: {}, Page: {}, Size: {}, Filter: {} ===",
                userId, page, size, filter);

        int offset = page * size;

        try {
            // OPTIMIZED: Separate data + count queries (no window function)
            ConversationRepository.ConversationResult result =
                    repository.fetchContactsOptimized(userId, search, filter, fromDate, toDate, offset, size);

            List<Map<String, Object>> contacts = result.getData();
            long total = result.getTotalCount();

            if (contacts.isEmpty()) {
                log.info("=== No contacts found - returning empty response in {}ms ===",
                        System.currentTimeMillis() - startTime);
                return buildEmptyResponse(page, size, userId);
            }

            // Get cached channel info (no database call if cached)
            List<Map<String, Object>> channels = getCachedChannelInfo(userId);

            // Build complete pagination response matching PHP
            Map<String, Object> usersData = buildPaginationResponse(contacts, page, size, total);

            long totalTime = System.currentTimeMillis() - startTime;

            // Log connection pool status AFTER query
            logConnectionPoolStatus("INBOX_END", userId);

            log.info("=== ConversationService getInbox END - Total time: {}ms, Records: {}, Total: {} ===",
                    totalTime, contacts.size(), total);

            return Map.of(
                    "users", usersData,
                    "channel", channels
            );

        } catch (Exception e) {
            log.error("Error in getInbox for user {}: {}", userId, e.getMessage(), e);
            logConnectionPoolStatus("INBOX_ERROR", userId);
            throw e;
        }
    }

    /**
     * Build complete pagination response matching PHP format exactly
     */
    private Map<String, Object> buildPaginationResponse(
            List<Map<String, Object>> data,
            int page,
            int size,
            long total
    ) {
        int currentPage = page + 1; // Convert 0-based to 1-based
        int lastPage = (int) Math.ceil((double) total / size);
        int from = data.isEmpty() ? 0 : (page * size) + 1;
        int to = data.isEmpty() ? 0 : (page * size) + data.size();

        String baseUrl = "https://aigreentick.com/api/v1/sendMessage";

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("current_page", currentPage);
        pagination.put("data", data);
        pagination.put("first_page_url", baseUrl + "?page=1");
        pagination.put("from", from);
        pagination.put("last_page", lastPage);
        pagination.put("last_page_url", baseUrl + "?page=" + lastPage);
        pagination.put("links", buildPaginationLinks(currentPage, lastPage, baseUrl));
        pagination.put("next_page_url", currentPage < lastPage ? baseUrl + "?page=" + (currentPage + 1) : null);
        pagination.put("path", baseUrl);
        pagination.put("per_page", size);
        pagination.put("prev_page_url", currentPage > 1 ? baseUrl + "?page=" + (currentPage - 1) : null);
        pagination.put("to", to);
        pagination.put("total", total);

        return pagination;
    }

    /**
     * Build pagination links matching Laravel's format
     */
    private List<Map<String, Object>> buildPaginationLinks(int currentPage, int lastPage, String baseUrl) {
        List<Map<String, Object>> links = new ArrayList<>();

        // Previous link
        Map<String, Object> prevLink = new LinkedHashMap<>();
        prevLink.put("url", currentPage > 1 ? baseUrl + "?page=" + (currentPage - 1) : null);
        prevLink.put("label", "&laquo; Previous");
        prevLink.put("active", false);
        links.add(prevLink);

        // Determine page range to show (max 10 pages)
        int maxPagesToShow = 10;
        int startPage = Math.max(1, currentPage - 4);
        int endPage = Math.min(lastPage, startPage + maxPagesToShow - 1);

        // Adjust start if we're near the end
        if (endPage - startPage < maxPagesToShow - 1) {
            startPage = Math.max(1, endPage - maxPagesToShow + 1);
        }

        // First page
        if (startPage > 1) {
            Map<String, Object> firstLink = new LinkedHashMap<>();
            firstLink.put("url", baseUrl + "?page=1");
            firstLink.put("label", "1");
            firstLink.put("active", false);
            links.add(firstLink);

            if (startPage > 2) {
                Map<String, Object> dots = new LinkedHashMap<>();
                dots.put("url", null);
                dots.put("label", "...");
                dots.put("active", false);
                links.add(dots);
            }
        }

        // Page numbers
        for (int i = startPage; i <= endPage; i++) {
            Map<String, Object> pageLink = new LinkedHashMap<>();
            pageLink.put("url", baseUrl + "?page=" + i);
            pageLink.put("label", String.valueOf(i));
            pageLink.put("active", i == currentPage);
            links.add(pageLink);
        }

        // Last page
        if (endPage < lastPage) {
            if (endPage < lastPage - 1) {
                Map<String, Object> dots = new LinkedHashMap<>();
                dots.put("url", null);
                dots.put("label", "...");
                dots.put("active", false);
                links.add(dots);
            }

            Map<String, Object> lastLink = new LinkedHashMap<>();
            lastLink.put("url", baseUrl + "?page=" + lastPage);
            lastLink.put("label", String.valueOf(lastPage));
            lastLink.put("active", false);
            links.add(lastLink);
        }

        // Next link
        Map<String, Object> nextLink = new LinkedHashMap<>();
        nextLink.put("url", currentPage < lastPage ? baseUrl + "?page=" + (currentPage + 1) : null);
        nextLink.put("label", "Next &raquo;");
        nextLink.put("active", false);
        links.add(nextLink);

        return links;
    }

    /**
     * OPTIMIZATION: Cached channel info with TTL
     */
    private List<Map<String, Object>> getCachedChannelInfo(Integer userId) {
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
     * Fetch channel info from database
     */
    private List<Map<String, Object>> fetchChannelInfo(Integer userId) {
        String sql = """
            SELECT
                id, user_id, created_by, whatsapp_no, whatsapp_no_id,
                whatsapp_biz_id, parmenent_token, token, status, response,
                created_at, updated_at, deleted_at
            FROM whatsapp_accounts
            WHERE user_id = ?
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
            return channel;
        });
    }

    /**
     * Format timestamp matching PHP format
     */
    private String formatTimestamp(java.sql.Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().toString().replace("T", " ");
    }

    /**
     * Build empty response structure
     */
    private Map<String, Object> buildEmptyResponse(int page, int size, Integer userId) {
        Map<String, Object> users = new LinkedHashMap<>();
        users.put("current_page", page + 1);
        users.put("data", Collections.emptyList());
        users.put("first_page_url", "https://aigreentick.com/api/v1/sendMessage?page=1");
        users.put("from", null);
        users.put("last_page", 0);
        users.put("last_page_url", "https://aigreentick.com/api/v1/sendMessage?page=0");
        users.put("links", Collections.emptyList());
        users.put("next_page_url", null);
        users.put("path", "https://aigreentick.com/api/v1/sendMessage");
        users.put("per_page", size);
        users.put("prev_page_url", null);
        users.put("to", null);
        users.put("total", 0L);

        return Map.of(
                "users", users,
                "channel", getCachedChannelInfo(userId)
        );
    }

    /**
     * CONNECTION POOL MONITORING: Log pool status for debugging
     */
    private void logConnectionPoolStatus(String stage, Integer userId) {
        try {
            int active = dataSource.getHikariPoolMXBean().getActiveConnections();
            int idle = dataSource.getHikariPoolMXBean().getIdleConnections();
            int waiting = dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
            int total = dataSource.getHikariPoolMXBean().getTotalConnections();

            if (waiting > 0 || active > 15) {
                log.warn("🔵 CONVERSATION {} [User: {}] - Active: {}/{}, Idle: {}, Waiting: {} ⚠️",
                        stage, userId, active, total, idle, waiting);
            } else {
                log.debug("🔵 CONVERSATION {} [User: {}] - Active: {}/{}, Idle: {}, Waiting: {}",
                        stage, userId, active, total, idle, waiting);
            }
        } catch (Exception e) {
            // Ignore if monitoring fails
            log.trace("Could not read connection pool metrics: {}", e.getMessage());
        }
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
    public static void invalidateChannelCache(Integer userId) {
        channelCache.remove(userId);
    }

    /**
     * Clear all channel cache (call on application refresh or manual trigger)
     */
    public static void clearAllCache() {
        channelCache.clear();
    }
}