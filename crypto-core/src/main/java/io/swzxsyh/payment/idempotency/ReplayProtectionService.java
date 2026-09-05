package io.swzxsyh.payment.idempotency;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.payment.util.RedisUtil;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@ConditionalOnBean(RedisUtil.class)
public class ReplayProtectionService {

  private final RedisUtil redisUtil;
  private final CryptoPaymentProperties properties;

  public ReplayProtectionService(RedisUtil redisUtil, CryptoPaymentProperties properties) {
    this.redisUtil = redisUtil;
    this.properties = properties;
  }

  public boolean claimTx(String chain, String txHash, long ttlHours) {
    if (!StringUtils.hasText(chain) || !StringUtils.hasText(txHash)) {
      return false;
    }
    String key = RedisKeyNamespace.replayTx(properties, chain, txHash);
    return redisUtil.trySetString(key, "1", ttlHours, TimeUnit.HOURS);
  }

  public boolean claimOrderKey(String scope, String key, long ttlHours) {
    if (!StringUtils.hasText(scope) || !StringUtils.hasText(key)) {
      return false;
    }
    String redisKey = RedisKeyNamespace.replayOrder(properties, scope, key);
    return redisUtil.trySetString(redisKey, "1", ttlHours, TimeUnit.HOURS);
  }
}
