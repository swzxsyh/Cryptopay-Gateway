package io.swzxsyh.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    int code,
    String msg,
    T data
) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(ApiResponseCode.SUCCESS.getCode(), ApiResponseCode.SUCCESS.getMessage(), data);
  }

  public static <T> ApiResponse<T> ok(String msg, T data) {
    return new ApiResponse<>(ApiResponseCode.SUCCESS.getCode(), msg, data);
  }

  public static ApiResponse<Void> fail(int code, String msg) {
    return new ApiResponse<>(code, msg, null);
  }

  /** 使用统一返回码构造失败响应。 */
  public static ApiResponse<Void> fail(ApiResponseCode responseCode) {
    return new ApiResponse<>(responseCode.getCode(), responseCode.getMessage(), null);
  }

  /** 使用统一返回码构造失败响应，并覆盖默认提示信息。 */
  public static ApiResponse<Void> fail(ApiResponseCode responseCode, String msg) {
    return new ApiResponse<>(responseCode.getCode(), msg, null);
  }
}
