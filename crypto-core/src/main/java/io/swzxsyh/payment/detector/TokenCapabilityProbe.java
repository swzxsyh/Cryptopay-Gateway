package io.swzxsyh.payment.detector;

import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import java.math.BigInteger;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;

/** Token 合约能力探测器，用于判断协议能力是否可用。 */
@Slf4j
@Service
public class TokenCapabilityProbe {

  private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
  private static final byte[] ZERO_BYTES_32 = new byte[32];

  private final ChainClientFactory chainClientFactory;

  public TokenCapabilityProbe(ChainClientFactory chainClientFactory) {
    this.chainClientFactory = chainClientFactory;
  }

  /** 判断代币合约是否存在。 */
  public boolean tokenContractExists(String chain, String tokenAddress) {
    if (!StringUtils.hasText(tokenAddress)) {
      return false;
    }
    if (ChainFamilyResolver.resolve(chain) != ChainFamily.EVM) {
      log.debug("Non-EVM chain skips token contract existence probe. chain={}, tokenAddress={}", chain, tokenAddress);
      return false;
    }
    try {
      String code = chainClientFactory.get(chain).getCode(tokenAddress);
      return StringUtils.hasText(code) && !"0x".equalsIgnoreCase(code);
    } catch (Exception e) {
      log.warn("Failed to probe token code. chain={}, tokenAddress={}, error={}",
          chain, tokenAddress, e.getMessage());
      return false;
    }
  }

  /** 探测代币是否支持 transferWithAuthorization。 */
  public boolean supportsTransferWithAuthorization(String chain, String tokenAddress, String walletAddress) {
    String owner = StringUtils.hasText(walletAddress) ? walletAddress : ZERO_ADDRESS;
    Function authorizationState = new Function(
        "authorizationState",
        List.of(new Address(owner), new Bytes32(ZERO_BYTES_32)),
        List.of(new TypeReference<Bool>() {
        }));
    return callLooksSupported(chain, tokenAddress, authorizationState);
  }

  /** 探测代币是否支持 permit。 */
  public boolean supportsPermit(String chain, String tokenAddress, String walletAddress) {
    String owner = StringUtils.hasText(walletAddress) ? walletAddress : ZERO_ADDRESS;
    Function nonces = new Function(
        "nonces",
        List.of(new Address(owner)),
        List.of(new TypeReference<Uint256>() {
        }));
    Function domainSeparator = new Function(
        "DOMAIN_SEPARATOR",
        List.of(),
        List.of(new TypeReference<Bytes32>() {
        }));
    return callLooksSupported(chain, tokenAddress, nonces)
        && callLooksSupported(chain, tokenAddress, domainSeparator);
  }

  /** 探测代币是否支持 approve。 */
  public boolean supportsApprove(String chain, String tokenAddress) {
    Function approve = new Function(
        "approve",
        List.of(new Address(ZERO_ADDRESS), new Uint256(BigInteger.ZERO)),
        List.of(new TypeReference<Bool>() {
        }));
    return callLooksSupported(chain, tokenAddress, approve);
  }

  /** 通过 eth_call 判断目标函数是否看起来可用。 */
  private boolean callLooksSupported(String chain, String tokenAddress, Function function) {
    if (!StringUtils.hasText(tokenAddress)) {
      return false;
    }
    if (ChainFamilyResolver.resolve(chain) != ChainFamily.EVM) {
      return false;
    }
    try {
      String data = FunctionEncoder.encode(function);
      String value = chainClientFactory.get(chain).ethCall(tokenAddress, data);
      return StringUtils.hasText(value) && !"0x".equalsIgnoreCase(value);
    } catch (Exception e) {
      log.debug("Token capability probe failed. chain={}, tokenAddress={}, function={}, error={}",
          chain, tokenAddress, function.getName(), e.getMessage());
      return false;
    }
  }
}
