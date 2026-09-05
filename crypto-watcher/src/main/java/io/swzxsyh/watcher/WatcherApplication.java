package io.swzxsyh.watcher;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 链上监听端启动入口。
 *
 * <p>只负责 WSS/RPC 监听、确认块扫描、原始链上事件解析和消息发布；订单入账、余额、回调由
 * crypto-payment 消费消息后处理。
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("io.swzxsyh.payment.mapper")
@ComponentScan("io.swzxsyh.payment")
public class WatcherApplication {

  public static void main(String[] args) {
    SpringApplication.run(WatcherApplication.class, args);
  }
}
