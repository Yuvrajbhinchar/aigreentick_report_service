package com.aigreentick.services.report.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            // Use JSON format (handles all cases including commas in values)
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting list to JSON string", e);
            return null;
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Try JSON first (handles array format)
        if (dbData.trim().startsWith("[")) {
            try {
                return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse as JSON array, trying comma-separated", e);
            }
        }

        // Fallback: treat as comma-separated (for backward compatibility)
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}

/*
 * ALTERNATIVE: Simple Comma-Separated Converter (if you prefer)
 *
 * @Override
 * public String convertToDatabaseColumn(List<String> attribute) {
 *     return attribute == null ? null : String.join(",", attribute);
 * }
 *
 * @Override
 * public List<String> convertToEntityAttribute(String dbData) {
 *     if (dbData == null || dbData.trim().isEmpty()) {
 *         return new ArrayList<>();
 *     }
 *     return Arrays.asList(dbData.split(","));
 * }
 */