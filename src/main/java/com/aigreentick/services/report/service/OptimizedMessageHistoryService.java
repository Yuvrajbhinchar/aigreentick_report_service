package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.MessageHistoryDTO.*;
import com.aigreentick.services.report.entity.WhatsappAccount;
import com.aigreentick.services.report.repository.OptimizedMessageHistoryRepository;
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
public class OptimizedMessageHistoryService {

    private final OptimizedMessageHistoryRepository repository;
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
        log.info("=== OptimizedMessageHistoryService START - User: {}, Page: {}, PerPage: {} ===",
                userId, page, perPage);

        int offset = (page - 1) * perPage;

        // STEP 1: Get latest messages with contact info and metadata (FAST)
        List<Map<String, Object>> messages = repository.getLatestMessagesWithMetadata(
                userId, search, filter, fromDate, toDate, perPage, offset
        );

        log.info("Step 1 completed: {} messages found in {}ms",
                messages.size(), System.currentTimeMillis() - startTime);

        // STEP 2: Extract IDs for batch fetching
        List<Long> chatIds = messages.stream()
                .map(m -> m.get("chat_id"))
                .filter(Objects::nonNull)
                .map(id -> ((Number) id).longValue())
                .collect(Collectors.toList());

        List<Long> reportIds = messages.stream()
                .map(m -> m.get("report_id"))
                .filter(Objects::nonNull)
                .map(id -> ((Number) id).longValue())
                .collect(Collectors.toList());

        // STEP 3: Batch fetch chats and reports (2 FAST queries instead of N queries)
        long batchStartTime = System.currentTimeMillis();
        Map<Long, Map<String, Object>> chatsMap = repository.getChatsForIds(chatIds);
        Map<Long, Map<String, Object>> reportsMap = repository.getReportsForIds(reportIds);

        log.info("Step 2-3 completed: Batch fetched {} chats and {} reports in {}ms",
                chatsMap.size(), reportsMap.size(), System.currentTimeMillis() - batchStartTime);

        // STEP 4: Assemble DTOs (in-memory - microseconds)
        List<MessageHistoryContactDTO> dtos = messages.stream()
                .map(msg -> assembleDTO(msg, chatsMap, reportsMap))
                .collect(Collectors.toList());

        // STEP 5: Get total count for pagination
        long totalCount = repository.countTotalMessages(userId, search, filter, fromDate, toDate);

        // STEP 6: Get channel info
        List<MessageHistoryWrapperResponse.ChannelInfo> channels = getChannelInfo(userId);

        // STEP 7: Build pagination response
        MessageHistoryPageResponse pageResponse = buildPaginationResponse(
                dtos, page, perPage, totalCount
        );

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== OptimizedMessageHistoryService END - Total time: {}ms ===", totalTime);

        return MessageHistoryWrapperResponse.builder()
                .users(pageResponse)
                .channel(channels)
                .build();
    }

    private MessageHistoryContactDTO assembleDTO(
            Map<String, Object> msg,
            Map<Long, Map<String, Object>> chatsMap,
            Map<Long, Map<String, Object>> reportsMap
    ) {
        // Contact info
                ContactInfo contact = ContactInfo.builder()
                .id(((Number) msg.get("contact_id")).longValue())
                .name((String) msg.get("cc_name"))
                .mobile((String) msg.get("cc_mobile"))
                .email((String) msg.get("cc_email"))
                .countryId((String) msg.get("cc_country_id"))
                .build();

        // Chat info (if exists)
            ChatInfo chat = null;
        if (msg.get("chat_id") != null) {
            Long chatId = ((Number) msg.get("chat_id")).longValue();
            Map<String, Object> chatData = chatsMap.get(chatId);
            if (chatData != null) {
                chat = ChatInfo.builder()
                        .text((String) chatData.get("text"))
                        .type((String) chatData.get("type"))
                        .time((String) chatData.get("time"))
                        .status((String) chatData.get("status"))
                        .build();
            }
        }

        // Report info (if exists)
            ReportInfo report = null;
        if (msg.get("report_id") != null) {
            Long reportId = ((Number) msg.get("report_id")).longValue();
            Map<String, Object> reportData = reportsMap.get(reportId);
            if (reportData != null) {
                report = ReportInfo.builder()
                        .id(reportId)
                        .status((String) reportData.get("status"))
                        .build();
            }
        }

        return MessageHistoryContactDTO.builder()
                .id(((Number) msg.get("id")).longValue())
                .contactId(((Number) msg.get("contact_id")).longValue())
                .contact(contact)
                .chat(chat)
                .report(report)
                .unreadCount((Integer) msg.get("unread_count"))
                .lastChatTime(msg.get("last_chat_time") != null ?
                        ((Number) msg.get("last_chat_time")).longValue() : null)
                .createdAt((LocalDateTime) msg.get("created_at"))
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

        // Page number links (show max 10 pages)
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
                id,
                user_id,
                created_by,
                whatsapp_no,
                whatsapp_no_id,
                whatsapp_biz_id,
                parmenent_token,
                token,
                status,
                response,
                created_at,
                updated_at,
                deleted_at
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