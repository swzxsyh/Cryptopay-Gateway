package io.swzxsyh.payment.signature;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** RSA 签名与验签工具。 */
public final class SignatureUtil {

  private SignatureUtil() {
  }

  public static String signRsaSha256(String content, String privateKeyPem) {
    try {
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initSign(parsePrivateKey(privateKeyPem));
      signature.update(content.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(signature.sign());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to sign payload", e);
    }
  }

  public static boolean verifyRsaSha256(String content, String signatureValue, String publicKeyPem) {
    try {
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initVerify(parsePublicKey(publicKeyPem));
      signature.update(content.getBytes(StandardCharsets.UTF_8));
      return signature.verify(Base64.getDecoder().decode(signatureValue));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to verify signature", e);
    }
  }

  private static PrivateKey parsePrivateKey(String pem) throws Exception {
    String normalized = normalizePem(pem);
    byte[] decoded = Base64.getDecoder().decode(normalized);
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
  }

  private static PublicKey parsePublicKey(String pem) throws Exception {
    String normalized = normalizePem(pem);
    byte[] decoded = Base64.getDecoder().decode(normalized);
    return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
  }

  private static String normalizePem(String pem) {
    if (pem == null) {
      throw new IllegalArgumentException("key pem is required");
    }
    return pem.replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s+", "");
  }
}
