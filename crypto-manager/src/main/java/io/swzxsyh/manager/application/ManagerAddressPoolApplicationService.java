package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerAddressPoolDtos.BlockRequest;
import io.swzxsyh.manager.api.dto.ManagerAddressPoolDtos.OverviewResponse;
import io.swzxsyh.manager.api.dto.ManagerAddressPoolDtos.PolicyRequest;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.payment.channel.address.DerivedAddressInventoryService;
import io.swzxsyh.payment.channel.address.DerivedAddressPoolService;
import io.swzxsyh.payment.mapper.DerivedAddressPoolPolicyMapper;
import io.swzxsyh.payment.mapper.DerivedAddressPoolRecordMapper;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolPolicy;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolRecord;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理端地址池库存、策略和人工风控操作服务。 */
@Service
public class ManagerAddressPoolApplicationService extends ManagerApplicationSupport {

  private final DerivedAddressPoolRecordMapper addressPoolRecordMapper;
  private final DerivedAddressPoolPolicyMapper addressPoolPolicyMapper;
  private final DerivedAddressInventoryService addressInventoryService;
  private final ObjectProvider<DerivedAddressPoolService> addressPoolServiceProvider;

  public ManagerAddressPoolApplicationService(
      DerivedAddressPoolRecordMapper addressPoolRecordMapper,
      DerivedAddressPoolPolicyMapper addressPoolPolicyMapper,
      DerivedAddressInventoryService addressInventoryService,
      ObjectProvider<DerivedAddressPoolService> addressPoolServiceProvider) {
    this.addressPoolRecordMapper = addressPoolRecordMapper;
    this.addressPoolPolicyMapper = addressPoolPolicyMapper;
    this.addressInventoryService = addressInventoryService;
    this.addressPoolServiceProvider = addressPoolServiceProvider;
  }

  /** 分页查询地址池记录，支持池、链、币种、状态和风控标记筛选。 */
  public ManagerPageResponse<DerivedAddressPoolRecord> pageAddressRecords(
      long page, long size, String poolKey, String chain, String token, String status, Boolean riskFlag) {
    LambdaQueryWrapper<DerivedAddressPoolRecord> query =
        Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
            .eq(hasText(poolKey), DerivedAddressPoolRecord::getPoolKey, poolKey)
            .eq(hasText(chain), DerivedAddressPoolRecord::getChain, normalizeFilterCode(chain))
            .eq(hasText(token), DerivedAddressPoolRecord::getToken, normalizeFilterCode(token))
            .eq(hasText(status), DerivedAddressPoolRecord::getStatus, normalizeFilterCode(status))
            .eq(riskFlag != null, DerivedAddressPoolRecord::isRiskFlag, riskFlag)
            .orderByDesc(DerivedAddressPoolRecord::getUpdatedAt)
            .orderByDesc(DerivedAddressPoolRecord::getCreatedAt);
    return page(addressPoolRecordMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }

  /** 查询指定地址池库存概览，组合数据库状态和 Redis 队列状态。 */
  public OverviewResponse addressPoolOverview(String poolKey) {
    if (!hasText(poolKey)) {
      throw new IllegalArgumentException("poolKey is required");
    }
    DerivedAddressPoolRecord sample =
        addressPoolRecordMapper.selectOne(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
            .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
            .last("LIMIT 1"));
    DerivedAddressPoolService poolService = addressPoolServiceProvider.getIfAvailable();
    long redisAvailable = poolService == null ? 0 : poolService.getAvailableAddresses(poolKey).size();
    long redisLeased = poolService == null ? 0 : poolService.getLeasedCount(poolKey);
    return new OverviewResponse(
        poolKey,
        sample == null ? null : sample.getChain(),
        sample == null ? null : sample.getToken(),
        countAddress(poolKey, DerivedAddressPoolStatus.AVAILABLE.name()),
        countAddress(poolKey, DerivedAddressPoolStatus.LEASED.name()),
        countAddress(poolKey, DerivedAddressPoolStatus.RISK_BLOCKED.name()),
        countAddress(poolKey, DerivedAddressPoolStatus.RETIRED.name()),
        redisAvailable,
        redisLeased);
  }

  /** 查询可归集候选地址，自动排除风控冻结地址。 */
  public List<String> collectableAddresses(String poolKey, int limit) {
    return addressInventoryService.findCollectableAddresses(poolKey, Math.max(0, limit));
  }

  /** 人工释放地址租约，优先调用 core 地址池服务保持 Redis 与数据库一致。 */
  public boolean releaseAddressLease(String poolKey, String leaseId, String reason) {
    DerivedAddressPoolService poolService = addressPoolServiceProvider.getIfAvailable();
    if (poolService != null) {
      return poolService.release(poolKey, leaseId, reason);
    }
    return addressInventoryService.markReleased(poolKey, leaseId, reason) != null;
  }

  /** 人工冻结风险地址，优先调用 core 地址池服务摘除 Redis 中的可用地址。 */
  public boolean blockAddress(String poolKey, String address, BlockRequest request) {
    DerivedAddressPoolService poolService = addressPoolServiceProvider.getIfAvailable();
    String reason = request == null ? "manager manual block" : request.reason();
    String provider = request == null ? "MANAGER" : request.providerId();
    Integer riskScore = request == null ? null : request.riskScore();
    if (poolService != null) {
      return poolService.blockAddress(poolKey, address, reason, provider, riskScore);
    }
    addressInventoryService.markRiskBlocked(poolKey, address, null, provider, riskScore, reason);
    return true;
  }

  /** 人工退休地址，避免后续再次进入可用池。 */
  public DerivedAddressPoolRecord retireAddress(String poolKey, String address, String reason) {
    DerivedAddressPoolService poolService = addressPoolServiceProvider.getIfAvailable();
    if (poolService != null) {
      return poolService.retireAddress(poolKey, address, reason);
    }
    return addressInventoryService.markRetired(poolKey, address, reason);
  }

  /** 人工恢复地址为可用态，优先调用 core 地址池服务同步写入 Redis 可用队列。 */
  public DerivedAddressPoolRecord restoreAddress(String poolKey, String address, String reason) {
    DerivedAddressPoolService poolService = addressPoolServiceProvider.getIfAvailable();
    if (poolService != null) {
      return poolService.restoreAddress(poolKey, address, reason);
    }
    return addressInventoryService.markAvailableManually(poolKey, address, reason);
  }

  /** 分页查询地址池补池策略。 */
  public ManagerPageResponse<DerivedAddressPoolPolicy> pageAddressPolicies(long page, long size) {
    return page(addressPoolPolicyMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<DerivedAddressPoolPolicy>lambdaQuery().orderByDesc(DerivedAddressPoolPolicy::getUpdatedAt)));
  }

  /** 新增或更新地址池补池策略。 */
  @Transactional
  public DerivedAddressPoolPolicy saveAddressPolicy(PolicyRequest request) {
    if (request == null || !hasText(request.poolKey())) {
      throw new IllegalArgumentException("poolKey is required");
    }
    DerivedAddressPoolPolicy entity = new DerivedAddressPoolPolicy();
    entity.setId(request.id());
    entity.setPoolKey(request.poolKey());
    entity.setMinSize(request.minSize());
    entity.setEnabled(request.enabled());
    LocalDateTime now = LocalDateTime.now();
    entity.setUpdatedAt(now);
    if (entity.getId() == null) {
      entity.setCreatedAt(now);
      addressPoolPolicyMapper.insert(entity);
    } else {
      addressPoolPolicyMapper.updateById(entity);
    }
    return entity;
  }

  private long countAddress(String poolKey, String status) {
    return addressPoolRecordMapper.selectCount(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
        .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
        .eq(DerivedAddressPoolRecord::getStatus, status));
  }
}
