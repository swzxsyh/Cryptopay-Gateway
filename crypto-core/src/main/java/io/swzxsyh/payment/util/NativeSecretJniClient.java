package io.swzxsyh.payment.util;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 默认 JNI 密钥读取器。
 *
 * <p>该实现只在 `crypto.payment.secrets.jni.enabled=true` 时注册。它要求你提前把 Rust/Go/C
 * 编译成 Java 可加载的本地库，例如 Windows 下的 `.dll`。resources 目录里的源码不会被 JVM 自动执行。
 */
@Component
@ConditionalOnProperty(prefix = "crypto.payment.secrets.jni", name = "enabled", havingValue = "true")
public class NativeSecretJniClient implements SecretJniClient {

  private final CryptoPaymentProperties properties;
  private final AtomicBoolean loaded = new AtomicBoolean(false);

  public NativeSecretJniClient(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  @Override
  public String resolve(String keyId, String encryptedValue) {
    if (!StringUtils.hasText(keyId)) {
      throw new IllegalArgumentException("jni secret keyId is required");
    }
    loadLibraryIfNecessary();
    try {
      return nativeResolve(keyId.trim(), encryptedValue == null ? "" : encryptedValue);
    } catch (UnsatisfiedLinkError ex) {
      throw new IllegalStateException("JNI secret native method is not linked: nativeResolve", ex);
    }
  }

  private void loadLibraryIfNecessary() {
    if (loaded.get()) {
      return;
    }
    CryptoPaymentProperties.Jni jni = properties.getSecrets().getJni();
    synchronized (loaded) {
      if (loaded.get()) {
        return;
      }
      try {
        if (jni != null && StringUtils.hasText(jni.getLibraryPath())) {
          System.load(jni.getLibraryPath().trim());
        } else if (jni != null && StringUtils.hasText(jni.getLibraryName())) {
          System.loadLibrary(jni.getLibraryName().trim());
        } else {
          throw new IllegalStateException(
              "crypto.payment.secrets.jni.library-path or library-name is required");
        }
        loaded.set(true);
      } catch (UnsatisfiedLinkError ex) {
        throw new IllegalStateException("Failed to load JNI secret library", ex);
      }
    }
  }

  private native String nativeResolve(String keyId, String encryptedValue);
}
