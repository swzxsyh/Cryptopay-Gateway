package io.swzxsyh.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 用户支付端启动入口。
 *
 * <p>加载 core 业务能力，仅排除后台管理侧控制器。
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("io.swzxsyh.payment.mapper")
@ComponentScan(
    basePackages = "io.swzxsyh.payment",
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.swzxsyh\\.manager\\.api\\..*Controller")
    })
public class PaymentApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentApplication.class, args);
  }
}
