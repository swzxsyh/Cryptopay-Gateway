package io.swzxsyh.payment.exception;

import io.swzxsyh.payment.api.dto.ApiResponseCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 通用业务异常基类，携带返回码、HTTP 状态与错误消息。 */
@Getter
public class BizException extends RuntimeException {

  private final int code;
  private final HttpStatus status;

  public BizException(String message) {
    this(ApiResponseCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
  }

  public BizException(ApiResponseCode responseCode, HttpStatus status, String message) {
    this(responseCode.getCode(), status, message);
  }

  public BizException(int code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }
}