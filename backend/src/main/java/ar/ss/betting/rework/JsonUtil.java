package ar.ss.betting.rework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Small JSON helper for persistence + API mapping.
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() { }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    /**
     * Parses a JSON string into a generic Java object (Map/List/String/Number/...).
     * This serializes back to clean JSON in Spring responses.
     */
    public static Object parseJsonToObject(String json) {
        try {
            return MAPPER.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse JSON", e);
        }
    }
}