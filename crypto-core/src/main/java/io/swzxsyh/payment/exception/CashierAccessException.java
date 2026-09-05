package io.swzxsyh.payment.exception;

import io.swzxsyh.payment.api.dto.ApiResponseCode;
import org.springframework.http.HttpStatus;

/** 收银台访问异常。 */
public class CashierAccessException extends BizException {

  public CashierAccessException(ApiResponseCode responseCode, HttpStatus status, String message) {
    super(responseCode, status, message);
  }

  public CashierAccessException(int code, HttpStatus status, String message) {
    super(code, status, message);
  }
}