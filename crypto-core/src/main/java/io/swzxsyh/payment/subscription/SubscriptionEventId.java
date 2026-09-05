package io.swzxsyh.payment.subscription;

import org.springframework.util.StringUtils;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

/** 订阅链上事件 ID 工具，保持 Java 端与合约 bytes32 订阅标识一致。 */
public final class SubscriptionEventId {

  private SubscriptionEventId() {}

  /** 使用订阅单号生成合约事件中的 bytes32 标识。 */
  public static String fromOrderNo(String subscriptionOrderNo) {
    if (!StringUtils.hasText(subscriptionOrderNo)) {
      throw new IllegalArgumentException("subscriptionOrderNo is required");
    }
    return normalize(Hash.sha3String(subscriptionOrderNo.trim()));
  }

  /** 统一 topic/bytes32 的大小写和 0x 前缀，避免不同来源格式导致匹配失败。 */
  public static String normalize(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    return Numeric.prependHexPrefix(Numeric.cleanHexPrefix(value.trim())).toLowerCase();
  }
}
