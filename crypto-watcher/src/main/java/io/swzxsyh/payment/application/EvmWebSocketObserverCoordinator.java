package io.swzxsyh.payment.application;

import io.swzxsyh.payment.alert.PaymentAlertService;
import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.config.CryptoPaymentProperties.ChainProfile;
import io.swzxsyh.payment.scanner.RedisScannerCheckpointService;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.payment.util.RedisUtil;
import io.reactivex.disposables.Disposable;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.utils.Numeric;

/** EVM WebSocket 区块头监听协调器，负责多节点选主、续租、启动和关闭。 */
@Slf4j
@Service
public class EvmWebSocketObserverCoordinator {

  private final CryptoPaymentProperties properties;
  private final ChainClientFactory chainClientFactory;
  private final PaymentAlertService alertService;
  private final RedisScannerCheckpointService redisCheckpointService;
  private final RedisUtil redisUtil;
  private final PaymentInstanceIdentity instanceIdentity;
  private final Map<String, WebSocketObserverState> observers = new ConcurrentHashMap<>();

  public EvmWebSocketObserverCoordinator(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      PaymentAlertService alertService,
      RedisScannerCheckpointService redisCheckpointService,
      RedisUtil redisUtil,
      PaymentInstanceIdentity instanceIdentity) {
    this.properties = properties;
    this.chainClientFactory = chainClientFactory;
    this.alertService = alertService;
    this.redisCheckpointService = redisCheckpointService;
    this.redisUtil = redisUtil;
    this.instanceIdentity = instanceIdentity;
  }

  /** 确保当前链只有一个 payment 实例持有 WSS 监听权。 */
  public void maintain(ChainProfile profile) {
    CryptoPaymentProperties.Scanner scanner = properties.scannerForChain(profile.getChain());
    String chain = profile.getChain();
    if (!scanner.isWebsocketEnabled() || !StringUtils.hasText(profile.getWsUrl())) {
      close(chain, "websocket disabled or wsUrl empty", true);
      return;
    }
    if (!chainClientFactory.get(chain).isEvmFamily()) {
      return;
    }

    String leaderKey = leaderKey(chain);
    int leaseSeconds = Math.max(15, scanner.getWebsocketLeaderLeaseSeconds());
    String instanceId = instanceIdentity.instanceId();
    boolean leader =
        redisUtil.renewStringIfEquals(leaderKey, instanceId, leaseSeconds, TimeUnit.SECONDS)
            || redisUtil.trySetString(leaderKey, instanceId, leaseSeconds, TimeUnit.SECONDS);
    if (!leader) {
      if (observers.get(normalizeChain(chain)) != null) {
        close(chain, "websocket leader lease held by another instance", true);
      }
      log.debug(
          "当前实例不是 WSS leader，本轮不启动 WebSocket。chain={}, instanceId={}, currentLeader={}",
          chain,
          instanceId,
          redisUtil.getString(leaderKey));
      return;
    }

    WebSocketObserverState existing = observers.get(normalizeChain(chain));
    if (existing != null && existing.isActive()) {
      log.debug("WSS leader 租约续期成功。chain={}, instanceId={}, leaseSeconds={}", chain, instanceId, leaseSeconds);
      return;
    }
    start(profile);
  }

  private void start(ChainProfile profile) {
    try {
      if (!chainClientFactory.get(profile.getChain()).isEvmFamily()) {
        log.info("非 EVM 链暂不启动 WebSocket 区块头监听。chain={}, family={}",
            profile.getChain(), chainClientFactory.get(profile.getChain()).family());
        return;
      }
      WebSocketService wsService = new WebSocketService(profile.getWsUrl(), true);
      wsService.connect();
      Web3j web3j = Web3j.build(wsService);
      String chain = profile.getChain();
      String instanceId = instanceIdentity.instanceId();

      Disposable disposable =
          web3j
              .newHeadsNotifications()
              .subscribe(
                  notification -> {
                    if (!isCurrentLeader(chain)) {
                      close(chain, "websocket leader lease lost", true);
                      return;
                    }
                    if (notification == null
                        || notification.getParams() == null
                        || notification.getParams().getResult() == null
                        || !StringUtils.hasText(notification.getParams().getResult().getNumber())) {
                      return;
                    }
                    long height =
                        Numeric.toBigInt(notification.getParams().getResult().getNumber()).longValue();
                    redisCheckpointService.updateObservedBlock(chain, height);
                    log.debug("WebSocket 收到新区块头。chain={}, blockNumber={}, instanceId={}", chain, height, instanceId);
                  },
                  error -> {
                    log.warn(
                        "WebSocket 监听失败，将继续使用 RPC 扫描。chain={}, error={}",
                        chain,
                        error.getMessage(),
                        error);
                    alertService.alertWebSocketFailure(chain, error.getMessage());
                    close(chain, "websocket subscription error: " + error.getMessage(), true);
                  });

      observers.put(
          normalizeChain(chain),
          new WebSocketObserverState(chain, wsService, web3j, disposable, Instant.now()));
      log.info("WebSocket 监听已启动。chain={}, instanceId={}, wsUrl={}", chain, instanceId, profile.getWsUrl());
      alertService.alertWebSocketLeaderAcquired(chain, instanceId);
    } catch (Exception e) {
      if (isConfigurationOrProviderAccessError(e)) {
        log.warn("WebSocket 监听启动失败，疑似节点配置或供应商权限问题，将依赖 RPC 轮询。chain={}, error={}",
            profile.getChain(), e.getMessage());
        log.debug("WebSocket 启动失败堆栈。chain={}", profile.getChain(), e);
      } else {
        log.error("WebSocket 监听启动失败。chain={}, error={}", profile.getChain(), e.getMessage(), e);
      }
      alertService.alertWebSocketFailure(profile.getChain(), e.getMessage());
      redisUtil.deleteStringIfEquals(leaderKey(profile.getChain()), instanceIdentity.instanceId());
    }
  }

  private boolean isCurrentLeader(String chain) {
    return instanceIdentity.instanceId().equals(redisUtil.getString(leaderKey(chain)));
  }

  private String leaderKey(String chain) {
    return RedisKeyNamespace.websocketLeader(properties, chain);
  }

  private String normalizeChain(String chain) {
    return chain == null ? "" : chain.trim().toUpperCase();
  }

  private void close(String chain, String reason, boolean alert) {
    WebSocketObserverState state = observers.remove(normalizeChain(chain));
    if (state == null) {
      return;
    }
    try {
      if (state.disposable() != null && !state.disposable().isDisposed()) {
        state.disposable().dispose();
      }
    } catch (Exception ex) {
      log.debug("关闭 WebSocket 订阅时出现异常。chain={}, error={}", chain, ex.getMessage());
    }
    try {
      if (state.web3j() != null) {
        state.web3j().shutdown();
      }
    } catch (Exception ex) {
      log.debug("关闭 Web3j WebSocket 客户端时出现异常。chain={}, error={}", chain, ex.getMessage());
    }
    try {
      if (state.wsService() != null) {
        state.wsService().close();
      }
    } catch (Exception ex) {
      log.debug("关闭 WebSocketService 时出现异常。chain={}, error={}", chain, ex.getMessage());
    }
    boolean released = redisUtil.deleteStringIfEquals(leaderKey(chain), instanceIdentity.instanceId());
    log.info(
        "WebSocket 监听已关闭。chain={}, instanceId={}, reason={}, leaderKeyReleased={}",
        chain,
        instanceIdentity.instanceId(),
        reason,
        released);
    if (alert) {
      alertService.alertWebSocketLeaderReleased(chain, instanceIdentity.instanceId(), reason);
    }
  }

  @PreDestroy
  public void shutdown() {
    new ArrayList<>(observers.keySet())
        .forEach(chain -> close(chain, "payment instance shutdown", true));
  }

  private boolean isConfigurationOrProviderAccessError(Exception ex) {
    String message = ex.getMessage();
    return message != null
        && (message.contains("not enabled for this app")
            || message.contains("Invalid response received: 403")
            || message.contains("HTTP connect timed out")
            || message.contains("Failed to connect to WebSocket"));
  }

  private record WebSocketObserverState(
      String chain,
      WebSocketService wsService,
      Web3j web3j,
      Disposable disposable,
      Instant startedAt) {

    private boolean isActive() {
      return disposable != null && !disposable.isDisposed();
    }
  }
}
