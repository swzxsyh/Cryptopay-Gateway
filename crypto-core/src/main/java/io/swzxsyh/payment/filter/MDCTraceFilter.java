package io.swzxsyh.payment.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** 请求级 traceId 过滤器，用于日志关联。 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCTraceFilter extends OncePerRequestFilter {

  public static final String TRACE_ID_KEY = "requestId";
  private static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = resolveTraceId(request);
    MDC.put(TRACE_ID_KEY, traceId);
    response.setHeader(TRACE_ID_KEY, traceId);
    response.setHeader(REQUEST_ID_HEADER, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(TRACE_ID_KEY);
    }
  }

  private String resolveTraceId(HttpServletRequest request) {
    String traceId = firstNonBlank(
        request.getHeader(TRACE_ID_KEY),
        request.getHeader(REQUEST_ID_HEADER),
        request.getParameter(TRACE_ID_KEY));
    return !StringUtils.hasText(traceId) ? newTraceId() : traceId;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  private String newTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
