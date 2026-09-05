package io.swzxsyh.payment.util;

/** KMS 密钥读取抽象。 */
public interface SecretKmsClient {

  /** 按 KMS keyId 读取密钥内容。 */
  String resolve(String keyId);
}
