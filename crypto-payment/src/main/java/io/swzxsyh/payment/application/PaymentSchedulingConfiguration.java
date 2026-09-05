package io.swzxsyh.payment.application;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** payment 模块定时任务线程池配置，避免某条链 RPC 超时阻塞其它链扫描。 */
@Configuration
public class PaymentSchedulingConfiguration {

  @Bean
  public TaskScheduler taskScheduler(CryptoPaymentProperties properties) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(Math.max(2, properties.getScanner().getSchedulerPoolSize()));
    scheduler.setThreadNamePrefix("payment-scheduler-");
    scheduler.setWaitForTasksToCompleteOnShutdown(false);
    scheduler.initialize();
    return scheduler;
  }
}
