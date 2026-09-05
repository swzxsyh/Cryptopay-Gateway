package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.gateway.x402.Eip3009PreviewRequest;
import io.swzxsyh.payment.gateway.x402.Eip3009PreviewResponse;
import io.swzxsyh.payment.gateway.x402.X402DiscoveryDocument;
import io.swzxsyh.payment.gateway.x402.X402ProtocolService;
import io.swzxsyh.payment.gateway.x402.X402RequirementDocument;
import io.swzxsyh.payment.gateway.x402.X402SupportDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** X402 协议发现与 EIP-3009 预探测入口。 */
@Slf4j
@RestController
@RequestMapping("/api/crypto/x402")
public class X402ProtocolController {

  private final X402ProtocolService protocolService;

  public X402ProtocolController(
      X402ProtocolService protocolService) {
    this.protocolService = protocolService;
  }

  /** 返回协议发现文档。 */
  @GetMapping("/discovery")
  public ApiResponse<X402DiscoveryDocument> discovery() {
    return ApiResponse.ok(protocolService.discovery());
  }

  /** 返回协议支持能力。 */
  @GetMapping("/support")
  public ApiResponse<X402SupportDocument> support() {
    return ApiResponse.ok(protocolService.support());
  }

  /** 返回协议需求说明。 */
  @GetMapping("/requirements")
  public ApiResponse<X402RequirementDocument> requirements() {
    return ApiResponse.ok(protocolService.requirements());
  }

  /** 预探测 EIP-3009 可用性。 */
  @PostMapping("/eip3009/preview")
  public ApiResponse<Eip3009PreviewResponse> previewEip3009(@RequestBody Eip3009PreviewRequest request) {
    return ApiResponse.ok(protocolService.previewEip3009(request));
  }
}
