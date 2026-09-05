package io.swzxsyh.payment.chain;

import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;

/** 链客户端抽象，用于屏蔽不同链 RPC/Web3j 访问差异。 */
public interface ChainClient {

  ChainFamily family();

  default boolean isEvmFamily() {
    return family() == ChainFamily.EVM;
  }

  String getCode(String address);

  String ethCall(String contractAddress, String data);

  java.math.BigDecimal getNativeBalance(String address);

  java.math.BigDecimal getGasPrice();

  java.math.BigInteger getLatestBlockNumber();

  org.web3j.protocol.core.methods.response.EthBlock getBlockByNumber(java.math.BigInteger blockNumber, boolean fullTransactions);

  EthLog getLogs(EthFilter filter);

  org.web3j.protocol.core.methods.response.TransactionReceipt getTransactionReceipt(String txHash);

  String sendTransaction(
      String privateKey,
      String to,
      String data,
      java.math.BigInteger valueWei,
      java.math.BigInteger gasLimit);
}
