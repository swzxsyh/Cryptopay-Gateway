package io.swzxsyh.payment.channel.address;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swzxsyh.payment.util.JsonUtil;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 本地 keystore 产物写入器。 */
@Slf4j
@Component
public class LocalKeystoreArtifactWriter {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  public String write(String directory, String filePrefix, String address, BigInteger privateKey, String password) {
    if (!StringUtils.hasText(directory)) {
      throw new IllegalArgumentException("keystore directory is required");
    }
    try {
      Path dir = Path.of(directory);
      Files.createDirectories(dir);
      byte[] salt = randomBytes(16);
      byte[] iv = randomBytes(12);
      byte[] encrypted = encrypt(privateKey, password, salt, iv);

      ObjectNode node = JsonUtil.createObjectNode();
      node.put("address", address);
      node.put("createdAt", LocalDateTime.now().toString());
      node.put("cipher", "AES/GCM/NoPadding");
      node.put("salt", Base64.getEncoder().encodeToString(salt));
      node.put("iv", Base64.getEncoder().encodeToString(iv));
      node.put("encryptedPrivateKey", Base64.getEncoder().encodeToString(encrypted));

      String safePrefix = StringUtils.hasText(filePrefix) ? filePrefix : "wallet";
      String filename = safePrefix + "-" + FILE_TIME.format(LocalDateTime.now()) + "-" + address + ".json";
      Path file = dir.resolve(filename);
      JsonUtil.writePrettyJson(file, node);
      log.info("Wrote local keystore artifact. address={}, file={}", address, file);
      return file.toString();
    } catch (IOException | GeneralSecurityException ex) {
      throw new IllegalStateException("Failed to write local keystore artifact for address: " + address, ex);
    }
  }

  private byte[] encrypt(BigInteger privateKey, String password, byte[] salt, byte[] iv)
      throws GeneralSecurityException {
    javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
        password == null ? new char[0] : password.toCharArray(), salt, 65536, 256);
    byte[] keyBytes = factory.generateSecret(spec).getEncoded();
    javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
    javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(128, iv));
    return cipher.doFinal(privateKey.toString(16).getBytes(StandardCharsets.UTF_8));
  }

  private byte[] randomBytes(int size) {
    byte[] bytes = new byte[size];
    RANDOM.nextBytes(bytes);
    return bytes;
  }
}
