package io.swzxsyh.payment.pending;

import io.swzxsyh.payment.chain.status.ChainTransactionStatusContext;
import io.swzxsyh.payment.chain.status.ChainTransactionStatusResolverRegistry;
import io.swzxsyh.payment.chain.status.ChainTransactionStatusResult;
import io.swzxsyh.payment.persistence.entity.PendingChainTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 达到确认数前的链上复核，避免未确认旁路因 reorg 或 RPC 抖动误投递。 */
@Slf4j
@Service
public class PendingChainTransactionVerifier {

  private final ChainTransactionStatusResolverRegistry statusResolverRegistry;

  public PendingChainTransactionVerifier(ChainTransactionStatusResolverRegistry statusResolverRegistry) {
    this.statusResolverRegistry = statusResolverRegistry;
  }

  /** 返回 true 才允许把 pending 交易投递到最终入账链路；临时查不到时返回 false 等下一轮。 */
  public boolean isStillCanonical(PendingChainTransaction pending, long latestBlock) {
    if (pending == null || !StringUtils.hasText(pending.getChain()) || !StringUtils.hasText(pending.getTxHash())) {
      return false;
    }
    ChainTransactionStatusResult result =
        statusResolverRegistry.resolve(ChainTransactionStatusContext.of(pending, latestBlock));
    if (!result.readyForPayment()) {
      log.debug(
          "链上交易统一状态未达到入账条件。chain={}, txHash={}, status={}, confirmations={}, reason={}",
          pending.getChain(),
          pending.getTxHash(),
          result.status(),
          result.currentConfirmations(),
          result.reason());
    }
    return result.readyForPayment();
  }
}
