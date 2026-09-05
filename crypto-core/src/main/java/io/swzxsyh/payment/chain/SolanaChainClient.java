package io.swzxsyh.payment.chain;

import io.swzxsyh.payment.chain.solana.SolanaJsonRpcClient;
import io.swzxsyh.payment.util.HttpClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/** Solana 链客户端骨架，先把链族抽象打通。 */
public class SolanaChainClient implements ChainClient {

  private static final BigDecimal LAMPORTS_PER_SOL = new BigDecimal("1000000000");

  private final SolanaJsonRpcClient rpcClient;

  public SolanaChainClient(String rpcUrl) {
    this.rpcClient = new SolanaJsonRpcClient(rpcUrl, new ObjectMapper());
  }

  public SolanaChainClient(String rpcUrl, ObjectMapper objectMapper, HttpClientUtil httpClientUtil) {
    this.rpcClient = new SolanaJsonRpcClient(rpcUrl, objectMapper, httpClientUtil);
  }

  @Override
  public ChainFamily family() {
    return ChainFamily.SOLANA;
  }

  @Override
  public String getCode(String address) {
    return "";
  }

  @Override
  public String ethCall(String contractAddress, String data) {
    throw new UnsupportedOperationException("Solana does not support EVM eth_call");
  }

  @Override
  public BigDecimal getNativeBalance(String address) {
    return new BigDecimal(BigInteger.valueOf(rpcClient.getBalanceLamports(address)))
        .divide(LAMPORTS_PER_SOL);
  }

  @Override
  public BigDecimal getGasPrice() {
    return BigDecimal.ZERO;
  }

  @Override
  public BigInteger getLatestBlockNumber() {
    return BigInteger.valueOf(rpcClient.getSlot());
  }

  public ArrayNode getSignaturesForAddress(String address, int limit, String before) {
    return rpcClient.getSignaturesForAddress(address, limit, before);
  }

  public JsonNode getTransaction(String signature) {
    return rpcClient.getTransaction(signature);
  }

  public SolanaJsonRpcClient.LatestBlockhash getLatestBlockhash() {
    return rpcClient.getLatestBlockhash();
  }

  public long getFeeForMessage(String messageBase64) {
    return rpcClient.getFeeForMessage(messageBase64);
  }

  public String sendRawTransaction(String signedTransactionBase64) {
    return rpcClient.sendRawTransaction(signedTransactionBase64);
  }

  public JsonNode simulateTransaction(String transactionBase64) {
    return rpcClient.simulateTransaction(transactionBase64);
  }

  public ArrayNode getRecentPrioritizationFees(String... addresses) {
    return rpcClient.getRecentPrioritizationFees(addresses);
  }

  @Override
  public EthBlock getBlockByNumber(BigInteger blockNumber, boolean fullTransactions) {
    throw new UnsupportedOperationException("Solana does not support EVM block retrieval");
  }

  @Override
  public EthLog getLogs(EthFilter filter) {
    throw new UnsupportedOperationException("Solana does not support EVM log filters");
  }

  @Override
  public TransactionReceipt getTransactionReceipt(String txHash) {
    return null;
  }

  @Override
  public String sendTransaction(
      String privateKey, String to, String data, BigInteger valueWei, BigInteger gasLimit) {
    throw new UnsupportedOperationException("Solana transaction sending is not wired yet");
  }
}
