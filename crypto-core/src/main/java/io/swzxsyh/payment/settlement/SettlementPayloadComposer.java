package io.swzxsyh.payment.settlement;

import java.util.List;

public interface SettlementPayloadComposer {

  String composeSignPayload(ContractSettlementCommand command, List<SettlementSplit> splits);

  String composeContractCallData(
      ContractSettlementCommand command,
      List<SettlementSplit> splits,
      String signature);
}
