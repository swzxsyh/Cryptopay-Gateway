package io.swzxsyh.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** Redisson 客户端配置。 */
@Configuration
public class RedissonConfiguration {

  @Bean(destroyMethod = "shutdown")
  public RedissonClient redissonClient(CryptoPaymentProperties properties, ObjectMapper objectMapper) {
    if (properties.getRedis() == null || !StringUtils.hasText(properties.getRedis().getUrl())) {
      throw new IllegalStateException("crypto.payment.redis.url is required");
    }
    Config config = new Config();
    config.setCodec(new JsonJacksonCodec(objectMapper.copy()));
    config.useSingleServer()
        .setAddress(properties.getRedis().getUrl())
        .setDatabase(properties.getRedis().getDatabase());
    if (properties.getRedis().getPassword() != null && !properties.getRedis().getPassword().isBlank()) {
      config.useSingleServer().setPassword(properties.getRedis().getPassword());
    }
    return Redisson.create(config);
  }
}
