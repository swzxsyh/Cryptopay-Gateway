package io.swzxsyh.payment.chain;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/** 非 EVM 链的占位客户端，保留链族识别与配置接入，但不提供 EVM RPC 能力。 */
public class UnsupportedChainClient implements ChainClient {

  private final ChainFamily family;

  public UnsupportedChainClient(ChainFamily family) {
    this.family = family == null ? ChainFamily.UNKNOWN : family;
  }

  @Override
  public ChainFamily family() {
    return family;
  }

  @Override
  public String getCode(String address) {
    throw unsupported();
  }

  @Override
  public String ethCall(String contractAddress, String data) {
    throw unsupported();
  }

  @Override
  public BigDecimal getNativeBalance(String address) {
    throw unsupported();
  }

  @Override
  public BigDecimal getGasPrice() {
    throw unsupported();
  }

  @Override
  public BigInteger getLatestBlockNumber() {
    throw unsupported();
  }

  @Override
  public EthBlock getBlockByNumber(BigInteger blockNumber, boolean fullTransactions) {
    throw unsupported();
  }

  @Override
  public EthLog getLogs(EthFilter filter) {
    throw unsupported();
  }

  @Override
  public TransactionReceipt getTransactionReceipt(String txHash) {
    throw unsupported();
  }

  @Override
  public String sendTransaction(String privateKey, String to, String data, BigInteger valueWei, BigInteger gasLimit) {
    throw unsupported();
  }

  private UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException("Chain family " + family + " is not supported by the EVM RPC client");
  }
}
