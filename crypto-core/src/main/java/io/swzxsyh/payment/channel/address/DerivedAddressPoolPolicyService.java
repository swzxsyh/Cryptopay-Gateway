package io.swzxsyh.payment.channel.address;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.DerivedAddressPoolPolicyMapper;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolPolicy;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 派生地址池策略服务，用于从数据库读取最小池大小。 */
@Slf4j
@Service
public class DerivedAddressPoolPolicyService {

  private static final int DEFAULT_MIN_SIZE = 3;
  private static final String GLOBAL_POOL_KEY = "GLOBAL";

  private final DerivedAddressPoolPolicyMapper mapper;

  public DerivedAddressPoolPolicyService(DerivedAddressPoolPolicyMapper mapper) {
    this.mapper = mapper;
  }

  public int resolveMinSize(String poolKey) {
    DerivedAddressPoolPolicy policy = null;
    if (StringUtils.hasText(poolKey)) {
      policy = mapper.selectOne(Wrappers.<DerivedAddressPoolPolicy>lambdaQuery()
          .eq(DerivedAddressPoolPolicy::getPoolKey, poolKey));
    }
    if (policy == null) {
      policy = mapper.selectOne(Wrappers.<DerivedAddressPoolPolicy>lambdaQuery()
          .eq(DerivedAddressPoolPolicy::getPoolKey, GLOBAL_POOL_KEY));
    }
    if (policy == null || policy.getMinSize() == null || policy.getMinSize() <= 0) {
      return DEFAULT_MIN_SIZE;
    }
    return policy.getMinSize();
  }

  public DerivedAddressPoolPolicy savePolicy(String poolKey, int minSize, boolean enabled) {
    DerivedAddressPoolPolicy policy = mapper.selectOne(Wrappers.<DerivedAddressPoolPolicy>lambdaQuery()
        .eq(DerivedAddressPoolPolicy::getPoolKey, poolKey));
    if (policy == null) {
      policy = new DerivedAddressPoolPolicy();
      policy.setPoolKey(poolKey);
      policy.setCreatedAt(LocalDateTime.now());
    }
    policy.setMinSize(Math.max(1, minSize));
    policy.setEnabled(enabled);
    policy.setUpdatedAt(LocalDateTime.now());
    if (policy.getId() == null) {
      mapper.insert(policy);
    } else {
      mapper.updateById(policy);
    }
    log.info("Saved derived address pool policy. poolKey={}, minSize={}, enabled={}",
        policy.getPoolKey(), policy.getMinSize(), policy.getEnabled());
    return policy;
  }
}
