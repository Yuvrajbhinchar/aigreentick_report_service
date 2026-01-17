package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.chatHistoryDTO.*;
import com.aigreentick.services.report.entity.User;
import com.aigreentick.services.report.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public ChatHistoryDTO getMessagesHistory(
            Integer providedUserId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String search,
            Integer page,
            Integer perPage
    ) {
        log.info("===ChatHistoryService getMessagesHistory START {}", LocalDateTime.now().format(FORMATTER));

        // Get authenticated user (in real scenario, get from auth context)
        User user = userRepository.findById(providedUserId.longValue()).orElseThrow();
        Long userId = user.getRoleId() == 7 ? user.getCreatedBy().longValue() : user.getId();

        // Build main query
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("    cm.id AS cm_id, ");
        sql.append("    cm.contact_id, ");
        sql.append("    cm.chat_id, ");
        sql.append("    cm.report_id, ");
        sql.append("    cm.created_at AS cm_created_at, ");
        sql.append("    cc.name AS contact_name, ");
        sql.append("    cc.mobile AS contact_mobile, ");
        sql.append("    cc.email AS contact_email, ");
        sql.append("    cc.country_id AS contact_country_id, ");
        sql.append("    c.text AS chat_text, ");
        sql.append("    c.type AS chat_type, ");
        sql.append("    c.time AS chat_time, ");
        sql.append("    c.status AS chat_status, ");
        sql.append("    r.id AS report_id, ");
        sql.append("    r.status AS report_status, ");
        sql.append("    unread_tbl.unread_count, ");
        sql.append("    last_msg.last_chat_time ");
        sql.append("FROM contacts_messages cm ");
        sql.append("INNER JOIN ( ");
        sql.append("    SELECT contact_id, MAX(id) AS last_message_id ");
        sql.append("    FROM contacts_messages ");
        sql.append("    WHERE user_id = ? ");
        sql.append("    GROUP BY contact_id ");
        sql.append(") lm ON lm.last_message_id = cm.id ");
        sql.append("INNER JOIN chat_contacts cc ON cc.id = cm.contact_id ");
        sql.append("LEFT JOIN chats c ON c.id = cm.chat_id ");
        sql.append("LEFT JOIN reports r ON r.id = cm.report_id ");
        sql.append("LEFT JOIN ( ");
        sql.append("    SELECT contact_id, COUNT(*) AS unread_count ");
        sql.append("    FROM chats ");
        sql.append("    WHERE LOWER(TRIM(type)) = 'recieve' ");
        sql.append("    AND TRIM(status) = '0' ");
        sql.append("    GROUP BY contact_id ");
        sql.append(") unread_tbl ON unread_tbl.contact_id = cc.id ");
        sql.append("LEFT JOIN ( ");
        sql.append("    SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time ");
        sql.append("    FROM chats ");
        sql.append("    GROUP BY contact_id ");
        sql.append(") last_msg ON last_msg.contact_id = cc.id ");
        sql.append("WHERE cm.user_id = ? ");

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(userId);

        // Search filter
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (cc.mobile LIKE ? OR cc.name LIKE ?) ");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Date range filter
        if (fromDate != null && toDate != null) {
            sql.append("AND cm.created_at BETWEEN ? AND ? ");
            params.add(fromDate);
            params.add(toDate);
        }

        sql.append("ORDER BY cm.id DESC ");

        // Calculate offset
        int offset = (page - 1) * perPage;
        sql.append("LIMIT ? OFFSET ?");
        params.add(perPage);
        params.add(offset);

        // Execute main query
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        // Build conversations
        List<ConversationDTO> conversations = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            conversations.add(mapToConversationDTO(row));
        }

        // Count query for pagination
        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(DISTINCT cm.contact_id) ");
        countSql.append("FROM contacts_messages cm ");
        countSql.append("INNER JOIN ( ");
        countSql.append("    SELECT contact_id, MAX(id) AS last_message_id ");
        countSql.append("    FROM contacts_messages ");
        countSql.append("    WHERE user_id = ? ");
        countSql.append("    GROUP BY contact_id ");
        countSql.append(") lm ON lm.last_message_id = cm.id ");
        countSql.append("INNER JOIN chat_contacts cc ON cc.id = cm.contact_id ");
        countSql.append("WHERE cm.user_id = ? ");

        List<Object> countParams = new ArrayList<>();
        countParams.add(userId);
        countParams.add(userId);

        if (search != null && !search.trim().isEmpty()) {
            countSql.append("AND (cc.mobile LIKE ? OR cc.name LIKE ?) ");
            String searchPattern = "%" + search + "%";
            countParams.add(searchPattern);
            countParams.add(searchPattern);
        }

        if (fromDate != null && toDate != null) {
            countSql.append("AND cm.created_at BETWEEN ? AND ? ");
            countParams.add(fromDate);
            countParams.add(toDate);
        }

        Integer total = jdbcTemplate.queryForObject(countSql.toString(), countParams.toArray(), Integer.class);
        if (total == null) total = 0;

        // Fetch channel data
        List<ChannelDTO> channels = fetchChannelData(Math.toIntExact(userId));

        // Build pagination
        String apiPath = "https://aigreentick.com/api/v1/get-messages-history";
        int lastPage = (int) Math.ceil((double) total / perPage);
        int from = conversations.isEmpty() ? 0 : offset + 1;
        int to = conversations.isEmpty() ? 0 : offset + conversations.size();

        UsersPageDTO usersPage = UsersPageDTO.builder()
                .currentPage(page)
                .data(conversations)
                .firstPageUrl(apiPath + "?page=1")
                .from(from)
                .lastPage(lastPage)
                .lastPageUrl(apiPath + "?page=" + lastPage)
                .links(buildLinks(apiPath, page, lastPage))
                .nextPageUrl(page < lastPage ? apiPath + "?page=" + (page + 1) : null)
                .path(apiPath)
                .perPage(perPage)
                .prevPageUrl(page > 1 ? apiPath + "?page=" + (page - 1) : null)
                .to(to)
                .total(total)
                .build();

        log.info("===ChatHistoryService getMessagesHistory END {}", LocalDateTime.now().format(FORMATTER));

        return ChatHistoryDTO.builder()
                .users(usersPage)
                .channel(channels)
                .build();
    }

    private ConversationDTO mapToConversationDTO(Map<String, Object> row) {
        ContactDTO contactDTO = ContactDTO.builder()
                .id(getInteger(row, "contact_id"))
                .name(getString(row, "contact_name"))
                .mobile(getString(row, "contact_mobile"))
                .email(getString(row, "contact_email"))
                .countryId(getString(row, "contact_country_id"))
                .build();

        ChatDTO chatDTO = null;
        if (row.get("chat_text") != null) {
            chatDTO = ChatDTO.builder()
                    .text(getString(row, "chat_text"))
                    .type(getString(row, "chat_type"))
                    .time(getString(row, "chat_time"))
                    .status(getString(row, "chat_status"))
                    .build();
        }

        com.aigreentick.services.report.dto.chatHistoryDTO.ReportDTO reportDTO = null;
        if (row.get("report_id") != null) {
            reportDTO = com.aigreentick.services.report.dto.chatHistoryDTO.ReportDTO.builder()
                    .id(getLong(row, "report_id"))
                    .status(getString(row, "report_status"))
                    .build();
        }

        return ConversationDTO.builder()
                .id(getInteger(row, "cm_id"))
                .contactId(getInteger(row, "contact_id"))
                .contact(contactDTO)
                .chat(chatDTO)
                .report(reportDTO)
                .unreadCount(getInteger(row, "unread_count"))
                .lastChatTime(getLong(row, "last_chat_time"))
                .createdAt(getFormattedDateTime(row, "cm_created_at"))
                .build();
    }

    private List<ChannelDTO> fetchChannelData(Integer userId) {
        String sql = """
            SELECT id, user_id, created_by, whatsapp_no, whatsapp_no_id, 
                   whatsapp_biz_id, parmenent_token, token, status, response,
                   created_at, updated_at, deleted_at
            FROM whatsapp_accounts 
            WHERE user_id = ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        ChannelDTO.builder()
                                .id(rs.getLong("id"))
                                .userId(rs.getLong("user_id"))
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

        // Previous link
        links.add(LinkDTO.builder()
                .url(currentPage > 1 ? baseUrl + "?page=" + (currentPage - 1) : null)
                .label("&laquo; Previous")
                .active(false)
                .build());

        // Page number links
        int startPage = Math.max(1, currentPage - 4);
        int endPage = Math.min(totalPages, currentPage + 5);

        // First page
        if (startPage > 1) {
            links.add(LinkDTO.builder()
                    .url(baseUrl + "?page=1")
                    .label("1")
                    .active(currentPage == 1)
                    .build());
        }

        // Ellipsis before
        if (startPage > 2) {
            links.add(LinkDTO.builder()
                    .url(null)
                    .label("...")
                    .active(false)
                    .build());
        }

        // Page range
        for (int i = startPage; i <= endPage; i++) {
            links.add(LinkDTO.builder()
                    .url(baseUrl + "?page=" + i)
                    .label(String.valueOf(i))
                    .active(i == currentPage)
                    .build());
        }

        // Ellipsis after
        if (endPage < totalPages - 1) {
            links.add(LinkDTO.builder()
                    .url(null)
                    .label("...")
                    .active(false)
                    .build());
        }

        // Last page
        if (endPage < totalPages) {
            links.add(LinkDTO.builder()
                    .url(baseUrl + "?page=" + totalPages)
                    .label(String.valueOf(totalPages))
                    .active(currentPage == totalPages)
                    .build());
        }

        // Next link
        links.add(LinkDTO.builder()
                .url(currentPage < totalPages ? baseUrl + "?page=" + (currentPage + 1) : null)
                .label("Next &raquo;")
                .active(false)
                .build());

        return links;
    }

    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof BigInteger) return ((BigInteger) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private Long getLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof BigInteger) return ((BigInteger) value).longValue();
        return Long.parseLong(value.toString());
    }

    private String getFormattedDateTime(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;

        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().format(FORMATTER);
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(FORMATTER);
        }
        return value.toString();
    }
}