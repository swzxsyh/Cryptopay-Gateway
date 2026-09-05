package io.swzxsyh.payment.util;

import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 运行时密钥解析器，支持 ENV / YAML / KMS / JNI 四种来源。 */
@Component
public class SecretValueResolver {

  private final ObjectProvider<SecretKmsClient> kmsClientProvider;
  private final ObjectProvider<SecretJniClient> jniClientProvider;

  public SecretValueResolver(
      ObjectProvider<SecretKmsClient> kmsClientProvider,
      ObjectProvider<SecretJniClient> jniClientProvider) {
    this.kmsClientProvider = kmsClientProvider;
    this.jniClientProvider = jniClientProvider;
  }

  public String resolveRequired(
      SecretSourceType sourceType,
      String configuredValue,
      String configuredEnvName,
      String kmsKeyId,
      String defaultEnvName,
      String fieldName) {
    String value = resolve(sourceType, configuredValue, configuredEnvName, kmsKeyId, defaultEnvName);
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException(fieldName + " is not configured");
    }
    return value;
  }

  public String resolveOptional(
      SecretSourceType sourceType,
      String configuredValue,
      String configuredEnvName,
      String kmsKeyId,
      String defaultEnvName) {
    return resolve(sourceType, configuredValue, configuredEnvName, kmsKeyId, defaultEnvName);
  }

  public String resolveRequired(
      String configuredEnvName, String fallbackValue, String defaultEnvName, String fieldName) {
    return resolveRequired(SecretSourceType.ENV, fallbackValue, configuredEnvName, null, defaultEnvName, fieldName);
  }

  public String resolveOptional(String configuredEnvName, String fallbackValue, String defaultEnvName) {
    return resolveOptional(SecretSourceType.ENV, fallbackValue, configuredEnvName, null, defaultEnvName);
  }

  private String resolve(
      SecretSourceType sourceType,
      String configuredValue,
      String configuredEnvName,
      String kmsKeyId,
      String defaultEnvName) {
    SecretSourceType resolvedType = Objects.requireNonNullElse(sourceType, SecretSourceType.ENV);
    return switch (resolvedType) {
      case YAML -> configuredValue;
      case KMS -> resolveFromKms(kmsKeyId, configuredValue);
      case JNI -> resolveFromJni(kmsKeyId, configuredValue);
      case ENV -> resolveFromEnv(configuredEnvName, configuredValue, defaultEnvName);
    };
  }

  private String resolveFromEnv(String configuredEnvName, String fallbackValue, String defaultEnvName) {
    String envName = StringUtils.hasText(configuredEnvName) ? configuredEnvName : defaultEnvName;
    if (StringUtils.hasText(envName)) {
      String value = System.getenv(envName);
      if (!StringUtils.hasText(value)) {
        value = System.getProperty(envName);
      }
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return fallbackValue;
  }

  private String resolveFromKms(String kmsKeyId, String fallbackValue) {
    if (!StringUtils.hasText(kmsKeyId)) {
      return fallbackValue;
    }
    SecretKmsClient client = kmsClientProvider.getIfAvailable();
    if (client == null) {
      throw new IllegalStateException("KMS secret resolver is not configured");
    }
    return client.resolve(kmsKeyId);
  }

  private String resolveFromJni(String keyId, String encryptedValue) {
    if (!StringUtils.hasText(keyId)) {
      throw new IllegalStateException("JNI secret keyId is required");
    }
    SecretJniClient client = jniClientProvider.getIfAvailable();
    if (client == null) {
      throw new IllegalStateException("JNI secret resolver is not configured");
    }
    return client.resolve(keyId, encryptedValue);
  }
}
