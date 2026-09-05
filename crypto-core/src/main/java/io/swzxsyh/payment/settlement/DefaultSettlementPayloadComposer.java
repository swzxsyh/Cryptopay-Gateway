package io.swzxsyh.payment.settlement;

import java.util.ArrayList;
import java.math.BigInteger;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

/** 默认分账负载拼装器，把订单与分账规则编码成链上合约可执行的 ABI calldata。 */
@Component
public class DefaultSettlementPayloadComposer implements SettlementPayloadComposer {

  /** 拼装签名用的规范化负载。 */
  @Override
  public String composeSignPayload(ContractSettlementCommand command, List<SettlementSplit> splits) {
    List<String> fields = new ArrayList<>();
    fields.add("order=" + command.cryptoOrderNo());
    fields.add("chain=" + command.chain());
    fields.add("token=" + command.token());
    fields.add("tokenAddress=" + command.tokenAddress());
    fields.add("amount=" + command.amount().toPlainString());
    fields.add("amountUnit=" + toTokenUnit(command));
    fields.add("contract=" + command.contractAddress());
    fields.add("expireTime=" + command.expireTime());
    fields.add("splits=" + splits.stream()
        .map(split -> split.role() + ":" + split.receiver() + ":" + split.basisPoints())
        .collect(Collectors.joining(",")));
    return String.join("|", fields);
  }

  /** 拼装隔离入账 calldata，前端钱包会把它作为 eth_sendTransaction.data 广播到链上。 */
  @Override
  public String composeContractCallData(
      ContractSettlementCommand command,
      List<SettlementSplit> splits,
      String signature) {
    Function function = new Function(
        "payIntoEscrow",
        List.of(
            new Bytes32(orderIdBytes32(command.cryptoOrderNo())),
            new Address(normalizeAddress(command.tokenAddress(), "tokenAddress")),
            new Uint256(toTokenUnit(command)),
            new DynamicArray<>(
                Address.class,
                splits.stream()
                    .map(split -> new Address(normalizeAddress(split.receiver(), "split.receiver")))
                    .toList()),
            new DynamicArray<>(
                Uint256.class,
                splits.stream()
                    .map(split -> new Uint256(BigInteger.valueOf(split.basisPoints())))
                    .toList()),
            new Uint256(expireEpochSeconds(command)),
            new DynamicBytes(signatureBytes(signature))),
        List.of());
    return FunctionEncoder.encode(function);
  }

  private BigInteger toTokenUnit(ContractSettlementCommand command) {
    return command.amount().movePointRight(command.tokenDecimals()).toBigIntegerExact();
  }

  private BigInteger expireEpochSeconds(ContractSettlementCommand command) {
    if (command.expireTime() == null) {
      return BigInteger.ZERO;
    }
    return BigInteger.valueOf(command.expireTime().atZone(ZoneId.systemDefault()).toEpochSecond());
  }

  private byte[] orderIdBytes32(String orderNo) {
    return Numeric.hexStringToByteArray(Hash.sha3String(orderNo));
  }

  private byte[] signatureBytes(String signature) {
    if (!StringUtils.hasText(signature)) {
      return new byte[0];
    }
    return Numeric.hexStringToByteArray(signature);
  }

  private String normalizeAddress(String address, String fieldName) {
    if (!StringUtils.hasText(address)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    String normalized = address.trim();
    if (!normalized.matches("^0x[0-9a-fA-F]{40}$")) {
      throw new IllegalArgumentException(fieldName + " must be a valid EVM address");
    }
    return normalized;
  }
}
