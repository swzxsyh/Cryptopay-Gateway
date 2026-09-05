package io.swzxsyh.payment.solana;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentOrder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bitcoinj.core.Base58;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.programs.AssociatedTokenProgram;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 校验 Solana fee payer 即将签名的 message 是否只包含当前订单允许的支付动作。 */
@Service
public class SolanaFeePayerMessageValidator {

  private static final String SYSTEM_PROGRAM = "11111111111111111111111111111111";
  private static final String TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
  private static final String ASSOCIATED_TOKEN_PROGRAM = AssociatedTokenProgram.PROGRAM_ID.toBase58();
  private static final int SYSTEM_TRANSFER = 2;
  private static final int TOKEN_TRANSFER_CHECKED = 12;

  private final CryptoPaymentProperties properties;

  public SolanaFeePayerMessageValidator(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  /**
   * 白名单校验：fee payer、program、收款地址、token mint、金额都必须与订单一致。
   *
   * <p>这里校验的是待签名 message，不信任前端传入的任何交易内容。
   */
  public void validate(PaymentOrder order, String feePayerAddress, byte[] message) {
    if (order == null) {
      throw new IllegalArgumentException("Payment order is required for Solana fee payer validation");
    }
    if (!StringUtils.hasText(feePayerAddress)) {
      throw new IllegalArgumentException("Solana fee payer address is required");
    }
    SolanaMessage parsed = SolanaMessage.parse(message);
    if (parsed.accounts().isEmpty()) {
      throw new IllegalArgumentException("Solana message has no account keys");
    }
    if (!sameAddress(parsed.accounts().get(0), feePayerAddress)) {
      throw new IllegalArgumentException("Solana fee payer does not match platform fee payer");
    }

    String recipient = recipient(order);
    String mint = order.getTokenAddress();
    int decimals = tokenDecimals(order);
    BigInteger expectedAtomicAmount = toAtomicAmount(order.getAmount(), decimals);
    int paymentInstructionCount = 0;

    for (CompiledInstruction instruction : parsed.instructions()) {
      String programId = parsed.accountAt(instruction.programIdIndex());
      if (!isAllowedProgram(programId)) {
        throw new IllegalArgumentException("Solana message contains non-whitelisted program: " + programId);
      }
      if (sameAddress(programId, SYSTEM_PROGRAM)) {
        paymentInstructionCount += validateSystemTransfer(parsed, instruction, order, recipient, expectedAtomicAmount);
      } else if (isTokenProgram(programId)) {
        paymentInstructionCount += validateTokenTransferChecked(parsed, instruction, recipient, mint, decimals, expectedAtomicAmount, programId);
      } else if (sameAddress(programId, ASSOCIATED_TOKEN_PROGRAM)) {
        validateAssociatedTokenCreate(parsed, instruction, feePayerAddress, recipient, mint);
      }
    }

    if (paymentInstructionCount != 1) {
      throw new IllegalArgumentException("Solana message must contain exactly one order payment transfer");
    }
  }

  private int validateSystemTransfer(
      SolanaMessage message,
      CompiledInstruction instruction,
      PaymentOrder order,
      String recipient,
      BigInteger expectedLamports) {
    if (StringUtils.hasText(order.getTokenAddress()) || !"SOL".equalsIgnoreCase(order.getToken())) {
      return 0;
    }
    byte[] data = instruction.data();
    if (data.length != 12 || littleEndianU32(data, 0) != SYSTEM_TRANSFER) {
      return 0;
    }
    String destination = message.accountAt(instruction.accountIndex(1));
    BigInteger lamports = littleEndianU64(data, 4);
    if (!sameAddress(destination, recipient) || expectedLamports.compareTo(lamports) != 0) {
      throw new IllegalArgumentException("Solana native transfer does not match order recipient or amount");
    }
    return 1;
  }

  private int validateTokenTransferChecked(
      SolanaMessage message,
      CompiledInstruction instruction,
      String recipient,
      String mint,
      int decimals,
      BigInteger expectedAtomicAmount,
      String tokenProgramId) {
    byte[] data = instruction.data();
    if (data.length < 10 || Byte.toUnsignedInt(data[0]) != TOKEN_TRANSFER_CHECKED) {
      return 0;
    }
    if (instruction.accounts().size() < 4) {
      throw new IllegalArgumentException("Solana transferChecked instruction accounts are incomplete");
    }
    String instructionMint = message.accountAt(instruction.accountIndex(1));
    String destinationAta = message.accountAt(instruction.accountIndex(2));
    BigInteger amount = littleEndianU64(data, 1);
    int instructionDecimals = Byte.toUnsignedInt(data[9]);
    if (!sameAddress(instructionMint, mint)) {
      throw new IllegalArgumentException("Solana token mint does not match order token");
    }
    if (!sameAddress(destinationAta, associatedTokenAddress(recipient, mint, tokenProgramId))) {
      throw new IllegalArgumentException("Solana token destination ATA does not match order recipient");
    }
    if (instructionDecimals != decimals || expectedAtomicAmount.compareTo(amount) != 0) {
      throw new IllegalArgumentException("Solana token transfer amount or decimals does not match order");
    }
    return 1;
  }

  private void validateAssociatedTokenCreate(
      SolanaMessage message,
      CompiledInstruction instruction,
      String feePayerAddress,
      String recipient,
      String mint) {
    if (!StringUtils.hasText(mint)) {
      throw new IllegalArgumentException("Associated token account creation is only allowed for SPL token order");
    }
    if (instruction.accounts().size() < 4) {
      throw new IllegalArgumentException("Associated token account instruction accounts are incomplete");
    }
    String payer = message.accountAt(instruction.accountIndex(0));
    String ata = message.accountAt(instruction.accountIndex(1));
    String owner = message.accountAt(instruction.accountIndex(2));
    String instructionMint = message.accountAt(instruction.accountIndex(3));
    if (!sameAddress(payer, feePayerAddress)) {
      throw new IllegalArgumentException("Associated token account payer must be platform fee payer");
    }
    if (!sameAddress(owner, recipient) || !sameAddress(instructionMint, mint)) {
      throw new IllegalArgumentException("Associated token account target does not match order");
    }
    String expectedAta = associatedTokenAddress(recipient, mint, TOKEN_PROGRAM);
    if (!sameAddress(ata, expectedAta)) {
      throw new IllegalArgumentException("Associated token account address does not match order recipient and mint");
    }
  }

  private String associatedTokenAddress(String owner, String mint, String tokenProgramId) {
    PublicKey.ProgramDerivedAddress pda =
        PublicKey.findProgramAddress(
            List.of(
                Base58.decode(owner),
                Base58.decode(tokenProgramId),
                Base58.decode(mint)),
            AssociatedTokenProgram.PROGRAM_ID);
    return pda.getAddress().toBase58();
  }

  private boolean isAllowedProgram(String programId) {
    return sameAddress(programId, SYSTEM_PROGRAM)
        || sameAddress(programId, TOKEN_PROGRAM)
        || sameAddress(programId, ASSOCIATED_TOKEN_PROGRAM);
  }

  private boolean isTokenProgram(String programId) {
    return sameAddress(programId, TOKEN_PROGRAM);
  }

  private String recipient(PaymentOrder order) {
    String recipient =
        StringUtils.hasText(order.getPaymentAddress()) ? order.getPaymentAddress() : order.getContractAddress();
    if (!StringUtils.hasText(recipient)) {
      throw new IllegalArgumentException("Solana payment recipient is not prepared");
    }
    return recipient;
  }

  private int tokenDecimals(PaymentOrder order) {
    return properties.getTokenProfiles().stream()
        .filter(profile -> sameAddress(profile.getChain(), order.getChain()))
        .filter(profile -> sameAddress(profile.getToken(), order.getToken()))
        .filter(profile -> !StringUtils.hasText(order.getTokenAddress())
            || sameAddress(profile.getTokenAddress(), order.getTokenAddress()))
        .findFirst()
        .map(CryptoPaymentProperties.TokenProfile::getDecimals)
        .orElse(9);
  }

  private BigInteger toAtomicAmount(BigDecimal amount, int decimals) {
    if (amount == null) {
      throw new IllegalArgumentException("Solana order amount is required");
    }
    return amount.movePointRight(Math.max(0, decimals)).setScale(0, RoundingMode.UNNECESSARY).toBigIntegerExact();
  }

  private BigInteger littleEndianU64(byte[] data, int offset) {
    byte[] value = Arrays.copyOfRange(data, offset, offset + 8);
    reverse(value);
    return new BigInteger(1, value);
  }

  private int littleEndianU32(byte[] data, int offset) {
    return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
  }

  private void reverse(byte[] value) {
    for (int i = 0, j = value.length - 1; i < j; i++, j--) {
      byte tmp = value[i];
      value[i] = value[j];
      value[j] = tmp;
    }
  }

  private boolean sameAddress(String left, String right) {
    return StringUtils.hasText(left)
        && StringUtils.hasText(right)
        && left.trim().equalsIgnoreCase(right.trim());
  }

  private record SolanaMessage(
      int requiredSignatures,
      List<String> accounts,
      List<CompiledInstruction> instructions) {

    static SolanaMessage parse(byte[] message) {
      Cursor cursor = new Cursor(message);
      int prefix = cursor.readU8();
      if ((prefix & 0x80) != 0) {
        throw new IllegalArgumentException("Versioned Solana message is not allowed for platform fee payer");
      }
      int requiredSignatures = prefix;
      cursor.readU8();
      cursor.readU8();
      int accountCount = cursor.readShortVec();
      List<String> accounts = new ArrayList<>(accountCount);
      for (int i = 0; i < accountCount; i++) {
        accounts.add(Base58.encode(cursor.readBytes(32)));
      }
      cursor.readBytes(32);
      int instructionCount = cursor.readShortVec();
      List<CompiledInstruction> instructions = new ArrayList<>(instructionCount);
      for (int i = 0; i < instructionCount; i++) {
        int programIdIndex = cursor.readU8();
        int accountIndexCount = cursor.readShortVec();
        List<Integer> accountIndexes = new ArrayList<>(accountIndexCount);
        for (int j = 0; j < accountIndexCount; j++) {
          accountIndexes.add(cursor.readU8());
        }
        byte[] data = cursor.readBytes(cursor.readShortVec());
        instructions.add(new CompiledInstruction(programIdIndex, accountIndexes, data));
      }
      cursor.requireFullyRead();
      if (requiredSignatures < 2) {
        throw new IllegalArgumentException("Solana fee payer transaction requires platform and user signatures");
      }
      return new SolanaMessage(requiredSignatures, accounts, instructions);
    }

    String accountAt(int index) {
      if (index < 0 || index >= accounts.size()) {
        throw new IllegalArgumentException("Solana message account index out of range: " + index);
      }
      return accounts.get(index);
    }
  }

  private record CompiledInstruction(int programIdIndex, List<Integer> accounts, byte[] data) {
    int accountIndex(int index) {
      if (index < 0 || index >= accounts.size()) {
        throw new IllegalArgumentException("Solana instruction account index out of range: " + index);
      }
      return accounts.get(index);
    }
  }

  private static final class Cursor {
    private final byte[] data;
    private int offset;

    private Cursor(byte[] data) {
      this.data = data == null ? new byte[0] : data;
    }

    private int readU8() {
      if (offset >= data.length) {
        throw new IllegalArgumentException("Malformed Solana message");
      }
      return Byte.toUnsignedInt(data[offset++]);
    }

    private int readShortVec() {
      int value = 0;
      int shift = 0;
      for (int i = 0; i < 3; i++) {
        int b = readU8();
        value |= (b & 0x7f) << shift;
        if ((b & 0x80) == 0) {
          return value;
        }
        shift += 7;
      }
      throw new IllegalArgumentException("Malformed Solana shortvec length");
    }

    private byte[] readBytes(int length) {
      if (length < 0 || offset + length > data.length) {
        throw new IllegalArgumentException("Malformed Solana message length");
      }
      byte[] value = Arrays.copyOfRange(data, offset, offset + length);
      offset += length;
      return value;
    }

    private void requireFullyRead() {
      if (offset != data.length) {
        throw new IllegalArgumentException("Solana message has trailing bytes");
      }
    }
  }
}
