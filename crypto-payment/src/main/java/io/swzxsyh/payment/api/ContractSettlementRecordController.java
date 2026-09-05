package io.swzxsyh.payment.api;

import io.swzxsyh.payment.settlement.record.ContractSettlementRecordService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 智能合约分账记录入口。 */
@RestController
@RequestMapping("/api/crypto/settlements/contracts")
public class ContractSettlementRecordController {

  private final ContractSettlementRecordService recordService;

  public ContractSettlementRecordController(ContractSettlementRecordService recordService) {
    this.recordService = recordService;
  }
}
