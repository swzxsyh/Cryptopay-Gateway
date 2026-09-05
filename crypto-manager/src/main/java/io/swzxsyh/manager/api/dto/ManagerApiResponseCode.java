package io.swzxsyh.manager.api.dto;

public enum ManagerApiResponseCode {

  SUCCESS(0, "OK"),
  BAD_REQUEST(400, "Bad request"),
  UNAUTHORIZED(401, "Unauthorized"),
  FORBIDDEN(403, "Forbidden"),
  INTERNAL_ERROR(500, "Internal server error");

  private final int code;
  private final String message;

  ManagerApiResponseCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
