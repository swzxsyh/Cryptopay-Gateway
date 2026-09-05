package io.swzxsyh.payment.application;

import io.swzxsyh.payment.alert.PaymentAlertService;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.config.CryptoPaymentProperties.ChainProfile;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 链监听应用层调度器，统一编排不同链 watcher 策略。 */
@Slf4j
@Service
public class ChainWatcherApplicationService {

  private final CryptoPaymentProperties properties;
  private final List<ChainWatcher> watchers;
  private final PaymentAlertService alertService;

  public ChainWatcherApplicationService(
      CryptoPaymentProperties properties,
      List<ChainWatcher> watchers,
      PaymentAlertService alertService) {
    this.properties = properties;
    this.watchers = watchers;
    this.alertService = alertService;
  }

  /** 应用启动后初始化所有已启用链的 watcher。 */
  @EventListener(ApplicationReadyEvent.class)
  public void bootstrapWatchers() {
    if (properties.getChainProfiles().isEmpty()) {
      log.warn("未配置任何链路，区块监听将保持空闲。");
      alertService.alertMissingChainProfiles();
      return;
    }
    log.info(
        "开始初始化链监听策略。chainCount={}, watcherCount={}, defaultScanIntervalSeconds={}",
        properties.getChainProfiles().size(),
        watchers.size(),
        properties.getScanner().getScanIntervalSeconds());
    for (ChainProfile profile : properties.getChainProfiles()) {
      if (!isEnabledProfile(profile)) {
        continue;
      }
      List<ChainWatcher> matchedWatchers = matchingWatchers(profile);
      if (matchedWatchers.isEmpty()) {
        log.warn("没有可用 watcher 支持当前链，链监听不会启动。chain={}", profile.getChain());
        continue;
      }
      matchedWatchers.forEach(watcher -> watcher.bootstrap(profile));
    }
    log.info("链监听策略初始化完成。chainCount={}", properties.getChainProfiles().size());
  }

  /** 定时扫描各链确认区块或确认 slot。 */
  @Scheduled(fixedDelayString = "#{@cryptoPaymentProperties.scanner.scanIntervalSeconds * 1000}")
  public void scanWatchers() {
    for (ChainProfile profile : properties.getChainProfiles()) {
      if (!isEnabledProfile(profile)) {
        continue;
      }
      matchingWatchers(profile).forEach(watcher -> watcher.scan(profile));
    }
  }

  private List<ChainWatcher> matchingWatchers(ChainProfile profile) {
    return watchers.stream().filter(watcher -> watcher.supports(profile)).toList();
  }

  private boolean isEnabledProfile(ChainProfile profile) {
    return profile != null && profile.isEnabled() && StringUtils.hasText(profile.getChain());
  }
}
