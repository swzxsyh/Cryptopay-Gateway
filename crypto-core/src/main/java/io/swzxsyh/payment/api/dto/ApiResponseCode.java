package io.swzxsyh.payment.api.dto;

/**
 * 统一业务返回码。
 *
 * <p>前端可通过统一拦截器根据 {@code code} 做全局处理，例如：
 * <ul>
 *   <li>403 跳转登录或补齐凭证</li>
 *   <li>404 提示资源不存在</li>
 *   <li>410 提示订单已失效（过期 / 已支付 / 已取消）</li>
 *   <li>500 提示服务异常</li>
 * </ul>
 */
public enum ApiResponseCode {

  /** 成功。 */
  SUCCESS(0, "OK"),

  /** 请求参数错误或业务校验失败。 */
  BAD_REQUEST(400, "Bad request"),

  /** 未认证或缺少访问凭证。 */
  UNAUTHORIZED(403, "Unauthorized"),

  /** 资源或路由不存在。 */
  NOT_FOUND(404, "Not found"),

  /** 资源已失效（如收银台订单过期、不可支付）。 */
  GONE(410, "Resource gone"),

  /** 资源冲突（如重复提交、重复支付）。 */
  CONFLICT(409, "Conflict"),

  /** 幂等请求仍在处理中，调用方可稍后重试。 */
  REQUEST_IN_PROGRESS(425, "Request in progress"),

  /** 服务端内部错误。 */
  INTERNAL_ERROR(500, "Internal server error");

  private final int code;
  private final String message;

  ApiResponseCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  /** 根据整型码查找对应枚举，未命中时返回 {@link #INTERNAL_ERROR}。 */
  public static ApiResponseCode of(int code) {
    for (ApiResponseCode value : values()) {
      if (value.code == code) {
        return value;
      }
    }
    return INTERNAL_ERROR;
  }
}
