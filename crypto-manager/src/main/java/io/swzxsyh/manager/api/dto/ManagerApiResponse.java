package io.swzxsyh.manager.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ManagerApiResponse<T>(int code, String msg, T data) {

  public static <T> ManagerApiResponse<T> ok(T data) {
    return new ManagerApiResponse<>(
        ManagerApiResponseCode.SUCCESS.getCode(), ManagerApiResponseCode.SUCCESS.getMessage(), data);
  }

  public static ManagerApiResponse<Void> fail(ManagerApiResponseCode responseCode) {
    return new ManagerApiResponse<>(responseCode.getCode(), responseCode.getMessage(), null);
  }

  public static ManagerApiResponse<Void> fail(ManagerApiResponseCode responseCode, String msg) {
    return new ManagerApiResponse<>(responseCode.getCode(), msg, null);
  }
}
