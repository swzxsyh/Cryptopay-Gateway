package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerApiResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "io.swzxsyh.manager")
public class ManagerExceptionHandler {

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  /** 处理管理端参数错误和业务状态错误。 */
  public ResponseEntity<ManagerApiResponse<Void>> handleBadRequest(RuntimeException e) {
    return ResponseEntity.badRequest()
        .body(ManagerApiResponse.fail(ManagerApiResponseCode.BAD_REQUEST, e.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  /** 处理管理端权限不足异常。 */
  public ResponseEntity<ManagerApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ManagerApiResponse.fail(ManagerApiResponseCode.FORBIDDEN, "Access denied"));
  }

  @ExceptionHandler(Exception.class)
  /** 处理管理端未预期异常。 */
  public ResponseEntity<ManagerApiResponse<Void>> handleUnknown(Exception e) {
    log.error("Manager request failed", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ManagerApiResponse.fail(ManagerApiResponseCode.INTERNAL_ERROR));
  }
}
