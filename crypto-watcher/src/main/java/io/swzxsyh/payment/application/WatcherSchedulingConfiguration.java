package io.swzxsyh.payment.application;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** watcher 模块定时任务线程池配置，避免单条链 RPC 超时阻塞其它链扫描。 */
@Configuration
public class WatcherSchedulingConfiguration {

  @Bean
  public TaskScheduler taskScheduler(CryptoPaymentProperties properties) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(Math.max(2, properties.getScanner().getSchedulerPoolSize()));
    scheduler.setThreadNamePrefix("watcher-scheduler-");
    scheduler.setWaitForTasksToCompleteOnShutdown(false);
    scheduler.initialize();
    return scheduler;
  }
}
