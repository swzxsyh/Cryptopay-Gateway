package io.swzxsyh.manager.security.util;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.HmacHashFunction;
import com.warrenstrange.googleauth.KeyRepresentation;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.util.StringUtils;

/** Google Authenticator TOTP 工具，供登录二次验证和敏感操作二次确认复用。 */
public final class GoogleAuthenticatorUtil {

  private static final int CODE_DIGITS = 6;
  private static final int TIME_STEP_SECONDS = 30;
  private static final int DEFAULT_WINDOW_SIZE = 3;

  private static final GoogleAuthenticator AUTHENTICATOR =
      new GoogleAuthenticator(
          new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
              .setCodeDigits(CODE_DIGITS)
              .setTimeStepSizeInMillis(TIME_STEP_SECONDS * 1000L)
              .setWindowSize(DEFAULT_WINDOW_SIZE)
              .setHmacHashFunction(HmacHashFunction.HmacSHA1)
              .setKeyRepresentation(KeyRepresentation.BASE32)
              .build());

  private GoogleAuthenticatorUtil() {}

  /** 生成绑定密钥；secretKey 需要保存到数据库或 KMS，不能明文返回给普通页面长期暴露。 */
  public static BindingSecret generateSecret() {
    GoogleAuthenticatorKey key = AUTHENTICATOR.createCredentials();
    return new BindingSecret(key.getKey(), key.getVerificationCode(), key.getScratchCodes());
  }

  /** 校验用户输入的 6 位动态验证码。 */
  public static boolean verifyCode(String secretKey, String code) {
    if (!StringUtils.hasText(secretKey) || !StringUtils.hasText(code)) {
      return false;
    }
    String normalizedCode = code.trim();
    if (!normalizedCode.matches("\\d{" + CODE_DIGITS + "}")) {
      return false;
    }
    return AUTHENTICATOR.authorize(secretKey.trim(), Integer.parseInt(normalizedCode));
  }

  /** 构造 otpauth URI，可用于二维码生成；issuer 会显示为 App/平台名称。 */
  public static String buildOtpAuthUri(String issuer, String accountName, String secretKey) {
    if (!StringUtils.hasText(issuer)
        || !StringUtils.hasText(accountName)
        || !StringUtils.hasText(secretKey)) {
      throw new IllegalArgumentException("issuer, accountName and secretKey are required");
    }
    String encodedIssuer = encode(issuer.trim());
    String encodedAccount = encode(accountName.trim());
    return "otpauth://totp/"
        + encodedIssuer
        + ":"
        + encodedAccount
        + "?secret="
        + encode(secretKey.trim())
        + "&issuer="
        + encodedIssuer
        + "&algorithm=SHA1"
        + "&digits="
        + CODE_DIGITS
        + "&period="
        + TIME_STEP_SECONDS;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /** Google Authenticator 绑定信息。 */
  public record BindingSecret(
      String secretKey,
      int verificationCode,
      List<Integer> scratchCodes) {}
}
