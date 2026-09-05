package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.api.dto.TokenRoutePreviewRequest;
import io.swzxsyh.payment.api.dto.TokenRoutePreviewResponse;
import io.swzxsyh.payment.routing.TokenRoutePlanner;
import io.swzxsyh.payment.routing.TokenRouteRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/crypto/token-routing")
public class TokenRouteController {

  private final TokenRoutePlanner routePlanner;
  private final CashierProbeGuardService cashierProbeGuardService;

  public TokenRouteController(
      TokenRoutePlanner routePlanner,
      CashierProbeGuardService cashierProbeGuardService) {
    this.routePlanner = routePlanner;
    this.cashierProbeGuardService = cashierProbeGuardService;
  }

  @PostMapping("/preview")
  public ApiResponse<TokenRoutePreviewResponse> preview(@RequestBody TokenRoutePreviewRequest request) {
    cashierProbeGuardService.assertAllowed(request);
    TokenRoutePreviewResponse response = TokenRoutePreviewResponse.from(
        routePlanner.plan(new TokenRouteRequest(
            request.chain(),
            request.token(),
            request.tokenAddress(),
            request.walletAddress(),
            request.walletAccountType()
        )));
    log.info("Preview token route. chain={}, token={}, routeType={}",
        response.chain(), response.token(), response.routeType());
    return ApiResponse.ok(response);
  }
}
