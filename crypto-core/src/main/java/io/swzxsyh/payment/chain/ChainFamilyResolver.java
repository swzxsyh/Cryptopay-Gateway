package io.swzxsyh.payment.chain;

import org.springframework.util.StringUtils;

/** 根据链编码推断链族，方便在未显式配置时做默认分流。 */
public final class ChainFamilyResolver {

  private ChainFamilyResolver() {}

  public static ChainFamily resolve(String chain) {
    if (!StringUtils.hasText(chain)) {
      return ChainFamily.UNKNOWN;
    }
    String normalized = chain.trim().toUpperCase();
    if (normalized.startsWith("TRON") || normalized.contains("TRON")) {
      return ChainFamily.TRON;
    }
    if (normalized.startsWith("SUI") || normalized.contains("SUI")) {
      return ChainFamily.SUI;
    }
    if (normalized.startsWith("TON") || normalized.contains("TON")) {
      return ChainFamily.TON;
    }
    if (normalized.startsWith("SOL") || normalized.contains("SOLANA")) {
      return ChainFamily.SOLANA;
    }
    return ChainFamily.EVM;
  }
}
