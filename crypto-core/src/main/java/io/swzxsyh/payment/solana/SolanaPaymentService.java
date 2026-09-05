package io.swzxsyh.payment.solana;

import io.swzxsyh.payment.chain.ChainClient;
import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.SolanaChainClient;
import io.swzxsyh.payment.chain.solana.SolanaJsonRpcClient;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.repository.PaymentOrderRepository;
import io.swzxsyh.payment.util.SecretValueResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.util.Base64;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import org.p2p.solanaj.core.Account;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Solana 支付协议服务，封装 fee payer、费用预估和 raw transaction 广播。 */
@Service
public class SolanaPaymentService {

  private static final BigDecimal LAMPORTS_PER_SOL = new BigDecimal("1000000000");

  private final CryptoPaymentProperties properties;
  private final ChainClientFactory chainClientFactory;
  private final PaymentOrderRepository orderRepository;
  private final SecretValueResolver secretValueResolver;
  private final SolanaFeePayerMessageValidator feePayerMessageValidator;

  public SolanaPaymentService(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      PaymentOrderRepository orderRepository,
      SecretValueResolver secretValueResolver,
      SolanaFeePayerMessageValidator feePayerMessageValidator) {
    this.properties = properties;
    this.chainClientFactory = chainClientFactory;
    this.orderRepository = orderRepository;
    this.secretValueResolver = secretValueResolver;
    this.feePayerMessageValidator = feePayerMessageValidator;
  }

  /** 根据订单生成 Solana 钱包支付意图。 */
  public SolanaPaymentIntent buildPaymentIntent(String cryptoOrderNo) {
    PaymentOrder order =
        orderRepository
            .findByCryptoOrderNo(cryptoOrderNo)
            .orElseThrow(() -> new IllegalArgumentException("Crypto order not found: " + cryptoOrderNo));
    SolanaChainClient client = solanaClient(order.getChain());
    CryptoPaymentProperties.ChainProfile chainProfile = chainProfile(order.getChain());
    CryptoPaymentProperties.TokenProfile tokenProfile = tokenProfile(order.getChain(), order.getTokenAddress(), order.getToken());
    SolanaJsonRpcClient.LatestBlockhash blockhash = client.getLatestBlockhash();
    long estimatedFeeLamports = fallbackFeeLamports(1);
    String feePayerAddress = resolveFeePayerAddress();
    String recipient = StringUtils.hasText(order.getPaymentAddress()) ? order.getPaymentAddress() : order.getContractAddress();
    if (!StringUtils.hasText(recipient)) {
      throw new IllegalStateException("Solana payment recipient is not prepared for order: " + cryptoOrderNo);
    }
    boolean feePayerEnabled =
        properties.getSolana().isFeePayerEnabled() && StringUtils.hasText(feePayerAddress);
    return new SolanaPaymentIntent(
        order.getCryptoOrderNo(),
        order.getChain(),
        chainProfile.getRpcUrl(),
        recipient,
        order.getAmount(),
        order.getToken(),
        order.getTokenAddress(),
        tokenProfile == null ? 9 : tokenProfile.getDecimals(),
        feePayerEnabled,
        feePayerEnabled ? "PLATFORM_FEE_PAYER" : "CUSTOMER_PAYS",
        feePayerEnabled ? feePayerAddress : "",
        estimatedFeeLamports,
        lamportsToSol(estimatedFeeLamports),
        blockhash.blockhash(),
        blockhash.lastValidBlockHeight(),
        feePayerEnabled
            ? "平台可作为 Solana fee payer 支付交易手续费；用户仍需钱包签名授权转账。"
            : "当前 Solana 钱包支付由用户钱包作为 fee payer。");
  }

  /** 根据前端构造好的 Solana message 预估 fee；没有 message 时按签名数量做保守估算。 */
  public SolanaFeeEstimateResponse estimateFee(SolanaFeeEstimateRequest request) {
    SolanaChainClient client = solanaClient(request.chain());
    SolanaJsonRpcClient.LatestBlockhash blockhash = client.getLatestBlockhash();
    if (StringUtils.hasText(request.messageBase64())) {
      long fee = client.getFeeForMessage(request.messageBase64());
      if (fee > 0L) {
        return new SolanaFeeEstimateResponse(
            request.chain(),
            fee,
            lamportsToSol(fee),
            true,
            blockhash.blockhash(),
            blockhash.lastValidBlockHeight(),
            "estimated by Solana getFeeForMessage");
      }
    }
    int signerCount = request.signerCount() == null ? 1 : Math.max(1, request.signerCount());
    long fallback = fallbackFeeLamports(signerCount);
    return new SolanaFeeEstimateResponse(
        request.chain(),
        fallback,
        lamportsToSol(fallback),
        false,
        blockhash.blockhash(),
        blockhash.lastValidBlockHeight(),
        "fallback fee estimate by signer count");
  }

  /** 广播已由钱包签名完成的 Solana raw transaction。 */
  public SolanaRawTransactionResponse sendRawTransaction(SolanaRawTransactionRequest request) {
    String signature = solanaClient(request.chain()).sendRawTransaction(request.signedTransactionBase64());
    return new SolanaRawTransactionResponse(request.chain(), signature);
  }

  /** 使用平台 fee payer 私钥签名 Solana transaction message。 */
  public SolanaFeePayerSignResponse signFeePayerMessage(PaymentOrder order, SolanaFeePayerSignRequest request) {
    if (!properties.getSolana().isFeePayerEnabled()) {
      throw new IllegalStateException("Solana fee payer is disabled");
    }
    if (order == null || !StringUtils.hasText(order.getChain())) {
      throw new IllegalArgumentException("Solana order is required");
    }
    if (!order.getChain().equalsIgnoreCase(request.chain())) {
      throw new IllegalArgumentException("Solana fee payer request chain does not match order chain");
    }
    OrderStatus status = order.getStatus();
    if (status != OrderStatus.WAITING_PAYMENT && status != OrderStatus.DETECTED) {
      throw new IllegalArgumentException("Solana order is not payable: " + order.getStatus());
    }
    solanaClient(order.getChain());
    byte[] message = decodeBase64(request.messageBase64(), "Solana transaction message");
    if (message.length == 0 || message.length > 4096) {
      throw new IllegalArgumentException("Invalid Solana transaction message length");
    }
    Account account = resolveFeePayerAccount();
    feePayerMessageValidator.validate(order, account.getPublicKeyBase58(), message);
    byte[] signature = signEd25519(account.getSecretKey(), message);
    return new SolanaFeePayerSignResponse(
        request.chain(),
        account.getPublicKeyBase58(),
        Base64.getEncoder().encodeToString(signature));
  }

  /** 解析 fee payer 地址：优先显式配置，其次从 SDK 支持的私钥格式推导。 */
  public String resolveFeePayerAddress() {
    if (!properties.getSolana().isFeePayerEnabled()
        && !StringUtils.hasText(properties.getSolana().getFeePayerAddress())) {
      return "";
    }
    if (StringUtils.hasText(properties.getSolana().getFeePayerAddress())) {
      return properties.getSolana().getFeePayerAddress().trim();
    }
    return resolveFeePayerAccount().getPublicKeyBase58();
  }

  private Account resolveFeePayerAccount() {
    String privateKey =
        secretValueResolver.resolveOptional(
            properties.getSolana().getFeePayerPrivateKeySourceType(),
            properties.getSolana().getFeePayerPrivateKey(),
            properties.getSolana().getFeePayerPrivateKeyEnv(),
            properties.getSolana().getFeePayerPrivateKeyKmsKeyId(),
            "CRYPTO_PAYMENT_SOLANA_FEE_PAYER_PRIVATE_KEY");
    if (!StringUtils.hasText(privateKey)) {
      throw new IllegalStateException("Solana fee payer private key is not configured");
    }
    try {
      return privateKey.trim().startsWith("[")
          ? Account.fromJson(privateKey.trim())
          : Account.fromBase58PrivateKey(privateKey.trim());
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid Solana fee payer private key", ex);
    }
  }

  private byte[] signEd25519(byte[] secretKey, byte[] message) {
    try {
      byte[] seed = secretKey.length >= 32 ? java.util.Arrays.copyOfRange(secretKey, 0, 32) : secretKey;
      var spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
      EdDSAPrivateKey privateKey = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(seed, spec));
      EdDSAEngine signer = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
      signer.initSign(privateKey);
      signer.update(message);
      return signer.sign();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to sign Solana fee payer message", ex);
    }
  }

  private byte[] decodeBase64(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    try {
      return Base64.getDecoder().decode(value.trim());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(fieldName + " must be base64", ex);
    }
  }

  private SolanaChainClient solanaClient(String chain) {
    ChainClient client = chainClientFactory.get(chain);
    if (client.family() != ChainFamily.SOLANA || !(client instanceof SolanaChainClient solanaChainClient)) {
      throw new IllegalArgumentException("chain is not Solana: " + chain);
    }
    return solanaChainClient;
  }

  private CryptoPaymentProperties.ChainProfile chainProfile(String chain) {
    return properties.getChainProfiles().stream()
        .filter(item -> item.isEnabled() && item.getChain().equalsIgnoreCase(chain))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No chain profile configured for chain: " + chain));
  }

  private CryptoPaymentProperties.TokenProfile tokenProfile(String chain, String tokenAddress, String token) {
    return properties.getTokenProfiles().stream()
        .filter(item -> item.getChain().equalsIgnoreCase(chain))
        .filter(item -> !StringUtils.hasText(tokenAddress) || tokenAddress.equalsIgnoreCase(item.getTokenAddress()))
        .filter(item -> !StringUtils.hasText(token) || token.equalsIgnoreCase(item.getToken()))
        .findFirst()
        .orElse(null);
  }

  private long fallbackFeeLamports(int signerCount) {
    long perSignature = Math.max(1L, properties.getSolana().getDefaultFeeLamportsPerSignature());
    return perSignature * Math.max(1, signerCount);
  }

  private BigDecimal lamportsToSol(long lamports) {
    return BigDecimal.valueOf(lamports).divide(LAMPORTS_PER_SOL, 9, RoundingMode.DOWN);
  }

}
