package io.swzxsyh.payment.chain;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.tx.RawTransactionManager;

/** 基于 Web3j 的链客户端实现。 */
public class Web3jChainClient implements ChainClient {

  private static final long RPC_TIMEOUT_SECONDS = 5L;

  private final Web3j web3j;

  public Web3jChainClient(String rpcUrl) {
    if (!StringUtils.hasText(rpcUrl)) {
      throw new IllegalArgumentException("rpcUrl is required");
    }
    OkHttpClient httpClient = HttpService.getOkHttpClientBuilder()
        .connectTimeout(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build();
    this.web3j = Web3j.build(new HttpService(rpcUrl, httpClient, false));
  }

  @Override
  public ChainFamily family() {
    return ChainFamily.EVM;
  }

  @Override
  public String getCode(String address) {
    try {
      EthGetCode response = web3j.ethGetCode(address, DefaultBlockParameterName.LATEST).send();
      return response.getCode();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch code for address: " + address, e);
    }
  }

  @Override
  public String ethCall(String contractAddress, String data) {
    try {
      EthCall response = web3j.ethCall(
          Transaction.createEthCallTransaction(null, contractAddress, data),
          DefaultBlockParameterName.LATEST).send();
      if (response.hasError()) {
        throw new IllegalStateException("eth_call error: " + response.getError().getMessage());
      }
      return response.getValue();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to eth_call contract: " + contractAddress, e);
    }
  }

  @Override
  public BigDecimal getNativeBalance(String address) {
    try {
      EthGetBalance response = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
      BigInteger wei = response.getBalance();
      return new BigDecimal(wei);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch native balance for address: " + address, e);
    }
  }

  @Override
  public BigDecimal getGasPrice() {
    try {
      EthGasPrice response = web3j.ethGasPrice().send();
      return new BigDecimal(response.getGasPrice());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch gas price", e);
    }
  }

  @Override
  public BigInteger getLatestBlockNumber() {
    try {
      return web3j.ethBlockNumber().send().getBlockNumber();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch latest block number", e);
    }
  }

  @Override
  public EthBlock getBlockByNumber(BigInteger blockNumber, boolean fullTransactions) {
    try {
      return web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockNumber), fullTransactions).send();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch block by number: " + blockNumber, e);
    }
  }

  @Override
  public EthLog getLogs(EthFilter filter) {
    try {
      return web3j.ethGetLogs(filter).send();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch logs", e);
    }
  }

  @Override
  public TransactionReceipt getTransactionReceipt(String txHash) {
    try {
      return web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt().orElse(null);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch transaction receipt: " + txHash, e);
    }
  }

  @Override
  public String sendTransaction(
      String privateKey, String to, String data, BigInteger valueWei, BigInteger gasLimit) {
    try {
      Credentials credentials = Credentials.create(privateKey);
      long chainId = web3j.ethChainId().send().getChainId().longValue();
      BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
      RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials, chainId);
      EthSendTransaction response =
          transactionManager.sendTransaction(gasPrice, gasLimit, to, data, valueWei);
      if (response.hasError()) {
        throw new IllegalStateException("send transaction error: " + response.getError().getMessage());
      }
      return response.getTransactionHash();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to send transaction to: " + to, e);
    }
  }
}
