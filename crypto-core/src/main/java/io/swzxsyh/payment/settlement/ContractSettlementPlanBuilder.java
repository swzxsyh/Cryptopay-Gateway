package io.swzxsyh.payment.settlement;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 智能合约分账计划构建器，用于把分账规则拼装成可执行计划。 */
@Slf4j
@Service
public class ContractSettlementPlanBuilder {

  private final SettlementPayloadComposer payloadComposer;

  public ContractSettlementPlanBuilder(SettlementPayloadComposer payloadComposer) {
    this.payloadComposer = payloadComposer;
  }

  /** 根据分账命令构建最终的分账计划。 */
  public ContractSettlementPlan build(ContractSettlementCommand command) {
    validate(command);

    List<SettlementSplit> splits = command.settlementRules().stream()
        .map(rule -> SettlementSplit.of(
            rule.role(),
            rule.receiver(),
            rule.basisPoints(),
            command.amount()))
        .sorted(Comparator.comparing(SettlementSplit::role))
        .toList();

    String signPayload = payloadComposer.composeSignPayload(command, splits);
    String signature = null;
    String contractCallData = payloadComposer.composeContractCallData(command, splits, signature);

    log.info("Built contract settlement plan. cryptoOrderNo={}, token={}, amount={}, splits={}",
        command.cryptoOrderNo(), command.token(), command.amount(), splits.size());

    return new ContractSettlementPlan(
        command.cryptoOrderNo(),
        command.chain(),
        command.token(),
        command.contractAddress(),
        command.amount(),
        command.expireTime(),
        splits,
        signPayload,
        signature,
        contractCallData
    );
  }

  /** 校验分账命令的基础字段与分账比例。 */
  private void validate(ContractSettlementCommand command) {
    if (!StringUtils.hasText(command.cryptoOrderNo())) {
      throw new IllegalArgumentException("cryptoOrderNo is required");
    }
    if (!StringUtils.hasText(command.token())) {
      throw new IllegalArgumentException("token is required");
    }
    if (!StringUtils.hasText(command.tokenAddress())) {
      throw new IllegalArgumentException("tokenAddress is required");
    }
    if (command.tokenDecimals() < 0) {
      throw new IllegalArgumentException("tokenDecimals must be greater than or equal to zero");
    }
    if (!StringUtils.hasText(command.contractAddress())) {
      throw new IllegalArgumentException("contractAddress is required");
    }
    if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be greater than zero");
    }
    if (command.settlementRules() == null || command.settlementRules().isEmpty()) {
      throw new IllegalArgumentException("settlementRules is required");
    }

    int totalBasisPoints = command.settlementRules().stream()
        .mapToInt(SettlementRule::basisPoints)
        .sum();
    if (totalBasisPoints != 10000) {
      throw new IllegalArgumentException("settlement basisPoints must equal 10000");
    }

    List<String> emptyReceivers = command.settlementRules().stream()
        .filter(rule -> !StringUtils.hasText(rule.receiver()))
        .map(SettlementRule::role)
        .toList();
    if (!emptyReceivers.isEmpty()) {
      throw new IllegalArgumentException("settlement receiver is required for roles: " + emptyReceivers);
    }
  }
}
