package io.swzxsyh.payment.util;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 外部 HTTP 调用工具，集中复用 OkHttp 单例连接池，降低频繁创建连接的资源消耗。 */
@Slf4j
@Component
public class HttpClientUtil {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private final OkHttpClient client;

  public HttpClientUtil(CryptoPaymentProperties properties) {
    CryptoPaymentProperties.ExternalHttp config = properties.getExternalHttp();
    this.client =
        new OkHttpClient.Builder()
            .connectionPool(
                new ConnectionPool(
                    Math.max(1, config.getMaxIdleConnections()),
                    Math.max(1, config.getKeepAliveSeconds()),
                    TimeUnit.SECONDS))
            .connectTimeout(Duration.ofMillis(Math.max(1, config.getConnectTimeoutMillis())))
            .readTimeout(Duration.ofMillis(Math.max(1, config.getReadTimeoutMillis())))
            .writeTimeout(Duration.ofMillis(Math.max(1, config.getWriteTimeoutMillis())))
            .callTimeout(Duration.ofMillis(Math.max(1, config.getCallTimeoutMillis())))
            .retryOnConnectionFailure(config.isRetryOnConnectionFailure())
            .protocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build();
    log.info(
        "外部 HTTP 单例客户端已初始化。maxIdleConnections={}, keepAliveSeconds={}, http2Enabled=true",
        config.getMaxIdleConnections(),
        config.getKeepAliveSeconds());
  }

  /** 发送 JSON POST 请求，返回状态码、响应体和响应头。 */
  public ExternalHttpResponse postJson(String url, Object body, Map<String, String> headers) {
    if (!StringUtils.hasText(url)) {
      throw new IllegalArgumentException("url is required");
    }
    String payload = body instanceof String stringBody ? stringBody : JsonUtil.toJson(body);
    Request.Builder builder =
        new Request.Builder()
            .url(url)
            .post(RequestBody.create(payload, JSON))
            .header("Content-Type", "application/json");
    if (headers != null) {
      headers.forEach(
          (name, value) -> {
            if (StringUtils.hasText(name) && value != null) {
              builder.header(name, value);
            }
          });
    }
    return execute(builder.build());
  }

  private ExternalHttpResponse execute(Request request) {
    try (Response response = client.newCall(request).execute()) {
      String body = response.body() == null ? "" : response.body().string();
      Map<String, String> headers = new LinkedHashMap<>();
      response.headers().forEach(pair -> headers.put(pair.getFirst(), pair.getSecond()));
      return new ExternalHttpResponse(response.code(), body, headers);
    } catch (IOException ex) {
      throw new IllegalStateException("HTTP request failed: " + request.url(), ex);
    }
  }

  /** 外部 HTTP 响应快照。 */
  public record ExternalHttpResponse(int statusCode, String body, Map<String, String> headers) {
    public boolean is2xxSuccessful() {
      return statusCode >= 200 && statusCode < 300;
    }
  }
}
