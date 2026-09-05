package io.swzxsyh.payment.solana;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Solana 交易解析服务，负责从 jsonParsed 交易中提取 SOL / SPL Token 入账信息。 */
@Service
public class SolanaTransferParseService {

  private static final String SYSTEM_PROGRAM = "system";
  private static final String TOKEN_PROGRAM = "spl-token";
  private static final String TOKEN_2022_PROGRAM = "spl-token-2022";

  public Optional<SolanaIncomingTransfer> parse(
      JsonNode transaction, String expectedDestination, String expectedTokenAddress) {
    if (transaction == null || transaction.isMissingNode()) {
      return Optional.empty();
    }
    long slot = transaction.path("slot").asLong(0L);
    Instant blockTimestamp =
        transaction.hasNonNull("blockTime")
            ? Instant.ofEpochSecond(transaction.path("blockTime").asLong())
            : null;
    String signature = transaction.path("transaction").path("signatures").path(0).asText("");
    if (!StringUtils.hasText(signature)) {
      return Optional.empty();
    }

    JsonNode message = transaction.path("transaction").path("message");
    Optional<SolanaIncomingTransfer> parsed =
        parseInstructions(
            signature,
            slot,
            blockTimestamp,
            message.path("instructions"),
            expectedDestination,
            expectedTokenAddress);
    if (parsed.isPresent()) {
      return parsed;
    }
    JsonNode meta = transaction.path("meta");
    Optional<SolanaIncomingTransfer> balanceDelta =
        parseTokenBalanceDelta(
            signature,
            slot,
            blockTimestamp,
            message,
            meta,
            expectedDestination,
            expectedTokenAddress);
    if (balanceDelta.isPresent()) {
      return balanceDelta;
    }
    return parseInnerInstructions(
        signature,
        slot,
        blockTimestamp,
        meta.path("innerInstructions"),
        expectedDestination,
        expectedTokenAddress);
  }

  private Optional<SolanaIncomingTransfer> parseInnerInstructions(
      String signature,
      long slot,
      Instant blockTimestamp,
      JsonNode innerInstructions,
      String expectedDestination,
      String expectedTokenAddress) {
    if (!innerInstructions.isArray()) {
      return Optional.empty();
    }
    for (JsonNode inner : innerInstructions) {
      Optional<SolanaIncomingTransfer> parsed =
          parseInstructions(
              signature,
              slot,
              blockTimestamp,
              inner.path("instructions"),
              expectedDestination,
              expectedTokenAddress);
      if (parsed.isPresent()) {
        return parsed;
      }
    }
    return Optional.empty();
  }

  private Optional<SolanaIncomingTransfer> parseTokenBalanceDelta(
      String signature,
      long slot,
      Instant blockTimestamp,
      JsonNode message,
      JsonNode meta,
      String expectedDestination,
      String expectedTokenAddress) {
    if (!StringUtils.hasText(expectedDestination) || !meta.path("postTokenBalances").isArray()) {
      return Optional.empty();
    }
    Map<Integer, TokenBalanceSnapshot> preBalances = new HashMap<>();
    for (JsonNode balance : meta.path("preTokenBalances")) {
      TokenBalanceSnapshot snapshot = tokenBalanceSnapshot(balance);
      preBalances.put(snapshot.accountIndex(), snapshot);
    }
    for (JsonNode balance : meta.path("postTokenBalances")) {
      TokenBalanceSnapshot post = tokenBalanceSnapshot(balance);
      if (!sameAddress(post.owner(), expectedDestination)) {
        continue;
      }
      if (StringUtils.hasText(expectedTokenAddress)
          && StringUtils.hasText(post.mint())
          && !expectedTokenAddress.equalsIgnoreCase(post.mint())) {
        continue;
      }
      TokenBalanceSnapshot pre =
          preBalances.getOrDefault(
              post.accountIndex(), new TokenBalanceSnapshot(post.accountIndex(), post.owner(), post.mint(), BigDecimal.ZERO, post.decimals()));
      BigDecimal delta = post.amount().subtract(pre.amount());
      if (delta.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      return Optional.of(
          new SolanaIncomingTransfer(
              signature,
              slot,
              firstSigner(message),
              expectedDestination,
              post.mint(),
              delta,
              blockTimestamp,
              false,
              post.decimals()));
    }
    return Optional.empty();
  }

  private TokenBalanceSnapshot tokenBalanceSnapshot(JsonNode balance) {
    int decimals = balance.path("uiTokenAmount").path("decimals").asInt(0);
    String amount = balance.path("uiTokenAmount").path("amount").asText("0");
    return new TokenBalanceSnapshot(
        balance.path("accountIndex").asInt(-1),
        balance.path("owner").asText(""),
        balance.path("mint").asText(""),
        decimalTokenAmount(amount, decimals),
        decimals);
  }

  private String firstSigner(JsonNode message) {
    JsonNode keys = message.path("accountKeys");
    if (!keys.isArray()) {
      return "";
    }
    for (JsonNode key : keys) {
      if (key.path("signer").asBoolean(false)) {
        return key.path("pubkey").asText("");
      }
    }
    return keys.path(0).path("pubkey").asText("");
  }

  private Optional<SolanaIncomingTransfer> parseInstructions(
      String signature,
      long slot,
      Instant blockTimestamp,
      JsonNode instructions,
      String expectedDestination,
      String expectedTokenAddress) {
    if (!instructions.isArray()) {
      return Optional.empty();
    }
    for (JsonNode instruction : instructions) {
      Optional<SolanaIncomingTransfer> parsed =
          parseInstruction(
              signature,
              slot,
              blockTimestamp,
              instruction,
              expectedDestination,
              expectedTokenAddress);
      if (parsed.isPresent()) {
        return parsed;
      }
    }
    return Optional.empty();
  }

  private Optional<SolanaIncomingTransfer> parseInstruction(
      String signature,
      long slot,
      Instant blockTimestamp,
      JsonNode instruction,
      String expectedDestination,
      String expectedTokenAddress) {
    if (instruction == null || instruction.isMissingNode()) {
      return Optional.empty();
    }

    String program = instruction.path("program").asText("");
    String programId = instruction.path("programId").asText("");
    JsonNode parsed = instruction.path("parsed");
    if (parsed.isMissingNode() || !parsed.isObject()) {
      return Optional.empty();
    }

    String type = parsed.path("type").asText("");
    JsonNode info = parsed.path("info");
    if (!info.isObject()) {
      return Optional.empty();
    }

    if (SYSTEM_PROGRAM.equalsIgnoreCase(program)
        && matches(type, "transfer", "transferWithSeed")) {
      String destination = info.path("destination").asText("");
      if (!sameAddress(destination, expectedDestination)) {
        return Optional.empty();
      }
      BigDecimal amount = lamportsToSol(info.path("lamports").asText("0"));
      return Optional.of(
          new SolanaIncomingTransfer(
              signature,
              slot,
              info.path("source").asText(""),
              destination,
              "",
              amount,
              blockTimestamp,
              true,
              9));
    }

    if ((TOKEN_PROGRAM.equalsIgnoreCase(program) || TOKEN_2022_PROGRAM.equalsIgnoreCase(program)
            || programId.contains("Token"))
        && matches(type, "transfer", "transferChecked", "mintTo", "mintToChecked")) {
      String destination = info.path("destination").asText("");
      if (!sameAddress(destination, expectedDestination)) {
        return Optional.empty();
      }
      String tokenAddress = info.path("mint").asText("");
      if (StringUtils.hasText(expectedTokenAddress)
          && StringUtils.hasText(tokenAddress)
          && !expectedTokenAddress.equalsIgnoreCase(tokenAddress)) {
        return Optional.empty();
      }
      String source = info.path("source").asText(info.path("authority").asText(""));
      int decimals = info.path("tokenAmount").path("decimals").asInt(info.path("decimals").asInt(0));
      BigDecimal amount = tokenAmount(info);
      return Optional.of(
          new SolanaIncomingTransfer(
              signature,
              slot,
              source,
              destination,
              tokenAddress,
              amount,
              blockTimestamp,
              false,
              decimals));
    }

    return Optional.empty();
  }

  private boolean matches(String value, String... candidates) {
    for (String candidate : candidates) {
      if (candidate.equalsIgnoreCase(value)) {
        return true;
      }
    }
    return false;
  }

  private boolean sameAddress(String left, String right) {
    return StringUtils.hasText(left)
        && StringUtils.hasText(right)
        && left.trim().equalsIgnoreCase(right.trim());
  }

  private BigDecimal lamportsToSol(String lamports) {
    try {
      return new BigDecimal(new BigInteger(StringUtils.hasText(lamports) ? lamports.trim() : "0"))
          .movePointLeft(9)
          .setScale(9, RoundingMode.DOWN);
    } catch (Exception ex) {
      return BigDecimal.ZERO;
    }
  }

  private BigDecimal tokenAmount(JsonNode info) {
    JsonNode tokenAmount = info.path("tokenAmount");
    String uiAmountString = tokenAmount.path("uiAmountString").asText("");
    if (StringUtils.hasText(uiAmountString)) {
      try {
        return new BigDecimal(uiAmountString);
      } catch (Exception ignore) {
        // fall through
      }
    }
    String amount = tokenAmount.path("amount").asText(info.path("amount").asText("0"));
    int decimals = tokenAmount.path("decimals").asInt(info.path("decimals").asInt(0));
    try {
      return new BigDecimal(new BigInteger(amount)).movePointLeft(Math.max(0, decimals));
    } catch (Exception ex) {
      return BigDecimal.ZERO;
    }
  }

  private BigDecimal decimalTokenAmount(String amount, int decimals) {
    try {
      return new BigDecimal(new BigInteger(StringUtils.hasText(amount) ? amount.trim() : "0"))
          .movePointLeft(Math.max(0, decimals));
    } catch (Exception ex) {
      return BigDecimal.ZERO;
    }
  }

  private record TokenBalanceSnapshot(
      int accountIndex,
      String owner,
      String mint,
      BigDecimal amount,
      int decimals) {}
}
