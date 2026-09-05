package io.swzxsyh.payment.gas.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.api.CashierProbeGuardService;
import io.swzxsyh.payment.gas.GasPolicyPlanner;
import io.swzxsyh.payment.gas.GasPolicyRequest;
import io.swzxsyh.payment.gas.GasPolicyResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gas 预探测接口。 */
@Slf4j
@RestController
@RequestMapping("/api/crypto/gas")
public class GasPreviewController {

  private final GasPolicyPlanner planner;
  private final CashierProbeGuardService cashierProbeGuardService;

  public GasPreviewController(
      GasPolicyPlanner planner,
      CashierProbeGuardService cashierProbeGuardService) {
    this.planner = planner;
    this.cashierProbeGuardService = cashierProbeGuardService;
  }

  /** 预估 Gas 策略。 */
  @PostMapping("/preview")
  public ApiResponse<GasPolicyResult> preview(@RequestBody GasPolicyRequest request) {
    cashierProbeGuardService.assertAllowed(request);
    GasPolicyResult result = planner.plan(request);
    log.info("Gas preview handled. chain={}, token={}, payerMode={}, fallback={}",
        request.chain(), request.token(), result.payerMode(), result.fallbackSuggestion());
    return ApiResponse.ok(result);
  }
}
