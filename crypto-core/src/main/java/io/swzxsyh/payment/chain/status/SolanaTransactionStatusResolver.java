package io.swzxsyh.payment.chain.status;

import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.SolanaChainClient;
import io.swzxsyh.payment.persistence.entity.PendingChainTransaction;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/** Solana 交易状态解析器，统一处理 signature 是否可查、meta.err 和 slot 确认数。 */
@Component
public class SolanaTransactionStatusResolver implements ChainTransactionStatusResolver {

  private final ChainClientFactory chainClientFactory;

  public SolanaTransactionStatusResolver(ChainClientFactory chainClientFactory) {
    this.chainClientFactory = chainClientFactory;
  }

  @Override
  public boolean supports(String chain) {
    return chainClientFactory.get(chain).family() == ChainFamily.SOLANA;
  }

  @Override
  public ChainTransactionStatusResult resolve(ChainTransactionStatusContext context) {
    PendingChainTransaction pending = context.pending();
    var client = chainClientFactory.get(pending.getChain());
    if (!(client instanceof SolanaChainClient solanaClient)) {
      return ChainTransactionStatusResult.unsupported("chain client is not SolanaChainClient");
    }
    JsonNode transaction = solanaClient.getTransaction(pending.getTxHash());
    if (transaction == null || transaction.isMissingNode() || transaction.isNull()) {
      return new ChainTransactionStatusResult(
          ChainTransactionStatus.CONFIRMING,
          0,
          pending.getBlockNumber(),
          "Solana transaction is not available yet");
    }
    JsonNode metaErr = transaction.path("meta").path("err");
    if (!metaErr.isMissingNode() && !metaErr.isNull()) {
      return new ChainTransactionStatusResult(
          ChainTransactionStatus.FAILED,
          confirmations(context.latestBlock(), pending.getBlockNumber()),
          pending.getBlockNumber(),
          "Solana transaction meta.err is present");
    }
    int confirmations = confirmations(context.latestBlock(), pending.getBlockNumber());
    ChainTransactionStatus status =
        confirmations >= context.targetConfirmations()
            ? ChainTransactionStatus.CONFIRMED
            : ChainTransactionStatus.CONFIRMING;
    return new ChainTransactionStatusResult(
        status,
        confirmations,
        pending.getBlockNumber(),
        "Solana transaction verified");
  }

  private int confirmations(long latestSlot, Long txSlot) {
    if (txSlot == null) {
      return 0;
    }
    return (int) Math.max(0L, latestSlot - txSlot + 1L);
  }
}
