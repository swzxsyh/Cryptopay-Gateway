package io.swzxsyh.payment.signature;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swzxsyh.payment.mapper.RequestSignatureReplayRecordMapper;
import io.swzxsyh.payment.persistence.entity.RequestSignatureReplayRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 请求签名防重放服务。 */
@Slf4j
@Service
public class SignatureReplayService {

  private final RequestSignatureReplayRecordMapper mapper;

  public SignatureReplayService(RequestSignatureReplayRecordMapper mapper) {
    this.mapper = mapper;
  }

  public void claim(
      String scope, String merchantId, String nonce, String canonicalPayload, long ttlSeconds) {
    if (!StringUtils.hasText(scope)
        || !StringUtils.hasText(merchantId)
        || !StringUtils.hasText(nonce)) {
      throw new IllegalArgumentException("scope, merchantId and nonce are required");
    }

    String requestHash = sha256Hex(canonicalPayload);
    LocalDateTime now = LocalDateTime.now();
    RequestSignatureReplayRecord record = new RequestSignatureReplayRecord();
    record.setScope(scope);
    record.setMerchantId(merchantId);
    record.setNonce(nonce);
    record.setRequestHash(requestHash);
    record.setCreatedAt(now);
    record.setExpiresAt(now.plusSeconds(Math.max(1, ttlSeconds)));
    try {
      mapper.insert(record);
    } catch (DuplicateKeyException ex) {
      throw new IllegalStateException("duplicate signature nonce detected");
    }
  }

  public void purgeExpired() {
    mapper.delete(
        new LambdaQueryWrapper<RequestSignatureReplayRecord>()
            .lt(RequestSignatureReplayRecord::getExpiresAt, LocalDateTime.now()));
  }

  private String sha256Hex(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash signature payload", e);
    }
  }
}
