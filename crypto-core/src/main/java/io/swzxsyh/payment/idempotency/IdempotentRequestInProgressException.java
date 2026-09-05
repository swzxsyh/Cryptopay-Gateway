package io.swzxsyh.payment.idempotency;

import io.swzxsyh.payment.api.dto.ApiResponseCode;
import io.swzxsyh.payment.exception.BizException;
import org.springframework.http.HttpStatus;

/** 表示同一个幂等请求仍在处理中，调用方可以稍后用同一幂等键重试。 */
public class IdempotentRequestInProgressException extends BizException {

  public IdempotentRequestInProgressException() {
    super(
        ApiResponseCode.REQUEST_IN_PROGRESS,
        HttpStatus.TOO_EARLY,
        "Idempotent request is still processing");
  }
}
