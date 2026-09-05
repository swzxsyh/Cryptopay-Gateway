package io.swzxsyh.payment.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 异步任务执行器配置。 */
@Configuration
public class AsyncConfig {

  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("swzxsyh-async-");
    executor.initialize();
    return executor;
  }

  /**
   * 商户回调 HTTP 投递专用虚拟线程执行器。
   *
   * <p>回调属于典型 I/O 等待场景，虚拟线程可以减少阻塞 HTTP 请求对平台线程的占用。
   */
  @Bean(name = "callbackTaskExecutor", destroyMethod = "close")
  public ExecutorService callbackTaskExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
