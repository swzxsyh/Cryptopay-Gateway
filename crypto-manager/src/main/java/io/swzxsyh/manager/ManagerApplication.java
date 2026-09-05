package io.swzxsyh.manager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 后台管理端启动入口。
 *
 * <p>加载 core 的配置、查询、持久化等后台可复用能力，排除支付端控制器、监听和链上确认编排。
 */
@SpringBootApplication
@EnableAsync
@MapperScan({"io.swzxsyh.payment.mapper", "io.swzxsyh.manager.security.mapper", "io.swzxsyh.manager.message.mapper"})
@ComponentScan(
    basePackages = {"io.swzxsyh.payment", "io.swzxsyh.manager"},
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.swzxsyh\\.payment\\.api\\..*Controller"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.swzxsyh\\.payment\\.gateway\\.api\\..*Controller"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.swzxsyh\\.payment\\.application\\..*"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.swzxsyh\\.payment\\.pending\\..*"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.swzxsyh\\.watcher\\.service\\..*")
    })
public class ManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ManagerApplication.class, args);
  }
}
