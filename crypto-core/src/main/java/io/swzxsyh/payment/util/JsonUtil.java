package io.swzxsyh.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/** JSON 读写工具。 */
public final class JsonUtil {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

  private static final ObjectMapper CANONICAL_OBJECT_MAPPER = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

  private JsonUtil() {
  }

  public static String toJson(Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize JSON", e);
    }
  }

  /** 输出字段顺序稳定的 JSON，适用于签名、幂等键和事件去重哈希。 */
  public static String toCanonicalJson(Object value) {
    try {
      return CANONICAL_OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize canonical JSON", e);
    }
  }

  public static byte[] toBytes(Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsBytes(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize JSON", e);
    }
  }

  public static <T> T fromJson(String json, Class<T> type) {
    try {
      return OBJECT_MAPPER.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to deserialize JSON", e);
    }
  }

  public static <T> T fromJson(byte[] json, Class<T> type) {
    try {
      return OBJECT_MAPPER.readValue(json, type);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to deserialize JSON", e);
    }
  }

  public static JsonNode readTree(String json) {
    try {
      return OBJECT_MAPPER.readTree(json);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse JSON tree", e);
    }
  }

  public static JsonNode toTree(Object value) {
    return OBJECT_MAPPER.valueToTree(value);
  }

  public static ObjectNode createObjectNode() {
    return OBJECT_MAPPER.createObjectNode();
  }

  public static void writePrettyJson(Path path, Object value) {
    writePrettyJson(path.toFile(), value);
  }

  public static void writePrettyJson(File file, Object value) {
    try {
      OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, value);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write pretty JSON", e);
    }
  }
}
