package io.swzxsyh.payment.application;

import io.swzxsyh.payment.config.CryptoPaymentProperties.ChainProfile;

/**
 * 单链监听策略接口。
 *
 * <p>payment application 只负责调度不同 watcher，具体的 EVM、Solana、TRON、SUI、TON
 * 扫描细节由各自实现类封装。
 */
public interface ChainWatcher {

  /** 判断当前 watcher 是否支持这条链。 */
  boolean supports(ChainProfile profile);

  /** 应用启动时执行链级初始化，例如 checkpoint 引导、WSS 区块头订阅。 */
  void bootstrap(ChainProfile profile);

  /** 定时执行确认块/确认 slot 扫描。 */
  void scan(ChainProfile profile);
}
