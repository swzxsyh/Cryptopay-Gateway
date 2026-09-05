package io.swzxsyh.payment.chain.solana;

import io.swzxsyh.payment.util.HttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.util.StringUtils;

/** 最小化 Solana JSON-RPC 客户端。 */
public class SolanaJsonRpcClient {

  private static final AtomicLong REQUEST_ID = new AtomicLong(1L);

  private final URI endpoint;
  private final HttpClient httpClient;
  private final HttpClientUtil httpClientUtil;
  private final ObjectMapper objectMapper;

  public SolanaJsonRpcClient(String rpcUrl, ObjectMapper objectMapper) {
    this(rpcUrl, objectMapper, null);
  }

  public SolanaJsonRpcClient(String rpcUrl, ObjectMapper objectMapper, HttpClientUtil httpClientUtil) {
    if (!StringUtils.hasText(rpcUrl)) {
      throw new IllegalArgumentException("rpcUrl is required");
    }
    this.endpoint = URI.create(rpcUrl);
    this.objectMapper = objectMapper;
    this.httpClientUtil = httpClientUtil;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
  }

  public JsonNode call(String method, JsonNode params) {
    try {
      ObjectNode request = objectMapper.createObjectNode();
      request.put("jsonrpc", "2.0");
      request.put("id", REQUEST_ID.getAndIncrement());
      request.put("method", method);
      if (params != null) {
        request.set("params", params);
      } else {
        request.set("params", objectMapper.createArrayNode());
      }
      if (httpClientUtil != null) {
        HttpClientUtil.ExternalHttpResponse response =
            httpClientUtil.postJson(endpoint.toString(), request, Map.of("Content-Type", "application/json"));
        JsonNode body = objectMapper.readTree(response.body());
        if (body.hasNonNull("error")) {
          throw new IllegalStateException("Solana RPC error: " + body.get("error").toString());
        }
        return body.path("result");
      }
      HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
          .build();
      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      JsonNode body = objectMapper.readTree(response.body());
      if (body.hasNonNull("error")) {
        throw new IllegalStateException("Solana RPC error: " + body.get("error").toString());
      }
      return body.path("result");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Failed to call Solana RPC method: " + method, e);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to call Solana RPC method: " + method, e);
    }
  }

  public long getSlot() {
    return call("getSlot", objectMapper.createArrayNode()).asLong();
  }

  public long getBalanceLamports(String address) {
    ArrayNode params = objectMapper.createArrayNode();
    params.add(address);
    params.add(objectMapper.createObjectNode());
    JsonNode result = call("getBalance", params);
    return result.path("value").asLong();
  }

  public ArrayNode getSignaturesForAddress(String address, int limit, String before) {
    ArrayNode params = objectMapper.createArrayNode();
    params.add(address);
    ObjectNode options = objectMapper.createObjectNode();
    options.put("limit", Math.max(1, Math.min(limit, 1000)));
    options.put("commitment", "confirmed");
    if (StringUtils.hasText(before)) {
      options.put("before", before);
    }
    params.add(options);
    JsonNode result = call("getSignaturesForAddress", params);
    return result != null && result.isArray() ? (ArrayNode) result : objectMapper.createArrayNode();
  }

  public JsonNode getTransaction(String signature) {
    ArrayNode params = objectMapper.createArrayNode();
    params.add(signature);
    ObjectNode options = objectMapper.createObjectNode();
    options.put("encoding", "jsonParsed");
    options.put("commitment", "confirmed");
    options.put("maxSupportedTransactionVersion", 0);
    params.add(options);
    return call("getTransaction", params);
  }

  public LatestBlockhash getLatestBlockhash() {
    ArrayNode params = objectMapper.createArrayNode();
    ObjectNode options = objectMapper.createObjectNode();
    options.put("commitment", "confirmed");
    params.add(options);
    JsonNode value = call("getLatestBlockhash", params).path("value");
    return new LatestBlockhash(
        value.path("blockhash").asText(""),
        value.path("lastValidBlockHeight").asLong(0L));
  }

  public long getFeeForMessage(String messageBase64) {
    if (!StringUtils.hasText(messageBase64)) {
      return 0L;
    }
    // 提前做一次 Base64 校验，避免把明显无效内容打到 RPC 节点。
    Base64.getDecoder().decode(messageBase64);
    ArrayNode params = objectMapper.createArrayNode();
    params.add(messageBase64);
    ObjectNode options = objectMapper.createObjectNode();
    options.put("commitment", "confirmed");
    params.add(options);
    JsonNode value = call("getFeeForMessage", params).path("value");
    return value.isNumber() ? value.asLong() : 0L;
  }

  public String sendRawTransaction(String signedTransactionBase64) {
    if (!StringUtils.hasText(signedTransactionBase64)) {
      throw new IllegalArgumentException("signedTransactionBase64 is required");
    }
    Base64.getDecoder().decode(signedTransactionBase64);
    ArrayNode params = objectMapper.createArrayNode();
    params.add(signedTransactionBase64);
    ObjectNode options = objectMapper.createObjectNode();
    options.put("encoding", "base64");
    options.put("skipPreflight", false);
    options.put("maxRetries", 3);
    params.add(options);
    return call("sendTransaction", params).asText("");
  }

  public JsonNode simulateTransaction(String signedOrUnsignedTransactionBase64) {
    if (!StringUtils.hasText(signedOrUnsignedTransactionBase64)) {
      throw new IllegalArgumentException("transaction base64 is required");
    }
    Base64.getDecoder().decode(signedOrUnsignedTransactionBase64);
    ArrayNode params = objectMapper.createArrayNode();
    params.add(signedOrUnsignedTransactionBase64);
    ObjectNode options = objectMapper.createObjectNode();
    options.put("encoding", "base64");
    options.put("sigVerify", false);
    options.put("replaceRecentBlockhash", true);
    params.add(options);
    return call("simulateTransaction", params);
  }

  public ArrayNode getRecentPrioritizationFees(String... addresses) {
    ArrayNode params = objectMapper.createArrayNode();
    ArrayNode addressArray = objectMapper.createArrayNode();
    if (addresses != null) {
      for (String address : addresses) {
        if (StringUtils.hasText(address)) {
          addressArray.add(address);
        }
      }
    }
    params.add(addressArray);
    JsonNode result = call("getRecentPrioritizationFees", params);
    return result != null && result.isArray() ? (ArrayNode) result : objectMapper.createArrayNode();
  }

  /** Solana 最近 blockhash 响应。 */
  public record LatestBlockhash(String blockhash, long lastValidBlockHeight) {}
}
