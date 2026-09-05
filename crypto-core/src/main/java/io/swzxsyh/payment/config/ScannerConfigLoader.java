package io.swzxsyh.payment.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.PaymentChainScannerConfigMapper;
import io.swzxsyh.payment.mapper.PaymentScannerConfigMapper;
import io.swzxsyh.payment.persistence.entity.PaymentChainScannerConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 全局和单链扫描配置加载器。 */
@Component
@Order(20)
public class ScannerConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentScannerConfigMapper scannerMapper;
  private final PaymentChainScannerConfigMapper chainScannerMapper;

  public ScannerConfigLoader(
      CryptoPaymentProperties properties,
      PaymentScannerConfigMapper scannerMapper,
      PaymentChainScannerConfigMapper chainScannerMapper) {
    this.properties = properties;
    this.scannerMapper = scannerMapper;
    this.chainScannerMapper = chainScannerMapper;
  }

  @Override
  public void apply() {
    applyGlobalScanner();
    applyChainScanners();
  }

  private void applyGlobalScanner() {
    first(scannerMapper.selectList(null))
        .ifPresent(
            config -> {
              CryptoPaymentProperties.Scanner scanner = properties.getScanner();
              setIfPresent(config.getConfirmationDepth(), scanner::setConfirmationDepth);
              setIfPresent(config.getScanIntervalSeconds(), scanner::setScanIntervalSeconds);
              setIfPresent(config.getCheckpointFlushBlocks(), scanner::setCheckpointFlushBlocks);
              setIfPresent(config.getBackfillBlocks(), scanner::setBackfillBlocks);
              setIfPresent(config.getLogScanBatchBlocks(), scanner::setLogScanBatchBlocks);
              setIfPresent(config.getLogScanRetryAttempts(), scanner::setLogScanRetryAttempts);
              setIfPresent(config.getFailureCooldownSeconds(), scanner::setFailureCooldownSeconds);
              setIfPresent(config.getWebsocketEnabled(), scanner::setWebsocketEnabled);
              setIfPresent(config.getWebsocketLeaderLeaseSeconds(), scanner::setWebsocketLeaderLeaseSeconds);
              setIfPresent(config.getChainReplayTtlHours(), scanner::setChainReplayTtlHours);
            });
  }

  private void applyChainScanners() {
    Map<String, CryptoPaymentProperties.Scanner> chainScanners = new LinkedHashMap<>();
    chainScannerMapper
        .selectList(
            Wrappers.<PaymentChainScannerConfig>lambdaQuery()
                .eq(PaymentChainScannerConfig::getEnabled, Boolean.TRUE)
                .orderByAsc(PaymentChainScannerConfig::getChainCode))
        .stream()
        .filter(config -> StringUtils.hasText(config.getChainCode()))
        .forEach(
            config -> {
              CryptoPaymentProperties.Scanner scanner =
                  CryptoPaymentProperties.Scanner.copyOf(properties.getScanner());
              setIfPresent(config.getConfirmationDepth(), scanner::setConfirmationDepth);
              setIfPresent(config.getScanIntervalSeconds(), scanner::setScanIntervalSeconds);
              setIfPresent(config.getCheckpointFlushBlocks(), scanner::setCheckpointFlushBlocks);
              setIfPresent(config.getBackfillBlocks(), scanner::setBackfillBlocks);
              setIfPresent(config.getLogScanBatchBlocks(), scanner::setLogScanBatchBlocks);
              setIfPresent(config.getLogScanRetryAttempts(), scanner::setLogScanRetryAttempts);
              setIfPresent(config.getFailureCooldownSeconds(), scanner::setFailureCooldownSeconds);
              setIfPresent(config.getWebsocketEnabled(), scanner::setWebsocketEnabled);
              setIfPresent(config.getWebsocketLeaderLeaseSeconds(), scanner::setWebsocketLeaderLeaseSeconds);
              setIfPresent(config.getChainReplayTtlHours(), scanner::setChainReplayTtlHours);
              chainScanners.put(config.getChainCode().trim().toUpperCase(), scanner);
            });
    properties.setChainScannerProfiles(chainScanners);
  }
}
