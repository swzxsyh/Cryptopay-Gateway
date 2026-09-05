package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.kyt.KytScreeningRequest;
import io.swzxsyh.payment.kyt.KytScreeningResult;
import io.swzxsyh.payment.kyt.KytScreeningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/crypto/kyt")
public class KytScreeningController {

  private final KytScreeningService screeningService;
  private final CashierProbeGuardService cashierProbeGuardService;

  public KytScreeningController(
      KytScreeningService screeningService,
      CashierProbeGuardService cashierProbeGuardService) {
    this.screeningService = screeningService;
    this.cashierProbeGuardService = cashierProbeGuardService;
  }

  @PostMapping("/screen")
  public ApiResponse<KytScreeningResult> screen(@RequestBody KytScreeningRequest request) {
    cashierProbeGuardService.assertAllowed(request);
    KytScreeningResult result = screeningService.screen(request);
    log.info("KYT screen request handled. cryptoOrderNo={}, decision={}, riskScore={}",
        request.cryptoOrderNo(), result.decision(), result.riskScore());
    return ApiResponse.ok(result);
  }
}
