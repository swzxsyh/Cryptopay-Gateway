package io.swzxsyh.payment.routing;

import io.swzxsyh.payment.detector.WalletCapabilityDetector;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Token 支付路由规划器。 */
@Slf4j
@Service
public class TokenRoutePlanner {

  private final TokenCapabilityResolver capabilityResolver;
  private final WalletCapabilityDetector walletCapabilityDetector;

  public TokenRoutePlanner(
      TokenCapabilityResolver capabilityResolver,
      WalletCapabilityDetector walletCapabilityDetector) {
    this.capabilityResolver = capabilityResolver;
    this.walletCapabilityDetector = walletCapabilityDetector;
  }

  public TokenRoutePlan plan(TokenRouteRequest request) {
    WalletAccountType walletType =
        walletCapabilityDetector.detect(
            request.chain(), request.walletAddress(), request.walletAccountType());
    TokenRouteRequest effectiveRequest =
        new TokenRouteRequest(
            request.chain(),
            request.token(),
            request.tokenAddress(),
            request.walletAddress(),
            walletType);

    Optional<TokenCapabilityProfile> profileOpt = capabilityResolver.resolve(effectiveRequest);
    TokenCapabilityProfile profile =
        profileOpt.orElseGet(
            () ->
                new TokenCapabilityProfile(
                    request.chain(),
                    request.token(),
                    request.tokenAddress(),
                    6,
                    false,
                    false,
                    false,
                    true,
                    null));

    TokenRouteType routeType;
    String reason;
    boolean requiresUserAction;
    String payload;

    if (walletType == WalletAccountType.SCA && profile.smartContractSettlement()) {
      routeType = TokenRouteType.SMART_CONTRACT_SETTLEMENT;
      reason = "smart account detected; prefer smart contract settlement";
      requiresUserAction = true;
      payload = buildContractPayload(effectiveRequest, profile);
    } else if (profile.transferWithAuthorization() && walletType != WalletAccountType.SCA) {
      routeType = TokenRouteType.TRANSFER_WITH_AUTHORIZATION;
      reason = "token contract supports transferWithAuthorization; wallet must sign authorization";
      requiresUserAction = true;
      payload = buildAuthorizationPayload(effectiveRequest, profile);
    } else if (profile.permit() && walletType != WalletAccountType.SCA) {
      routeType = TokenRouteType.PERMIT_SIGNATURE;
      reason = "token contract supports permit";
      requiresUserAction = true;
      payload = buildPermitPayload(effectiveRequest, profile);
    } else if (profile.approve()) {
      routeType = TokenRouteType.USER_APPROVE;
      reason = "token contract requires wallet approve + transferFrom";
      requiresUserAction = true;
      payload = buildApprovePayload(effectiveRequest, profile);
    } else {
      routeType = TokenRouteType.SMART_CONTRACT_SETTLEMENT;
      reason = "fallback to smart contract settlement";
      requiresUserAction = true;
      payload = buildContractPayload(effectiveRequest, profile);
    }

    log.info(
        "Planned token route. chain={}, token={}, tokenAddress={}, routeType={}, walletType={}",
        request.chain(),
        request.token(),
        request.tokenAddress(),
        routeType,
        walletType);

    return new TokenRoutePlan(
        request.chain(),
        request.token(),
        profile.tokenAddress(),
        request.walletAddress(),
        walletType,
        routeType,
        reason,
        requiresUserAction,
        payload);
  }

  private String buildAuthorizationPayload(
      TokenRouteRequest request, TokenCapabilityProfile profile) {
    return "transferWithAuthorization(token="
        + request.token()
        + ", tokenAddress="
        + profile.tokenAddress()
        + ", chain="
        + request.chain()
        + ")";
  }

  private String buildPermitPayload(TokenRouteRequest request, TokenCapabilityProfile profile) {
    return "permit(token="
        + request.token()
        + ", tokenAddress="
        + profile.tokenAddress()
        + ", chain="
        + request.chain()
        + ")";
  }

  private String buildApprovePayload(TokenRouteRequest request, TokenCapabilityProfile profile) {
    return "approveThenTransferFrom(token="
        + request.token()
        + ", tokenAddress="
        + profile.tokenAddress()
        + ", chain="
        + request.chain()
        + ")";
  }

  private String buildContractPayload(TokenRouteRequest request, TokenCapabilityProfile profile) {
    String contractAddress =
        StringUtils.hasText(profile.settlementContractAddress())
            ? profile.settlementContractAddress()
            : "0x0000000000000000000000000000000000000000";
    return "smartContractSettlement(token="
        + request.token()
        + ", tokenAddress="
        + profile.tokenAddress()
        + ", contractAddress="
        + contractAddress
        + ", chain="
        + request.chain()
        + ")";
  }
}
