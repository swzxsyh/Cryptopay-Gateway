package io.swzxsyh.payment.channel.address;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolRecord;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolStatus;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.payment.util.RedisUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 派生地址池服务，负责数据库库存、Redis 队列和租约状态的一致性。 */
@Slf4j
@Service
@ConditionalOnBean(RedisUtil.class)
public class DerivedAddressPoolService {

  private final CryptoPaymentProperties properties;
  private final RedisUtil redisUtil;
  private final LockUtil lockUtil;
  private final ObjectProvider<DerivedAddressProvisioner> provisionerProvider;
  private final DerivedAddressInventoryService inventoryService;
  private final DerivedAddressPoolPolicyService policyService;

  public DerivedAddressPoolService(
      CryptoPaymentProperties properties,
      RedisUtil redisUtil,
      LockUtil lockUtil,
      ObjectProvider<DerivedAddressProvisioner> provisionerProvider,
      DerivedAddressInventoryService inventoryService,
      DerivedAddressPoolPolicyService policyService) {
    this.properties = properties;
    this.redisUtil = redisUtil;
    this.lockUtil = lockUtil;
    this.provisionerProvider = provisionerProvider;
    this.inventoryService = inventoryService;
    this.policyService = policyService;
  }

  /** 从地址池租用一个可用地址。 */
  public DerivedAddressLease lease(String chain, String token, String orderNo) {
    String poolKey = poolKey(chain, token);
    String queueKey = queueKey(poolKey);
    String leaseKey = leaseKey(poolKey);
    String lockKey = lockKey(poolKey);

    return lockUtil.withLock(lockKey,
        properties.getRedis().getLockWaitMillis(),
        properties.getRedis().getLockLeaseSeconds(),
        () -> {
          ensureMinimumPool(poolKey, chain, token, orderNo);

          String address = null;
          String leaseId = null;
          boolean autoCreated = false;
          for (int i = 0; i < 16 && !StringUtils.hasText(address); i++) {
            String candidate = pollNextAvailableAddress(poolKey, queueKey);
            if (!StringUtils.hasText(candidate)) {
              break;
            }
            String candidateLeaseId = UUID.randomUUID().toString();
            try {
              inventoryService.markLeased(poolKey, candidateLeaseId, orderNo, candidate);
              address = candidate;
              leaseId = candidateLeaseId;
            } catch (Exception ex) {
              log.warn("Failed to lease derived address, skip candidate. poolKey={}, address={}, error={}",
                  poolKey, candidate, ex.getMessage());
            }
          }
          if (!StringUtils.hasText(address)) {
            address = createAndStoreAddress(poolKey, chain, token, orderNo);
            autoCreated = true;
            leaseId = UUID.randomUUID().toString();
            inventoryService.markLeased(poolKey, leaseId, orderNo, address);
          }
          if (!StringUtils.hasText(leaseId)) {
            leaseId = UUID.randomUUID().toString();
            inventoryService.markLeased(poolKey, leaseId, orderNo, address);
          }
          DerivedAddressLease lease = new DerivedAddressLease(
              poolKey,
              leaseId,
              address,
              orderNo,
              autoCreated,
              LocalDateTime.now(),
              LocalDateTime.now().plusMinutes(properties.getRedis().getLeaseMinutes())
          );
          redisUtil.putWithLease(leaseKey, lease.leaseId(), lease.address(),
              properties.getRedis().getLeaseMinutes(), java.util.concurrent.TimeUnit.MINUTES);
          log.info("Leased derived address. poolKey={}, address={}, leaseId={}, orderNo={}",
              poolKey, address, lease.leaseId(), orderNo);
          ensureMinimumPool(poolKey, chain, token, orderNo);
          return lease;
        });
  }

  /** 释放已租用的地址，若地址已被风控则不会重新入池。 */
  public boolean release(String poolKey, String leaseId, String reason) {
    return release(poolKey, leaseId, reason, null);
  }

  /** 释放已租用的地址，并记录最近一次链上交易哈希用于复用审计。 */
  public boolean release(String poolKey, String leaseId, String reason, String lastTxHash) {
    if (!StringUtils.hasText(poolKey) || !StringUtils.hasText(leaseId)) {
      return false;
    }

    String leaseKey = leaseKey(poolKey);
    String queueKey = queueKey(poolKey);
    String lockKey = lockKey(poolKey);

    return lockUtil.withLock(lockKey,
        properties.getRedis().getLockWaitMillis(),
        properties.getRedis().getLockLeaseSeconds(),
        () -> {
          String address = redisUtil.remove(leaseKey, leaseId);

          boolean reuseAllowed = properties.getDerivedAddress().isReuseAddress();
          int cooldownMinutes = Math.max(0, properties.getDerivedAddress().getReuseCooldownMinutes());
          DerivedAddressPoolRecord record =
              inventoryService.markReleased(poolKey, leaseId, reason, reuseAllowed, cooldownMinutes, lastTxHash);
          if (record == null) {
            return false;
          }
          if (!StringUtils.hasText(address)) {
            address = record.getAddress();
            log.debug("Redis lease already missing, release derived address by database lease. poolKey={}, address={}, leaseId={}",
                poolKey, address, leaseId);
          }
          if (record != null && DerivedAddressPoolStatus.AVAILABLE.name().equals(record.getStatus())
              && !record.isRiskFlag()) {
            redisUtil.addLastIfAbsent(queueKey, address);
          } else if (record != null && DerivedAddressPoolStatus.COOLDOWN.name().equals(record.getStatus())) {
            log.info(
                "Derived address enters cooldown before reuse. poolKey={}, address={}, leaseId={}, cooldownUntil={}",
                poolKey,
                address,
                leaseId,
                record.getCooldownUntil());
          }
          log.info("Released derived address. poolKey={}, address={}, leaseId={}, reason={}, blocked={}",
              poolKey, address, leaseId, reason, record != null && record.isRiskFlag());
          ensureMinimumPool(poolKey, null, null, null);
          return true;
        });
  }

  /** 将某个地址标记为风控命中并立即从池中摘除。 */
  public boolean blockAddress(String poolKey, String address, String reason, String providerId, Integer riskScore) {
    if (!StringUtils.hasText(poolKey) || !StringUtils.hasText(address)) {
      return false;
    }

    String queueKey = queueKey(poolKey);
    String leaseKey = leaseKey(poolKey);
    String lockKey = lockKey(poolKey);

    return lockUtil.withLock(lockKey,
        properties.getRedis().getLockWaitMillis(),
        properties.getRedis().getLockLeaseSeconds(),
        () -> {
          DerivedAddressPoolRecord current = inventoryService.findByPoolKeyAndAddress(poolKey, address).orElse(null);
          String existingLeaseId = current == null ? null : current.getLeaseId();
          DerivedAddressPoolRecord record = inventoryService.markRiskBlocked(
              poolKey, address, null, providerId, riskScore, reason);
          redisUtil.removeValue(queueKey, address);
          if (StringUtils.hasText(existingLeaseId)) {
            redisUtil.remove(leaseKey, existingLeaseId);
          }
          log.warn("Blocked derived address by KYT. poolKey={}, address={}, provider={}, riskScore={}, reason={}",
              poolKey, address, providerId, riskScore, reason);
          return true;
        });
  }

  /** 人工退休地址，并同步从 Redis 可用队列和租约缓存中摘除。 */
  public DerivedAddressPoolRecord retireAddress(String poolKey, String address, String reason) {
    if (!StringUtils.hasText(poolKey) || !StringUtils.hasText(address)) {
      return null;
    }

    String queueKey = queueKey(poolKey);
    String leaseKey = leaseKey(poolKey);
    String lockKey = lockKey(poolKey);

    return lockUtil.withLock(
        lockKey,
        properties.getRedis().getLockWaitMillis(),
        properties.getRedis().getLockLeaseSeconds(),
        () -> {
          DerivedAddressPoolRecord current =
              inventoryService.findByPoolKeyAndAddress(poolKey, address).orElse(null);
          if (current == null) {
            return null;
          }
          redisUtil.removeValue(queueKey, address);
          if (StringUtils.hasText(current.getLeaseId())) {
            redisUtil.remove(leaseKey, current.getLeaseId());
          }
          DerivedAddressPoolRecord retired = inventoryService.markRetired(poolKey, address, reason);
          log.info("Retired derived address and removed it from Redis pool. poolKey={}, address={}, reason={}",
              poolKey, address, reason);
          ensureMinimumPool(poolKey, current.getChain(), current.getToken(), "manager-retire");
          return retired;
        });
  }

  /** 人工恢复地址为可用态，并同步写入 Redis 可用队列。 */
  public DerivedAddressPoolRecord restoreAddress(String poolKey, String address, String reason) {
    if (!StringUtils.hasText(poolKey) || !StringUtils.hasText(address)) {
      return null;
    }

    String queueKey = queueKey(poolKey);
    String leaseKey = leaseKey(poolKey);
    String lockKey = lockKey(poolKey);

    return lockUtil.withLock(
        lockKey,
        properties.getRedis().getLockWaitMillis(),
        properties.getRedis().getLockLeaseSeconds(),
        () -> {
          DerivedAddressPoolRecord current =
              inventoryService.findByPoolKeyAndAddress(poolKey, address).orElse(null);
          if (current == null) {
            return null;
          }
          if (DerivedAddressPoolStatus.LEASED.name().equals(current.getStatus())) {
            throw new IllegalStateException("leased derived address cannot be restored directly: " + address);
          }
          if (StringUtils.hasText(current.getLeaseId())) {
            redisUtil.remove(leaseKey, current.getLeaseId());
          }
          DerivedAddressPoolRecord restored =
              inventoryService.markAvailableManually(poolKey, address, reason);
          if (restored != null && !restored.isRiskFlag()) {
            redisUtil.addLastIfAbsent(queueKey, restored.getAddress());
          }
          log.info("Restored derived address to Redis available pool. poolKey={}, address={}, reason={}",
              poolKey, address, reason);
          return restored;
        });
  }

  public List<String> getAvailableAddresses(String poolKey) {
    return redisUtil.readAllStrings(queueKey(poolKey));
  }

  /** 获取当前可用于归集传递的地址列表。 */
  public List<String> getCollectableAddresses(String poolKey) {
    return inventoryService.findCollectableAddresses(poolKey, 0);
  }

  public long getLeasedCount(String poolKey) {
    return redisUtil.mapSize(leaseKey(poolKey));
  }

  public String poolKey(String chain, String token) {
    String safeChain = StringUtils.hasText(chain) ? chain.toUpperCase() : "UNKNOWN_CHAIN";
    String safeToken = StringUtils.hasText(token) ? token.toUpperCase() : "UNKNOWN_TOKEN";
    return RedisKeyNamespace.derivedAddressPool(properties, safeChain, safeToken);
  }

  private void ensureMinimumPool(String poolKey, String chain, String token, String orderNo) {
    int minSize = Math.max(1, policyService.resolveMinSize(poolKey));
    promoteCooldownExpired(poolKey);
    long currentAvailable = inventoryService.countAvailable(poolKey);
    if (currentAvailable < minSize) {
      if (!StringUtils.hasText(chain) || !StringUtils.hasText(token)) {
        List<DerivedAddressPoolRecord> availableRecords = inventoryService.findAvailable(poolKey, 0);
        String queueKey = queueKey(poolKey);
        for (DerivedAddressPoolRecord record : availableRecords) {
          if (!record.isRiskFlag() && !DerivedAddressPoolStatus.RISK_BLOCKED.name().equals(record.getStatus())) {
            redisUtil.addLastIfAbsent(queueKey, record.getAddress());
          }
        }
        return;
      }
      int needed = Math.max(1, minSize - (int) currentAvailable);
      List<String> created = createAndStoreAddresses(poolKey, chain, token, needed, orderNo);
      log.info("Refilled derived address inventory. poolKey={}, createdCount={}, targetMin={}",
          poolKey, created.size(), minSize);
    }

    List<DerivedAddressPoolRecord> availableRecords = inventoryService.findAvailable(poolKey, 0);
    String queueKey = queueKey(poolKey);
    for (DerivedAddressPoolRecord record : availableRecords) {
      if (!record.isRiskFlag() && !DerivedAddressPoolStatus.RISK_BLOCKED.name().equals(record.getStatus())) {
        redisUtil.addLastIfAbsent(queueKey, record.getAddress());
      }
    }

    if (redisUtil.deque(queueKey).size() < minSize && availableRecords.size() < minSize) {
      int needed = minSize - availableRecords.size();
      createAndStoreAddresses(poolKey, chain, token, needed, orderNo);
      availableRecords = inventoryService.findAvailable(poolKey, 0);
      for (DerivedAddressPoolRecord record : availableRecords) {
        if (!record.isRiskFlag() && !DerivedAddressPoolStatus.RISK_BLOCKED.name().equals(record.getStatus())) {
          redisUtil.addLastIfAbsent(queueKey, record.getAddress());
        }
      }
    }
  }

  private List<String> createAndStoreAddresses(String poolKey, String chain, String token, int count, String orderNo) {
    if (count <= 0) {
      return List.of();
    }
    DerivedAddressProvisioner provisioner = provisionerProvider.getIfAvailable();
    if (provisioner == null) {
      throw new IllegalStateException("DerivedAddressProvisioner is required to refill derived address pool");
    }
    List<String> created = provisioner.createAddresses(chain, token, count, orderNo);
    if (created == null || created.isEmpty()) {
      throw new IllegalStateException("DerivedAddressProvisioner returned no address");
    }
    for (String address : created) {
      if (!StringUtils.hasText(address)) {
        continue;
      }
      inventoryService.saveGeneratedAddress(poolKey, chain, token, address, resolveMode(), orderNo);
    }
    return created;
  }

  private String createAndStoreAddress(String poolKey, String chain, String token, String orderNo) {
    List<String> created = createAndStoreAddresses(poolKey, chain, token, 1, orderNo);
    if (created.isEmpty() || !StringUtils.hasText(created.get(0))) {
      throw new IllegalStateException("Failed to create derived address");
    }
    return created.get(0);
  }

  private String pollNextAvailableAddress(String poolKey, String queueKey) {
    for (int i = 0; i < 16; i++) {
      String address = redisUtil.pollFirst(queueKey);
      if (!StringUtils.hasText(address)) {
        return null;
      }
      if (inventoryService.isRiskBlocked(poolKey, address)) {
        log.warn("Skip blocked derived address from queue. poolKey={}, address={}", poolKey, address);
        continue;
      }
      return address;
    }
    return null;
  }

  private String resolveMode() {
    String mode = properties.getDerivedAddress().getMode();
    return StringUtils.hasText(mode) ? mode.trim().toUpperCase() : DerivedAddressProvisionerMode.ADDRESS_FACTORY.name();
  }

  private String queueKey(String poolKey) {
    return poolKey + ":" + RedisKeyNamespace.DERIVED_ADDRESS_AVAILABLE_SUFFIX;
  }

  private String leaseKey(String poolKey) {
    return poolKey + ":" + RedisKeyNamespace.DERIVED_ADDRESS_LEASES_SUFFIX;
  }

  private String lockKey(String poolKey) {
    return poolKey + ":" + RedisKeyNamespace.DERIVED_ADDRESS_LOCK_SUFFIX;
  }

  private void promoteCooldownExpired(String poolKey) {
    if (!properties.getDerivedAddress().isReuseAddress()) {
      return;
    }
    String queueKey = queueKey(poolKey);
    for (DerivedAddressPoolRecord record : inventoryService.findReusableCooldownExpired(poolKey, 0)) {
      DerivedAddressPoolRecord available = inventoryService.markAvailableAfterCooldown(poolKey, record.getAddress());
      if (DerivedAddressPoolStatus.AVAILABLE.name().equals(available.getStatus()) && !available.isRiskFlag()) {
        redisUtil.addLastIfAbsent(queueKey, available.getAddress());
        log.info(
            "Cooldown derived address returned to available queue. poolKey={}, address={}, reuseCount={}",
            poolKey,
            available.getAddress(),
            available.getReuseCount());
      }
    }
  }
}
