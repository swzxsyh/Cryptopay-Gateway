package io.swzxsyh.payment.chain.status;

import io.swzxsyh.payment.chain.ChainClient;
import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.persistence.entity.PendingChainTransaction;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/** EVM 交易状态解析器，统一处理 receipt 成功、日志是否仍在规范链和确认数。 */
@Slf4j
@Component
public class EvmTransactionStatusResolver implements ChainTransactionStatusResolver {

  private final ChainClientFactory chainClientFactory;

  public EvmTransactionStatusResolver(ChainClientFactory chainClientFactory) {
    this.chainClientFactory = chainClientFactory;
  }

  @Override
  public boolean supports(String chain) {
    return chainClientFactory.get(chain).family() == ChainFamily.EVM;
  }

  @Override
  public ChainTransactionStatusResult resolve(ChainTransactionStatusContext context) {
    PendingChainTransaction pending = context.pending();
    ChainClient client = chainClientFactory.get(pending.getChain());
    TransactionReceipt receipt = client.getTransactionReceipt(pending.getTxHash());
    if (receipt == null || receipt.getBlockNumber() == null) {
      return new ChainTransactionStatusResult(
          ChainTransactionStatus.CONFIRMING,
          0,
          pending.getBlockNumber(),
          "EVM receipt is not available yet");
    }
    if (!receipt.isStatusOK()) {
      return new ChainTransactionStatusResult(
          ChainTransactionStatus.FAILED,
          confirmations(context.latestBlock(), receipt.getBlockNumber().longValue()),
          receipt.getBlockNumber().longValue(),
          "EVM transaction receipt status is not success: " + receipt.getStatus());
    }
    if (!matchesExpectedTarget(receipt, pending)) {
      return new ChainTransactionStatusResult(
          ChainTransactionStatus.REORGED,
          confirmations(context.latestBlock(), receipt.getBlockNumber().longValue()),
          receipt.getBlockNumber().longValue(),
          "EVM receipt no longer contains expected transfer target/log");
    }
    int confirmations = confirmations(context.latestBlock(), receipt.getBlockNumber().longValue());
    ChainTransactionStatus status =
        confirmations >= context.targetConfirmations()
            ? ChainTransactionStatus.CONFIRMED
            : ChainTransactionStatus.CONFIRMING;
    return new ChainTransactionStatusResult(
        status,
        confirmations,
        receipt.getBlockNumber().longValue(),
        "EVM receipt verified");
  }

  private boolean matchesExpectedTarget(TransactionReceipt receipt, PendingChainTransaction pending) {
    if (StringUtils.hasText(pending.getTokenAddress())) {
      return receipt.getLogs() != null
          && receipt.getLogs().stream().anyMatch(log -> sameEvmLog(log, pending));
    }
    return !StringUtils.hasText(pending.getToAddress())
        || Objects.toString(receipt.getTo(), "").equalsIgnoreCase(pending.getToAddress());
  }

  private boolean sameEvmLog(Log log, PendingChainTransaction pending) {
    if (log == null) {
      return false;
    }
    boolean sameContract =
        !StringUtils.hasText(pending.getTokenAddress())
            || Objects.toString(log.getAddress(), "").equalsIgnoreCase(pending.getTokenAddress());
    boolean sameIndex =
        pending.getLogIndex() == null
            || log.getLogIndex() == null
            || pending.getLogIndex().longValue() == log.getLogIndex().longValue();
    return sameContract && sameIndex;
  }

  private int confirmations(long latestBlock, long txBlock) {
    return (int) Math.max(0L, latestBlock - txBlock + 1L);
  }
}
