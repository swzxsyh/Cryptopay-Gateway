package io.swzxsyh.payment.detector;

import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import io.swzxsyh.payment.routing.WalletAccountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 钱包类型探测器，用于区分 EOA 和合约钱包。 */
@Slf4j
@Service
public class WalletCapabilityDetector {

  private final ChainClientFactory chainClientFactory;

  public WalletCapabilityDetector(ChainClientFactory chainClientFactory) {
    this.chainClientFactory = chainClientFactory;
  }

  /** 探测钱包类型，优先使用外部传入结果。 */
  public WalletAccountType detect(String chain, String walletAddress, WalletAccountType providedType) {
    if (providedType != null && providedType != WalletAccountType.UNKNOWN) {
      return providedType;
    }
    if (!StringUtils.hasText(walletAddress)) {
      return WalletAccountType.UNKNOWN;
    }
    if (ChainFamilyResolver.resolve(chain) != ChainFamily.EVM) {
      log.debug("Non-EVM chain skips wallet code detection. chain={}, walletAddress={}", chain, walletAddress);
      return WalletAccountType.UNKNOWN;
    }

    try {
      String code = chainClientFactory.get(chain).getCode(walletAddress);
      WalletAccountType detectedType = hasRuntimeCode(code) ? WalletAccountType.SCA : WalletAccountType.EOA;
      log.info("Detected wallet type. chain={}, walletAddress={}, walletType={}",
          chain, walletAddress, detectedType);
      return detectedType;
    } catch (Exception e) {
      log.warn("Failed to detect wallet type. chain={}, walletAddress={}, error={}",
          chain, walletAddress, e.getMessage());
      return WalletAccountType.UNKNOWN;
    }
  }

  /** 判断链上代码是否存在，以识别合约钱包。 */
  private boolean hasRuntimeCode(String code) {
    return StringUtils.hasText(code) && !"0x".equalsIgnoreCase(code);
  }
}
