package io.swzxsyh.payment.config;

/** 结构化配置加载贡献者，每个实现只负责一组配置表到运行时配置的映射。 */
public interface PaymentConfigContributor {

  /** 从数据库读取本组配置并写入 CryptoPaymentProperties。 */
  void apply();
}
