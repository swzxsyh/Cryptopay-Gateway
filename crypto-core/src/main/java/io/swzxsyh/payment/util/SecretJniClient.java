package io.swzxsyh.payment.util;

/** JNI 本地密钥解密/读取抽象。 */
public interface SecretJniClient {

  /** 按密钥标识和密文调用本地库解密或读取密钥。 */
  String resolve(String keyId, String encryptedValue);
}
