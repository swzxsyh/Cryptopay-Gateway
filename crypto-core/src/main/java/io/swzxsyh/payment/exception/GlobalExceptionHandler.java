package io.swzxsyh.payment.exception;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.api.dto.ApiResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

/** 全局异常拦截器，将各类异常统一转换为 ApiResponse JSON 返回给前端。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 处理业务异常（含收银台访问异常），使用异常携带的返回码与 HTTP 状态。 */
  @ExceptionHandler(BizException.class)
  public ResponseEntity<ApiResponse<Void>> handleBizException(BizException e) {
    log.warn("Biz rejected: {} - {}", e.getCode(), e.getMessage());
    return ResponseEntity.status(e.getStatus()).body(ApiResponse.fail(e.getCode(), e.getMessage()));
  }

  /** 处理常见非法参数/状态异常。 */
  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(RuntimeException e) {
    log.warn("Request rejected: {}", e.getMessage());
    return ResponseEntity.badRequest()
        .body(ApiResponse.fail(ApiResponseCode.BAD_REQUEST, e.getMessage()));
  }

  /** 处理资源或路由未找到，返回统一 JSON 错误响应。 */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.fail(ApiResponseCode.NOT_FOUND));
  }

  /** 处理数据库唯一约束冲突异常，返回统一 JSON 错误响应。 */
  @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
  public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(Exception e) {
    log.error("Database constraint violation", e);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.fail(ApiResponseCode.CONFLICT));
  }

  /** 处理未预期异常。 */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception e) {
    log.error("Unexpected server error", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.fail(ApiResponseCode.INTERNAL_ERROR));
  }
}